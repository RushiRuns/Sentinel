package com.rushi.sentinel.ui.entry

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.graphics.Color
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.Checkbox
import androidx.compose.material.CheckboxDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.TextButton
import androidx.compose.ui.text.font.FontFamily
import androidx.hilt.navigation.compose.hiltViewModel
import com.rushi.sentinel.ui.generator.GeneratorControls
import com.rushi.sentinel.ui.generator.PasswordStrengthIndicator
import com.rushi.sentinel.ui.generator.GeneratorViewModel
import com.rushi.sentinel.ui.generator.PasswordStrength
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Folder
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rushi.sentinel.ui.theme.AccentCyan
import com.rushi.sentinel.ui.theme.DeepBackground
import com.rushi.sentinel.ui.theme.PrimaryTeal
import com.rushi.sentinel.ui.theme.SlateSurface
import com.rushi.sentinel.ui.theme.TextPrimary
import com.rushi.sentinel.ui.theme.TextSecondary
import java.security.SecureRandom

@Composable
fun AddEditEntryScreen(
    entryId: Long?,
    viewModel: AddEditEntryViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()
    var isPasswordVisible by remember { mutableStateOf(false) }
    var showGeneratorDialog by remember { mutableStateOf(false) }

    LaunchedEffect(entryId) {
        viewModel.loadEntry(entryId)
    }

    LaunchedEffect(viewModel) {
        viewModel.isSaveSuccess.collect { success ->
            if (success) {
                onBack()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (entryId == null) "Add Entry" else "Edit Entry", color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Cancel",
                            tint = TextPrimary
                        )
                    }
                },
                backgroundColor = SlateSurface,
                elevation = 4.dp,
                actions = {
                    if (!loading) {
                        IconButton(onClick = { viewModel.saveEntry() }) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Save",
                                tint = AccentCyan
                            )
                        }
                    }
                }
            )
        },
        backgroundColor = DeepBackground,
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(DeepBackground)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
                    .animateContentSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Error Alert
                AnimatedVisibility(visible = error != null) {
                    error?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colors.error,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        )
                    }
                }

                // Name Input Card
                Card(
                    shape = RoundedCornerShape(12.dp),
                    backgroundColor = SlateSurface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        OutlinedTextField(
                            value = viewModel.name,
                            onValueChange = { viewModel.name = it },
                            label = { Text("Name (Required)") },
                            singleLine = true,
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                textColor = TextPrimary,
                                focusedBorderColor = AccentCyan,
                                unfocusedBorderColor = TextSecondary.copy(alpha = 0.3f),
                                focusedLabelColor = AccentCyan,
                                unfocusedLabelColor = TextSecondary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Username Input Card
                Card(
                    shape = RoundedCornerShape(12.dp),
                    backgroundColor = SlateSurface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        OutlinedTextField(
                            value = viewModel.username,
                            onValueChange = { viewModel.username = it },
                            label = { Text("Username") },
                            singleLine = true,
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                textColor = TextPrimary,
                                focusedBorderColor = AccentCyan,
                                unfocusedBorderColor = TextSecondary.copy(alpha = 0.3f),
                                focusedLabelColor = AccentCyan,
                                unfocusedLabelColor = TextSecondary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Password Input Card with Generator Action
                Card(
                    shape = RoundedCornerShape(12.dp),
                    backgroundColor = SlateSurface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        OutlinedTextField(
                            value = viewModel.password,
                            onValueChange = { viewModel.password = it },
                            label = { Text("Password (Required)") },
                            singleLine = true,
                            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Next
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
                                unfocusedBorderColor = TextSecondary.copy(alpha = 0.3f),
                                focusedLabelColor = AccentCyan,
                                unfocusedLabelColor = TextSecondary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Quick generator helper button
                        Button(
                            onClick = {
                                showGeneratorDialog = true
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = PrimaryTeal,
                                contentColor = DeepBackground
                            ),
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Autorenew,
                                contentDescription = "Generate",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Generate Secure Password", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // URL Input Card
                Card(
                    shape = RoundedCornerShape(12.dp),
                    backgroundColor = SlateSurface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        OutlinedTextField(
                            value = viewModel.url,
                            onValueChange = { viewModel.url = it },
                            label = { Text("Website / URL") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                textColor = TextPrimary,
                                focusedBorderColor = AccentCyan,
                                unfocusedBorderColor = TextSecondary.copy(alpha = 0.3f),
                                focusedLabelColor = AccentCyan,
                                unfocusedLabelColor = TextSecondary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Category Selection Card
                var isDropdownExpanded by remember { mutableStateOf(false) }
                val categories by viewModel.categories.collectAsState()
                val selectedCategory = categories.find { it.id == viewModel.categoryId }

                Card(
                    shape = RoundedCornerShape(12.dp),
                    backgroundColor = SlateSurface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Category",
                            color = AccentCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Box {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(DeepBackground, RoundedCornerShape(8.dp))
                                    .border(
                                        width = 1.dp,
                                        color = TextSecondary.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { isDropdownExpanded = true }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = selectedCategory?.name ?: "None",
                                    color = TextPrimary,
                                    fontSize = 16.sp
                                )
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = "Select Category",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = isDropdownExpanded,
                                onDismissRequest = { isDropdownExpanded = false },
                                modifier = Modifier
                                    .fillMaxWidth(0.9f)
                                    .background(SlateSurface)
                            ) {
                                DropdownMenuItem(
                                    onClick = {
                                        viewModel.categoryId = null
                                        isDropdownExpanded = false
                                    }
                                ) {
                                    Text("None", color = TextPrimary)
                                }
                                categories.forEach { category ->
                                    DropdownMenuItem(
                                        onClick = {
                                            viewModel.categoryId = category.id
                                            isDropdownExpanded = false
                                        }
                                    ) {
                                        Text(category.name, color = TextPrimary)
                                    }
                                }
                            }
                        }
                    }
                }

                // Notes Input Card
                Card(
                    shape = RoundedCornerShape(12.dp),
                    backgroundColor = SlateSurface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        OutlinedTextField(
                            value = viewModel.notes,
                            onValueChange = { viewModel.notes = it },
                            label = { Text("Notes") },
                            minLines = 3,
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                textColor = TextPrimary,
                                focusedBorderColor = AccentCyan,
                                unfocusedBorderColor = TextSecondary.copy(alpha = 0.3f),
                                focusedLabelColor = AccentCyan,
                                unfocusedLabelColor = TextSecondary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Pin / Favorite Toggle Card
                Card(
                    shape = RoundedCornerShape(12.dp),
                    backgroundColor = SlateSurface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Mark as Favorite",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Pinned to the top of your list",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                        Checkbox(
                            checked = viewModel.isFavorite,
                            onCheckedChange = { viewModel.isFavorite = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = PrimaryTeal,
                                uncheckedColor = TextSecondary,
                                checkmarkColor = DeepBackground
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom Save Button
                Button(
                    onClick = { viewModel.saveEntry() },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = PrimaryTeal,
                        contentColor = DeepBackground
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text("SAVE CREDENTIAL", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }

                Spacer(modifier = Modifier.height(32.dp))
            }

            if (loading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable(enabled = false) {},
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PrimaryTeal)
                }
            }
        }

        if (showGeneratorDialog) {
            GeneratorDialog(
                onDismiss = { showGeneratorDialog = false },
                onUsePassword = { pwd ->
                    viewModel.password = pwd
                    isPasswordVisible = true
                    showGeneratorDialog = false
                }
            )
        }
    }
}

@Composable
fun GeneratorDialog(
    onDismiss: () -> Unit,
    onUsePassword: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: GeneratorViewModel = hiltViewModel()
    val password by viewModel.password.collectAsState()
    val length by viewModel.length.collectAsState()
    val includeUppercase by viewModel.includeUppercase.collectAsState()
    val includeLowercase by viewModel.includeLowercase.collectAsState()
    val includeNumbers by viewModel.includeNumbers.collectAsState()
    val includeSymbols by viewModel.includeSymbols.collectAsState()
    val strength by viewModel.strength.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Secure Password Generator",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Generated Password Box
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DeepBackground, RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = password,
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { viewModel.generatePassword() }) {
                        Icon(
                            imageVector = Icons.Default.Autorenew,
                            contentDescription = "Regenerate",
                            tint = AccentCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Strength Meter
                PasswordStrengthIndicator(strength = strength)

                // Controls
                GeneratorControls(
                    length = length,
                    includeUppercase = includeUppercase,
                    includeLowercase = includeLowercase,
                    includeNumbers = includeNumbers,
                    includeSymbols = includeSymbols,
                    onLengthChange = { viewModel.setLength(it) },
                    onToggleUppercase = { viewModel.toggleUppercase() },
                    onToggleLowercase = { viewModel.toggleLowercase() },
                    onToggleNumbers = { viewModel.toggleNumbers() },
                    onToggleSymbols = { viewModel.toggleSymbols() }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onUsePassword(password) }
            ) {
                Text("USE PASSWORD", color = PrimaryTeal, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = TextPrimary)
            }
        },
        backgroundColor = SlateSurface,
        contentColor = TextPrimary,
        shape = RoundedCornerShape(12.dp)
    )
}
