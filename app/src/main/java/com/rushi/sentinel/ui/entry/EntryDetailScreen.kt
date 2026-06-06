package com.rushi.sentinel.ui.entry

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rushi.sentinel.ui.theme.AccentCyan
import com.rushi.sentinel.ui.theme.DeepBackground
import com.rushi.sentinel.ui.theme.ErrorRed
import com.rushi.sentinel.ui.theme.PrimaryTeal
import com.rushi.sentinel.ui.theme.SlateSurface
import com.rushi.sentinel.ui.theme.TextPrimary
import com.rushi.sentinel.ui.theme.TextSecondary

@Composable
fun EntryDetailScreen(
    entryId: Long,
    viewModel: EntryDetailViewModel,
    onBack: () -> Unit,
    onEditClick: (Long) -> Unit,
    onCopyPassword: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val entry by viewModel.entry.collectAsState()
    val isPasswordMasked by viewModel.isPasswordMasked.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(entryId) {
        viewModel.loadEntry(entryId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Entry Details", color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                backgroundColor = SlateSurface,
                elevation = 4.dp,
                actions = {
                    IconButton(onClick = { onEditClick(entryId) }) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Entry",
                            tint = AccentCyan
                        )
                    }
                }
            )
        },
        backgroundColor = DeepBackground,
        modifier = modifier
    ) { innerPadding ->
        entry?.let { currentEntry ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Name / Title Card
                DetailItem(label = "Name", value = currentEntry.name)

                // Username Card
                DetailItem(
                    label = "Username",
                    value = currentEntry.username.ifEmpty { "None" }
                )

                // Password Card (Masked/Unmasked + Actions)
                Card(
                    shape = RoundedCornerShape(12.dp),
                    backgroundColor = SlateSurface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Password",
                            color = AccentCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (isPasswordMasked) {
                                    "••••••••"
                                } else {
                                    String(currentEntry.password)
                                },
                                color = TextPrimary,
                                fontSize = 18.sp,
                                modifier = Modifier.weight(1f)
                            )

                            Row {
                                IconButton(onClick = { viewModel.togglePasswordMask() }) {
                                    Icon(
                                        imageVector = if (isPasswordMasked) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = if (isPasswordMasked) "Show Password" else "Hide Password",
                                        tint = TextSecondary
                                    )
                                }
                                IconButton(onClick = {
                                    val pwd = String(currentEntry.password)
                                    onCopyPassword(pwd)
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy Password",
                                        tint = PrimaryTeal
                                    )
                                }
                            }
                        }
                        if (!isPasswordMasked) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Revealed password masks automatically in 30s",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // URL Card
                DetailItem(
                    label = "Website / URL",
                    value = currentEntry.url?.ifEmpty { "None" } ?: "None"
                )

                // Notes Card
                DetailItem(
                    label = "Notes",
                    value = currentEntry.notes?.ifEmpty { "No notes" } ?: "No notes"
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Delete Button
                OutlinedButton(
                    onClick = { showDeleteDialog = true },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = ErrorRed
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .border(1.dp, ErrorRed, RoundedCornerShape(8.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Icon"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Delete Entry",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } ?: run {
            // Loading/Error State
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "Loading entry details...", color = TextSecondary)
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Text(
                    text = "Delete Credential",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete this credential? This action cannot be undone.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteEntry(onComplete = onBack)
                    }
                ) {
                    Text(text = "DELETE", color = ErrorRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(text = "CANCEL", color = TextPrimary)
                }
            },
            backgroundColor = SlateSurface,
            contentColor = TextPrimary
        )
    }
}

@Composable
fun DetailItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        backgroundColor = SlateSurface,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = label,
                color = AccentCyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                color = TextPrimary,
                fontSize = 16.sp
            )
        }
    }
}
