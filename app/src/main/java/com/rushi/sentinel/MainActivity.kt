package com.rushi.sentinel

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.rushi.sentinel.data.repository.VaultRepository
import com.rushi.sentinel.ui.lock.LockViewModel
import com.rushi.sentinel.ui.navigation.SentinelNavGraph
import com.rushi.sentinel.ui.theme.SentinelTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var vaultRepository: VaultRepository

    private val lockViewModel: LockViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Block screenshots and hide in recent apps
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)

        // Observe process lifecycle for background auto-locking
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) {
                // App moved to background, lock the vault immediately
                vaultRepository.lock()
            }
        })
        
        setContent {
            SentinelTheme {
                SentinelNavGraph(lockViewModel)
            }
        }
    }
}