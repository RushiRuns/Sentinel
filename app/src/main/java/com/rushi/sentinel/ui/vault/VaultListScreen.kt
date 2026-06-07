package com.rushi.sentinel.ui.vault

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rushi.sentinel.domain.model.Entry
import com.rushi.sentinel.ui.theme.AccentCyan
import com.rushi.sentinel.ui.theme.DeepBackground
import com.rushi.sentinel.ui.theme.PrimaryTeal
import com.rushi.sentinel.ui.theme.SlateSurface
import com.rushi.sentinel.ui.theme.TextPrimary
import com.rushi.sentinel.ui.theme.TextSecondary

@Composable
fun VaultListScreen(
    viewModel: VaultListViewModel,
    onEntryClick: (Long) -> Unit,
    onAddEntryClick: () -> Unit,
    onManageCategoriesClick: () -> Unit,
    onGeneratorClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onCopyPassword: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val entries by viewModel.entries.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Sentinel",
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        color = TextPrimary
                    )
                },
                backgroundColor = SlateSurface,
                elevation = 4.dp,
                actions = {
                    IconButton(onClick = onGeneratorClick) {
                        Icon(
                            imageVector = Icons.Default.Autorenew,
                            contentDescription = "Password Generator",
                            tint = AccentCyan
                        )
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = AccentCyan
                        )
                    }
                    IconButton(onClick = { viewModel.lockVault() }) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Lock Vault",
                            tint = AccentCyan
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddEntryClick,
                backgroundColor = PrimaryTeal,
                contentColor = DeepBackground,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Entry",
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        backgroundColor = DeepBackground,
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Search entries...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search Icon",
                        tint = TextSecondary
                    )
                },
                singleLine = true,
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    textColor = TextPrimary,
                    focusedBorderColor = AccentCyan,
                    unfocusedBorderColor = TextSecondary.copy(alpha = 0.3f),
                    placeholderColor = TextSecondary
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Categories horizontal list
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // "All" Chip
                CategoryFilterChip(
                    name = "All",
                    isSelected = selectedFilter == VaultFilter.ALL && selectedCategoryId == null,
                    onClick = { viewModel.selectFilter(VaultFilter.ALL) }
                )

                // "Favorites" Chip
                CategoryFilterChip(
                    name = "⭐ Favorites",
                    isSelected = selectedFilter == VaultFilter.FAVORITES,
                    onClick = { viewModel.selectFilter(VaultFilter.FAVORITES) }
                )

                // "Recent" Chip
                CategoryFilterChip(
                    name = "🕒 Recent",
                    isSelected = selectedFilter == VaultFilter.RECENT,
                    onClick = { viewModel.selectFilter(VaultFilter.RECENT) }
                )

                // Dynamic Categories Chips
                categories.forEach { category ->
                    CategoryFilterChip(
                        name = category.name,
                        isSelected = selectedCategoryId == category.id,
                        onClick = { viewModel.selectCategory(category.id) }
                    )
                }

                // Manage Chip
                ManageCategoriesChip(
                    onClick = onManageCategoriesClick
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (entries.isEmpty()) {
                // Empty State
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        val emptyStateIcon = when (selectedFilter) {
                            VaultFilter.FAVORITES -> Icons.Default.Star
                            else -> Icons.Default.Search
                        }
                        val emptyStateText = when {
                            searchQuery.isNotEmpty() -> "No matching entries found"
                            selectedFilter == VaultFilter.FAVORITES -> "No Favorites yet"
                            selectedFilter == VaultFilter.RECENT -> "No Recent items"
                            else -> "Your Vault is Empty"
                        }
                        Icon(
                            imageVector = emptyStateIcon,
                            contentDescription = "No Entries Icon",
                            tint = TextSecondary.copy(alpha = 0.3f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = emptyStateText,
                            color = TextSecondary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
                // List of Entries
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(entries, key = { it.id }) { entry ->
                        EntryCard(
                            entry = entry,
                            onClick = { onEntryClick(entry.id) },
                            onCopyClick = {
                                val pwd = String(entry.password)
                                onCopyPassword(pwd)
                                viewModel.recordEntryAccess(entry.id)
                            }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(80.dp)) // padding for FAB
                    }
                }
            }
        }
    }
}

@Composable
fun EntryCard(
    entry: Entry,
    onClick: () -> Unit,
    onCopyClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        backgroundColor = SlateSurface,
        elevation = 2.dp,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = entry.name,
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (entry.isFavorite) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Favorite",
                            tint = AccentCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = entry.username.ifEmpty { "No username" },
                    color = TextSecondary,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = onCopyClick,
                modifier = Modifier
                    .background(PrimaryTeal.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                    .size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy Password",
                    tint = PrimaryTeal,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun CategoryFilterChip(
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                color = if (isSelected) PrimaryTeal else SlateSurface,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name,
            color = if (isSelected) DeepBackground else TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ManageCategoriesChip(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(
                color = Color.Transparent,
                shape = RoundedCornerShape(20.dp)
            )
            .border(
                width = 1.dp,
                color = AccentCyan.copy(alpha = 0.5f),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Folder,
            contentDescription = null,
            tint = AccentCyan,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "Manage",
            color = AccentCyan,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
