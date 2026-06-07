package com.rushi.sentinel.ui.settings

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rushi.sentinel.ui.theme.*

@Composable
fun BackupRestoreScreen(
    viewModel: BackupRestoreViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()

    var showPasswordDialog by remember { mutableStateOf(false) }
    var dialogTitle by remember { mutableStateOf("") }
    var dialogMessage by remember { mutableStateOf("") }
    var pendingUri by remember { mutableStateOf<Uri?>(null) }
    var isExportFlow by remember { mutableStateOf(true) }

    var passwordInput by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    // SAF Document Launchers
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            pendingUri = uri
            isExportFlow = true
            dialogTitle = "Export Password"
            dialogMessage = "Enter a password to encrypt this backup file. You will need this password to restore your vault later."
            passwordInput = ""
            showPasswordDialog = true
        }
    }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            pendingUri = uri
            isExportFlow = false
            dialogTitle = "Restore Password"
            dialogMessage = "Enter the password used to encrypt this backup file."
            passwordInput = ""
            showPasswordDialog = true
        }
    }

    // Handle Toast messages
    LaunchedEffect(successMessage) {
        successMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            viewModel.clearMessages()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Backup & Restore", color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.clearMessages()
                        onBack()
                    }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                backgroundColor = SlateSurface,
                elevation = 4.dp
            )
        },
        backgroundColor = DeepBackground,
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Info description banner
                Card(
                    shape = RoundedCornerShape(12.dp),
                    backgroundColor = SlateSurface,
                    elevation = 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Local Backup Strategy",
                            color = AccentCyan,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        Text(
                            text = "Sentinel is a local-only app and never syncs with the cloud. To prevent data loss, we recommend exporting an encrypted backup file and storing it securely on another device or physical drive.",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                }

                // Error Message banner
                AnimatedVisibility(visible = error != null) {
                    error?.let { errMsg ->
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            backgroundColor = ErrorRed.copy(alpha = 0.2f),
                            elevation = 0.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = errMsg,
                                    color = ErrorRed,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { viewModel.clearMessages() },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear Error",
                                        tint = ErrorRed,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Export Button Card
                BackupActionButtonCard(
                    title = "Export Backup",
                    subtitle = "Saves all credentials and categories in an encrypted JSON file.",
                    icon = Icons.Default.Backup,
                    buttonText = "EXPORT",
                    onClick = {
                        viewModel.clearMessages()
                        createDocumentLauncher.launch("sentinel_backup.json")
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Import Button Card
                BackupActionButtonCard(
                    title = "Restore Backup",
                    subtitle = "Replaces the current vault entirely with credentials from a backup file.",
                    icon = Icons.Default.Restore,
                    buttonText = "RESTORE",
                    onClick = {
                        viewModel.clearMessages()
                        openDocumentLauncher.launch(arrayOf("application/json", "application/octet-stream"))
                    }
                )
            }

            if (loading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PrimaryTeal)
                }
            }
        }
    }

    // Password Prompt Dialog
    if (showPasswordDialog) {
        AlertDialog(
            onDismissRequest = {
                showPasswordDialog = false
                pendingUri = null
                passwordInput = ""
            },
            title = {
                Text(
                    text = dialogTitle,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Column {
                    Text(
                        text = dialogMessage,
                        color = TextSecondary,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        label = { Text("Backup Password") },
                        singleLine = true,
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            val image = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                Icon(imageVector = image, contentDescription = "Toggle password visibility", tint = TextSecondary)
                            }
                        },
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            textColor = TextPrimary,
                            focusedBorderColor = AccentCyan,
                            unfocusedBorderColor = TextSecondary.copy(alpha = 0.3f),
                            placeholderColor = TextSecondary,
                            focusedLabelColor = AccentCyan,
                            unfocusedLabelColor = TextSecondary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (passwordInput.isNotBlank() && pendingUri != null) {
                            val pwdCharArray = passwordInput.toCharArray()
                            // Clear input string immediately to limit string lifecycle in memory
                            passwordInput = ""
                            showPasswordDialog = false
                            
                            if (isExportFlow) {
                                viewModel.exportBackup(context.contentResolver, pendingUri!!, pwdCharArray)
                            } else {
                                viewModel.importBackup(context.contentResolver, pendingUri!!, pwdCharArray)
                            }
                            pendingUri = null
                        } else {
                            Toast.makeText(context, "Password cannot be empty", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text(text = "CONFIRM", color = PrimaryTeal, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showPasswordDialog = false
                        pendingUri = null
                        passwordInput = ""
                    }
                ) {
                    Text(text = "CANCEL", color = TextPrimary)
                }
            },
            backgroundColor = SlateSurface,
            contentColor = TextPrimary
        )
    }
}

@Composable
fun BackupActionButtonCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    buttonText: String,
    onClick: () -> Unit
) {
    Card(
        backgroundColor = SlateSurface,
        shape = RoundedCornerShape(12.dp),
        elevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = PrimaryTeal,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subtitle,
                color = TextSecondary,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = PrimaryTeal,
                    contentColor = DeepBackground
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = buttonText,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}
