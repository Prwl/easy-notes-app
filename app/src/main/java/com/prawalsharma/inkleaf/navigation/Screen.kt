package com.prawalsharma.inkleaf.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Archive : Screen("archive")
    data object Trash : Screen("trash")
    data object Hidden : Screen("hidden")
    data object Folders : Screen("folders")
    data object Settings : Screen("settings")
    data object Editor : Screen("editor/{noteId}") {
        fun createRoute(noteId: Long = -1L) = "editor/$noteId"
    }
    data object FolderNotes : Screen("folder/{folderId}") {
        fun createRoute(folderId: Long) = "folder/$folderId"
    }
    data object Drawing : Screen("drawing/{noteId}/{hasExisting}") {
        fun createRoute(noteId: Long, hasExisting: Boolean = false) =
            "drawing/$noteId/${if (hasExisting) "1" else "0"}"
    }
}

/** Routes where bottom navigation should be visible. */
val bottomNavRoutes = setOf(Screen.Home.route, Screen.Folders.route, Screen.Settings.route)
