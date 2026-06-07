package com.rushi.sentinel.ui.lock

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.rushi.sentinel.ui.theme.AccentCyan
import com.rushi.sentinel.ui.theme.DeepBackground
import com.rushi.sentinel.ui.theme.PrimaryTeal
import com.rushi.sentinel.ui.theme.SlateSurface
import com.rushi.sentinel.ui.theme.TextPrimary
import com.rushi.sentinel.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun LockScreen(
    viewModel: LockViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val isSetupMode by viewModel.isSetupMode.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()
    val lockoutTimeRemaining by viewModel.lockoutTimeRemaining.collectAsState()
    val biometricEnabled by viewModel.biometricEnabled.collectAsState()
    val vaultResetEvent by viewModel.vaultResetEvent.collectAsState()

    var passwordText by remember { mutableStateOf("") }
    var confirmPasswordText by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isConfirmPasswordVisible by remember { mutableStateOf(false) }

    val isLockoutActive = lockoutTimeRemaining > 0

    // Trigger biometric prompt reusable flow
    val triggerBiometric = {
        scope.launch {
            val cipher = viewModel.getBiometricDecryptionCipher()
            if (cipher != null) {
                val activity = context as FragmentActivity
                val executor = ContextCompat.getMainExecutor(context)
                val biometricPrompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        val authenticatedCipher = result.cryptoObject?.cipher
                        if (authenticatedCipher != null) {
                            viewModel.unlockWithBiometricCipher(authenticatedCipher)
                        }
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        // Suppress negative button / click away logs
                    }
                })
                val promptInfo = BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Unlock Sentinel")
                    .setSubtitle("Confirm fingerprint or face to decrypt vault")
                    .setNegativeButtonText("Use Password")
                    .build()
                biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
            }
        }
    }

    // Auto-trigger biometric prompt on launch if enabled
    LaunchedEffect(biometricEnabled, isSetupMode, isLockoutActive) {
        if (biometricEnabled && !isSetupMode && !isLockoutActive) {
            triggerBiometric()
        }
    }

    // Reset password fields if setup mode switches
    LaunchedEffect(isSetupMode) {
        passwordText = ""
        confirmPasswordText = ""
    }

    // Vault-reset dialog: shown when a stale/incompatible plain-SQLite DB was detected and wiped.
    if (vaultResetEvent) {
        AlertDialog(
            onDismissRequest = { viewModel.clearVaultResetEvent() },
            title = {
                Text(
                    text = "Vault Reset",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "An incompatible database file was detected from a previous installation " +
                           "(before encryption was enabled). It has been automatically removed.\n\n" +
                           "Please set a new master password to initialize a fresh encrypted vault.",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.clearVaultResetEvent() }) {
                    Text("OK", color = AccentCyan, fontWeight = FontWeight.Bold)
                }
            },
            backgroundColor = SlateSurface,
            contentColor = TextPrimary
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        SlateSurface,
                        DeepBackground
                    )
                )
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .animateContentSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Glowing Lock Icon
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(PrimaryTeal.copy(alpha = 0.1f))
                    .border(2.dp, PrimaryTeal, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isSetupMode) Icons.Default.VpnKey else Icons.Default.Lock,
                    contentDescription = "Lock State Icon",
                    tint = AccentCyan,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Brand Title
            Text(
                text = "SENTINEL",
                color = TextPrimary,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp
            )

            Text(
                text = "LOCAL-ONLY VAULT",
                color = AccentCyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Subtitle Description
            Text(
                text = if (isSetupMode) {
                    "Set up a master password to initialize your secure, fully encrypted local vault."
                } else {
                    "Your vault is encrypted. Enter your master password to unlock."
                },
                color = TextSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Password Fields Container
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Master Password Input
                OutlinedTextField(
                    value = passwordText,
                    onValueChange = { passwordText = it },
                    label = { Text(if (isSetupMode) "Create Master Password" else "Master Password") },
                    singleLine = true,
                    enabled = !loading && !isLockoutActive,
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = if (isSetupMode) ImeAction.Next else ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (!isSetupMode && passwordText.isNotEmpty()) {
                                submitUnlock(passwordText, viewModel)
                                passwordText = ""
                            }
                        }
                    ),
                    trailingIcon = {
                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                            Icon(
                                imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (isPasswordVisible) "Hide password" else "Show password",
                                tint = TextSecondary
                            )
                        }
                    },
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        textColor = TextPrimary,
                        focusedBorderColor = AccentCyan,
                        unfocusedBorderColor = TextSecondary.copy(alpha = 0.5f),
                        focusedLabelColor = AccentCyan,
                        unfocusedLabelColor = TextSecondary,
                        disabledBorderColor = TextSecondary.copy(alpha = 0.2f),
                        disabledLabelColor = TextSecondary.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (isSetupMode) {
                    Spacer(modifier = Modifier.height(16.dp))

                    // Confirm Master Password Input
                    OutlinedTextField(
                        value = confirmPasswordText,
                        onValueChange = { confirmPasswordText = it },
                        label = { Text("Confirm Master Password") },
                        singleLine = true,
                        enabled = !loading,
                        visualTransformation = if (isConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (passwordText.isNotEmpty() && confirmPasswordText.isNotEmpty()) {
                                    submitSetup(passwordText, confirmPasswordText, viewModel)
                                    passwordText = ""
                                    confirmPasswordText = ""
                                }
                            }
                        ),
                        trailingIcon = {
                            IconButton(onClick = { isConfirmPasswordVisible = !isConfirmPasswordVisible }) {
                                Icon(
                                    imageVector = if (isConfirmPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (isConfirmPasswordVisible) "Hide password" else "Show password",
                                    tint = TextSecondary
                                )
                            }
                        },
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            textColor = TextPrimary,
                            focusedBorderColor = AccentCyan,
                            unfocusedBorderColor = TextSecondary.copy(alpha = 0.5f),
                            focusedLabelColor = AccentCyan,
                            unfocusedLabelColor = TextSecondary,
                            disabledBorderColor = TextSecondary.copy(alpha = 0.2f),
                            disabledLabelColor = TextSecondary.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Error Message slide/fade representation
                AnimatedVisibility(visible = error != null) {
                    error?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colors.error,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                        )
                    }
                }

                // Lockout delay warning message
                AnimatedVisibility(visible = isLockoutActive) {
                    Text(
                        text = "Too many failed attempts. Try again in $lockoutTimeRemaining seconds.",
                        color = MaterialTheme.colors.error,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    )
                }

                // Submit Button / Biometric Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            if (isSetupMode) {
                                submitSetup(passwordText, confirmPasswordText, viewModel)
                                passwordText = ""
                                confirmPasswordText = ""
                            } else {
                                submitUnlock(passwordText, viewModel)
                                passwordText = ""
                            }
                        },
                        enabled = !loading && !isLockoutActive && passwordText.isNotEmpty() && (!isSetupMode || confirmPasswordText.isNotEmpty()),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = PrimaryTeal,
                            contentColor = DeepBackground,
                            disabledBackgroundColor = PrimaryTeal.copy(alpha = 0.3f),
                            disabledContentColor = TextSecondary.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                    ) {
                        if (loading) {
                            CircularProgressIndicator(
                                color = DeepBackground,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Text(
                                text = if (isSetupMode) "Initialize Vault" else "Decrypt Vault",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    if (biometricEnabled && !isSetupMode && !isLockoutActive) {
                        Spacer(modifier = Modifier.width(12.dp))
                        IconButton(
                            onClick = { triggerBiometric() },
                            modifier = Modifier
                                .size(50.dp)
                                .background(PrimaryTeal.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                .border(1.dp, PrimaryTeal, RoundedCornerShape(8.dp))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = "Biometric Unlock",
                                tint = AccentCyan,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// Memory-safe submission handlers converting String to CharArray. ViewModels own zeroing.
private fun submitUnlock(password: String, viewModel: LockViewModel) {
    val chars = password.toCharArray()
    viewModel.unlock(chars)
}

private fun submitSetup(password: String, confirm: String, viewModel: LockViewModel) {
    val pwdChars = password.toCharArray()
    val confirmChars = confirm.toCharArray()
    viewModel.setupVault(pwdChars, confirmChars)
}
