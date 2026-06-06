package com.rushi.sentinel.ui.generator

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.Checkbox
import androidx.compose.material.CheckboxDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Slider
import androidx.compose.material.SliderDefaults
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
fun GeneratorScreen(
    viewModel: GeneratorViewModel,
    onBack: () -> Unit,
    onCopyPassword: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val password by viewModel.password.collectAsState()
    val length by viewModel.length.collectAsState()
    val includeUppercase by viewModel.includeUppercase.collectAsState()
    val includeLowercase by viewModel.includeLowercase.collectAsState()
    val includeNumbers by viewModel.includeNumbers.collectAsState()
    val includeSymbols by viewModel.includeSymbols.collectAsState()
    val strength by viewModel.strength.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Password Generator", color = TextPrimary) },
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
                elevation = 4.dp
            )
        },
        backgroundColor = DeepBackground,
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Password Display Card
            Card(
                shape = RoundedCornerShape(12.dp),
                backgroundColor = SlateSurface,
                elevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Password Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DeepBackground, RoundedCornerShape(8.dp))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = password,
                            color = TextPrimary,
                            fontSize = 20.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Strength Indicator Row
                    PasswordStrengthIndicator(strength = strength)

                    Spacer(modifier = Modifier.height(16.dp))

                    // Action Buttons (Copy / Regenerate)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(
                            onClick = { onCopyPassword(password) },
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = PrimaryTeal,
                                contentColor = DeepBackground
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Password",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("COPY", fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Button(
                            onClick = { viewModel.generatePassword() },
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = SlateSurface,
                                contentColor = AccentCyan
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Autorenew,
                                contentDescription = "Regenerate",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("REGENERATE", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Options Card
            Card(
                shape = RoundedCornerShape(12.dp),
                backgroundColor = SlateSurface,
                elevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Options",
                        color = AccentCyan,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

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
            }
        }
    }
}

@Composable
fun PasswordStrengthIndicator(
    strength: PasswordStrength,
    modifier: Modifier = Modifier
) {
    val strengthText = when (strength) {
        PasswordStrength.WEAK -> "Weak"
        PasswordStrength.MEDIUM -> "Medium"
        PasswordStrength.STRONG -> "Strong"
        PasswordStrength.VERY_STRONG -> "Very Strong"
    }

    val strengthColor = when (strength) {
        PasswordStrength.WEAK -> ErrorRed
        PasswordStrength.MEDIUM -> Color(0xFFFFB74D) // Orange
        PasswordStrength.STRONG -> PrimaryTeal
        PasswordStrength.VERY_STRONG -> AccentCyan
    }

    val segmentsFilled = when (strength) {
        PasswordStrength.WEAK -> 1
        PasswordStrength.MEDIUM -> 2
        PasswordStrength.STRONG -> 3
        PasswordStrength.VERY_STRONG -> 4
    }

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Password Strength",
                color = TextSecondary,
                fontSize = 12.sp
            )
            Text(
                text = strengthText,
                color = strengthColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Segmented Strength Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            for (i in 1..4) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .background(
                            color = if (i <= segmentsFilled) strengthColor else SlateSurface.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(3.dp)
                        )
                )
            }
        }
    }
}

@Composable
fun GeneratorControls(
    length: Int,
    includeUppercase: Boolean,
    includeLowercase: Boolean,
    includeNumbers: Boolean,
    includeSymbols: Boolean,
    onLengthChange: (Int) -> Unit,
    onToggleUppercase: () -> Unit,
    onToggleLowercase: () -> Unit,
    onToggleNumbers: () -> Unit,
    onToggleSymbols: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Length Slider
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Length",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "$length characters",
                    color = AccentCyan,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Slider(
                value = length.toFloat(),
                onValueChange = { onLengthChange(it.toInt()) },
                valueRange = 8f..64f,
                colors = SliderDefaults.colors(
                    thumbColor = PrimaryTeal,
                    activeTrackColor = PrimaryTeal,
                    inactiveTrackColor = TextSecondary.copy(alpha = 0.2f)
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Toggles list
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            GeneratorToggleRow(
                label = "Include Uppercase Letters (A-Z)",
                checked = includeUppercase,
                onCheckedChange = onToggleUppercase
            )
            GeneratorToggleRow(
                label = "Include Lowercase Letters (a-z)",
                checked = includeLowercase,
                onCheckedChange = onToggleLowercase
            )
            GeneratorToggleRow(
                label = "Include Numbers (0-9)",
                checked = includeNumbers,
                onCheckedChange = onToggleNumbers
            )
            GeneratorToggleRow(
                label = "Include Symbols (!@#\$%)",
                checked = includeSymbols,
                onCheckedChange = onToggleSymbols
            )
        }
    }
}

@Composable
fun GeneratorToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onCheckedChange)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = TextPrimary,
            fontSize = 15.sp
        )
        Checkbox(
            checked = checked,
            onCheckedChange = { onCheckedChange() },
            colors = CheckboxDefaults.colors(
                checkedColor = PrimaryTeal,
                uncheckedColor = TextSecondary.copy(alpha = 0.5f),
                checkmarkColor = DeepBackground
            )
        )
    }
}
