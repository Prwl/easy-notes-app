package com.prawalsharma.inkleaf.ui.lock

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.prawalsharma.inkleaf.R
import kotlinx.coroutines.launch

/** Full-screen lock overlay. Calls [onUnlocked] when PIN matches or biometric succeeds. */
@Composable
fun NoteLockScreen(
    hasPinSet: Boolean,
    verifyPin: suspend (String) -> Boolean,
    onUnlocked: () -> Unit,
    onSetPin: (String) -> Unit,  // called when a new PIN is established
    onCancel: () -> Unit          // back / dismiss without unlocking
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var input by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var isSettingPin by remember { mutableStateOf(!hasPinSet) }
    var pendingNewPin by remember { mutableStateOf("") }

    val biometricTitle = stringResource(R.string.biometric_title)
    val biometricSubtitle = stringResource(R.string.biometric_subtitle)
    val biometricNegativeButton = stringResource(R.string.biometric_negative_button)
    val errPinsMismatch = stringResource(R.string.error_pins_dont_match)
    val errIncorrectPin = stringResource(R.string.error_incorrect_pin)

    // System back / gesture should dismiss the lock screen instead of killing the activity
    androidx.activity.compose.BackHandler { onCancel() }

    // Launch biometric prompt on first composition if PIN is already set
    LaunchedEffect(Unit) {
        if (hasPinSet) {
            (context as? FragmentActivity)?.let { tryBiometric(it, biometricTitle, biometricSubtitle, biometricNegativeButton, onUnlocked) }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top-left back button so the user is never trapped on this screen
        IconButton(
            onClick = onCancel,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 12.dp, start = 8.dp)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.cd_back)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier
                .align(Alignment.Center)
                .padding(32.dp)
        ) {
            Icon(
                Icons.Default.Lock,
                null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Text(
                text = if (isSettingPin) {
                    if (pendingNewPin.isEmpty()) stringResource(R.string.lock_set_pin_prompt)
                    else stringResource(R.string.lock_confirm_pin_prompt)
                } else stringResource(R.string.lock_enter_pin_prompt),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )

            // PIN dots
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                repeat(4) { i ->
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(
                                if (i < input.length) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant
                            )
                    )
                }
            }

            if (errorMessage.isNotEmpty()) {
                Text(
                    errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Number pad
            NumberPad(
                onDigit = { digit ->
                    if (input.length < 4) {
                        input += digit
                        if (input.length == 4) {
                            // Evaluate PIN
                            if (isSettingPin) {
                                if (pendingNewPin.isEmpty()) {
                                    pendingNewPin = input
                                    input = ""
                                    errorMessage = ""
                                } else if (input == pendingNewPin) {
                                    onSetPin(input)
                                    onUnlocked()
                                } else {
                                    errorMessage = errPinsMismatch
                                    pendingNewPin = ""
                                    input = ""
                                }
                            } else {
                                scope.launch {
                                    val ok = verifyPin(input)
                                    if (ok) {
                                        onUnlocked()
                                    } else {
                                        errorMessage = errIncorrectPin
                                        input = ""
                                    }
                                }
                            }
                        }
                    }
                },
                onBackspace = { if (input.isNotEmpty()) input = input.dropLast(1) }
            )

            if (hasPinSet) {
                TextButton(onClick = {
                    (context as? FragmentActivity)?.let { tryBiometric(it, biometricTitle, biometricSubtitle, biometricNegativeButton, onUnlocked) }
                }) {
                    Icon(Icons.Default.Fingerprint, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.btn_use_fingerprint))
                }
            }
        }
    }
}

@Composable
private fun NumberPad(
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit
) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("", "0", "⌫")
    )
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                row.forEach { label ->
                    if (label.isEmpty()) {
                        Spacer(Modifier.size(72.dp))
                    } else {
                        FilledTonalButton(
                            onClick = { if (label == "⌫") onBackspace() else onDigit(label) },
                            modifier = Modifier.size(72.dp),
                            shape = CircleShape,
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(label, fontSize = 22.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}

private fun tryBiometric(
    activity: FragmentActivity,
    title: String,
    subtitle: String,
    negativeButtonText: String,
    onSuccess: () -> Unit
) {
    val manager = BiometricManager.from(activity)
    if (manager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) !=
        BiometricManager.BIOMETRIC_SUCCESS) return

    val executor = ContextCompat.getMainExecutor(activity)
    val prompt = BiometricPrompt(
        activity, executor,
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess()
            }
        }
    )
    val info = BiometricPrompt.PromptInfo.Builder()
        .setTitle(title)
        .setSubtitle(subtitle)
        .setNegativeButtonText(negativeButtonText)
        .build()
    prompt.authenticate(info)
}
