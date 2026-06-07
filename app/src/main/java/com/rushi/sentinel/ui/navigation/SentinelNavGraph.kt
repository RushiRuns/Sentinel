package com.rushi.sentinel.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.rushi.sentinel.ui.entry.AddEditEntryScreen
import com.rushi.sentinel.ui.entry.AddEditEntryViewModel
import com.rushi.sentinel.ui.entry.EntryDetailScreen
import com.rushi.sentinel.ui.entry.EntryDetailViewModel
import com.rushi.sentinel.ui.lock.LockScreen
import com.rushi.sentinel.ui.lock.LockViewModel
import com.rushi.sentinel.ui.vault.VaultListScreen
import com.rushi.sentinel.ui.vault.VaultListViewModel
import com.rushi.sentinel.ui.categories.CategoriesScreen
import com.rushi.sentinel.ui.categories.CategoriesViewModel
import com.rushi.sentinel.ui.generator.GeneratorScreen
import com.rushi.sentinel.ui.generator.GeneratorViewModel
import com.rushi.sentinel.ui.settings.SettingsScreen
import com.rushi.sentinel.ui.settings.BackupRestoreScreen
import com.rushi.sentinel.ui.settings.BackupRestoreViewModel

sealed class MainScreen {
    object VaultList : MainScreen()
    object Categories : MainScreen()
    object PasswordGenerator : MainScreen()
    object Settings : MainScreen()
    object BackupRestore : MainScreen()
    data class EntryDetail(val entryId: Long) : MainScreen()
    data class AddEditEntry(val entryId: Long?) : MainScreen()
}

@Composable
fun SentinelNavGraph(
    lockViewModel: LockViewModel,
    onCopyPassword: (String) -> Unit,
    onLockVault: () -> Unit
) {
    val isLocked by VaultLockState.isLocked.collectAsState()
    var currentScreen by remember { mutableStateOf<MainScreen>(MainScreen.VaultList) }

    // When vault locks, reset main navigation to start fresh upon next unlock
    LaunchedEffect(isLocked) {
        if (isLocked) {
            currentScreen = MainScreen.VaultList
        }
    }

    if (isLocked) {
        LockScreen(viewModel = lockViewModel)
    } else {
        val vaultListViewModel: VaultListViewModel = hiltViewModel()
        val entryDetailViewModel: EntryDetailViewModel = hiltViewModel()
        val addEditEntryViewModel: AddEditEntryViewModel = hiltViewModel()
        val categoriesViewModel: CategoriesViewModel = hiltViewModel()
        val generatorViewModel: GeneratorViewModel = hiltViewModel()
        val backupRestoreViewModel: BackupRestoreViewModel = hiltViewModel()

        when (val screen = currentScreen) {
            is MainScreen.VaultList -> {
                VaultListScreen(
                    viewModel = vaultListViewModel,
                    onEntryClick = { entryId ->
                        currentScreen = MainScreen.EntryDetail(entryId)
                    },
                    onAddEntryClick = {
                        currentScreen = MainScreen.AddEditEntry(null)
                    },
                    onManageCategoriesClick = {
                        currentScreen = MainScreen.Categories
                    },
                    onGeneratorClick = {
                        currentScreen = MainScreen.PasswordGenerator
                    },
                    onSettingsClick = {
                        currentScreen = MainScreen.Settings
                    },
                    onCopyPassword = onCopyPassword
                )
            }
            is MainScreen.Settings -> {
                SettingsScreen(
                    onBack = {
                        currentScreen = MainScreen.VaultList
                    },
                    onBackupRestoreClick = {
                        currentScreen = MainScreen.BackupRestore
                    },
                    onLockClick = onLockVault
                )
            }
            is MainScreen.BackupRestore -> {
                BackupRestoreScreen(
                    viewModel = backupRestoreViewModel,
                    onBack = {
                        currentScreen = MainScreen.Settings
                    }
                )
            }
            is MainScreen.Categories -> {
                CategoriesScreen(
                    viewModel = categoriesViewModel,
                    onBack = {
                        currentScreen = MainScreen.VaultList
                    }
                )
            }
            is MainScreen.PasswordGenerator -> {
                GeneratorScreen(
                    viewModel = generatorViewModel,
                    onBack = {
                        currentScreen = MainScreen.VaultList
                    },
                    onCopyPassword = onCopyPassword
                )
            }
            is MainScreen.EntryDetail -> {
                EntryDetailScreen(
                    entryId = screen.entryId,
                    viewModel = entryDetailViewModel,
                    onBack = {
                        currentScreen = MainScreen.VaultList
                    },
                    onEditClick = { entryId ->
                        currentScreen = MainScreen.AddEditEntry(entryId)
                    },
                    onCopyPassword = onCopyPassword
                )
            }
            is MainScreen.AddEditEntry -> {
                AddEditEntryScreen(
                    entryId = screen.entryId,
                    viewModel = addEditEntryViewModel,
                    onBack = {
                        if (screen.entryId != null) {
                            currentScreen = MainScreen.EntryDetail(screen.entryId)
                        } else {
                            currentScreen = MainScreen.VaultList
                        }
                    }
                )
            }
        }
    }
}
