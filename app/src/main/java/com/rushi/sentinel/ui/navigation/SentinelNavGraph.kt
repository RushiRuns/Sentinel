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

sealed class MainScreen {
    object VaultList : MainScreen()
    data class EntryDetail(val entryId: Long) : MainScreen()
    data class AddEditEntry(val entryId: Long?) : MainScreen()
}

@Composable
fun SentinelNavGraph(
    lockViewModel: LockViewModel,
    onCopyPassword: (String) -> Unit
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
