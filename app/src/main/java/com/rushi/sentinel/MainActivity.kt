package com.rushi.sentinel

import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.rushi.sentinel.data.repository.VaultRepository
import com.rushi.sentinel.ui.ClipboardHelper
import com.rushi.sentinel.ui.lock.LockViewModel
import com.rushi.sentinel.ui.navigation.SentinelNavGraph
import com.rushi.sentinel.ui.theme.SentinelTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var vaultRepository: VaultRepository

    private val lockViewModel: LockViewModel by viewModels()
    private lateinit var clipboardHelper: ClipboardHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Block screenshots and hide in recent apps
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)

        val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboardHelper = ClipboardHelper(clipboardManager, Handler(Looper.getMainLooper()))

        // Observe process lifecycle for background auto-locking
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) {
                // App moved to background, lock the vault immediately
                vaultRepository.lock()
            }
        })
        
        setContent {
            SentinelTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .systemBarsPadding(),
                    color = MaterialTheme.colors.background
                ) {
                    SentinelNavGraph(
                        lockViewModel = lockViewModel,
                        onCopyPassword = ::copyToClipboard,
                        onLockVault = { vaultRepository.lock() }
                    )
                }
            }
        }
    }

    private fun copyToClipboard(value: String) {
        clipboardHelper.copyToClipboard(value)
        Toast.makeText(this, "Password copied to clipboard (clears in 30s)", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        clipboardHelper.onDestroy()
    }
}