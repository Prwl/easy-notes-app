package com.prawalsharma.inkleaf

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.compose.animation.*
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.prawalsharma.inkleaf.data.prefs.ThemeMode
import com.prawalsharma.inkleaf.data.prefs.UserPreferences
import com.prawalsharma.inkleaf.navigation.Screen
import com.prawalsharma.inkleaf.navigation.bottomNavRoutes
import com.prawalsharma.inkleaf.ui.editor.EditorScreen
import com.prawalsharma.inkleaf.ui.folders.FolderListScreen
import com.prawalsharma.inkleaf.ui.folders.FolderNotesScreen
import com.prawalsharma.inkleaf.ui.home.ArchiveScreen
import com.prawalsharma.inkleaf.ui.home.HiddenNotesScreen
import com.prawalsharma.inkleaf.ui.home.HomeScreen
import com.prawalsharma.inkleaf.ui.home.TrashScreen
import com.prawalsharma.inkleaf.ui.settings.SettingsScreen
import com.prawalsharma.inkleaf.ui.theme.InkleafTheme
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

// Lightweight ViewModel to expose ThemeMode before NavGraph is built
@HiltViewModel
class MainViewModel @Inject constructor(prefs: UserPreferences) : ViewModel() {
    val themeMode = prefs.themeMode.stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.SYSTEM)
    val appFont = prefs.appFont.stateIn(viewModelScope, SharingStarted.Eagerly, com.prawalsharma.inkleaf.data.prefs.AppFont.DEFAULT)
    val dynamicColor = prefs.dynamicColor.stateIn(viewModelScope, SharingStarted.Eagerly, false)
}

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    /** Holds widget-launched note ID across recompositions and onNewIntent calls. */
    private val initialNoteIdState = mutableStateOf<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        initialNoteIdState.value = extractWidgetNoteId(intent)
        setContent {
            val mainVm: MainViewModel = hiltViewModel()
            val themeMode by mainVm.themeMode.collectAsStateWithLifecycle()
            val appFont by mainVm.appFont.collectAsStateWithLifecycle()
            val dynamicColor by mainVm.dynamicColor.collectAsStateWithLifecycle()
            InkleafTheme(themeMode = themeMode, appFont = appFont, dynamicColor = dynamicColor) {
                InkleafNavGraph(initialNoteId = initialNoteIdState.value, onConsumeInitial = {
                    initialNoteIdState.value = null
                })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractWidgetNoteId(intent)?.let { initialNoteIdState.value = it }
    }

    private fun extractWidgetNoteId(intent: Intent?): Long? {
        if (intent == null) return null
        if (!intent.hasExtra(com.prawalsharma.inkleaf.ui.widget.WIDGET_EXTRA_NOTE_ID)) return null
        return intent.getLongExtra(com.prawalsharma.inkleaf.ui.widget.WIDGET_EXTRA_NOTE_ID, -2L)
            .takeIf { it != -2L }
    }
}

@Composable
fun InkleafNavGraph(
    initialNoteId: Long? = null,
    onConsumeInitial: () -> Unit = {}
) {
    val navController = rememberNavController()
    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStack?.destination?.route
    val showBottomBar = currentRoute in bottomNavRoutes

    // Handle widget deep-link: navigate to editor for the given (or new) note.
    LaunchedEffect(initialNoteId) {
        val target = initialNoteId ?: return@LaunchedEffect
        navController.navigate(Screen.Editor.createRoute(target))
        onConsumeInitial()
    }

    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically { it },
                exit = slideOutVertically { it }
            ) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentRoute == Screen.Home.route,
                        onClick = {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Home.route) { inclusive = true }
                                launchSingleTop = true
                            }
                        },
                        icon = { Icon(Icons.AutoMirrored.Filled.Notes, null) },
                        label = { Text("Notes") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == Screen.Folders.route,
                        onClick = {
                            navController.navigate(Screen.Folders.route) {
                                popUpTo(Screen.Home.route)
                                launchSingleTop = true
                            }
                        },
                        icon = { Icon(Icons.Default.Folder, null) },
                        label = { Text("Folders") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == Screen.Settings.route,
                        onClick = {
                            navController.navigate(Screen.Settings.route) {
                                popUpTo(Screen.Home.route)
                                launchSingleTop = true
                            }
                        },
                        icon = { Icon(Icons.Default.Settings, null) },
                        label = { Text("Settings") }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onNoteClick = { navController.navigate(Screen.Editor.createRoute(it)) },
                    onNewNote = { navController.navigate(Screen.Editor.createRoute()) },
                    onNavigateToArchive = { navController.navigate(Screen.Archive.route) },
                    onNavigateToTrash = { navController.navigate(Screen.Trash.route) },
                    onNavigateToHidden = { navController.navigate(Screen.Hidden.route) },
                    bottomPadding = PaddingValues(0.dp)
                )
            }
            composable(
                route = Screen.Editor.route,
                arguments = listOf(navArgument("noteId") {
                    type = NavType.LongType; defaultValue = -1L
                })
            ) {
                EditorScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.Archive.route) {
                ArchiveScreen(
                    onBack = { navController.popBackStack() },
                    onNoteClick = { navController.navigate(Screen.Editor.createRoute(it)) }
                )
            }
            composable(Screen.Trash.route) {
                TrashScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.Hidden.route) {
                HiddenNotesScreen(
                    onBack = { navController.popBackStack() },
                    onNoteClick = { navController.navigate(Screen.Editor.createRoute(it)) }
                )
            }
            composable(Screen.Folders.route) {
                FolderListScreen(
                    onFolderClick = { folderId ->
                        navController.navigate(Screen.FolderNotes.createRoute(folderId))
                    },
                    bottomPadding = PaddingValues(0.dp)
                )
            }
            composable(
                route = Screen.FolderNotes.route,
                arguments = listOf(navArgument("folderId") { type = NavType.LongType })
            ) {
                FolderNotesScreen(
                    onBack = { navController.popBackStack() },
                    onNoteClick = { navController.navigate(Screen.Editor.createRoute(it)) },
                    onNewNote = { navController.navigate(Screen.Editor.createRoute()) }
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onNavigateToArchive = { navController.navigate(Screen.Archive.route) },
                    onNavigateToTrash = { navController.navigate(Screen.Trash.route) },
                    onNavigateToHidden = { navController.navigate(Screen.Hidden.route) },
                    bottomPadding = PaddingValues(0.dp)
                )
            }
        }
    }
}
