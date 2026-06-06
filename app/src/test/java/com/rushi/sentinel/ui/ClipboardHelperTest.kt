package com.rushi.sentinel.ui

import android.content.ClipboardManager
import android.os.Handler
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@RunWith(RobolectricTestRunner::class)
class ClipboardHelperTest {

    private val clipboardManager: ClipboardManager = mock()
    private val handler: Handler = mock()
    private lateinit var clipboardHelper: ClipboardHelper

    @Before
    fun setUp() {
        clipboardHelper = ClipboardHelper(clipboardManager, handler)
    }

    @Test
    fun testCopyToClipboard_setsClipAndPostsDelayedClear() {
        val password = "MySecurePassword"
        clipboardHelper.copyToClipboard(password)

        // Verify that the clipboard got set
        verify(clipboardManager).setPrimaryClip(any())

        // Verify that handler.postDelayed got scheduled with exactly 30 seconds
        verify(handler).postDelayed(any(), eq(30000L))
    }

    @Test
    fun testOnDestroy_removesCallbacks() {
        clipboardHelper.copyToClipboard("password")
        clipboardHelper.onDestroy()

        // Verify callbacks are cleared
        verify(handler).removeCallbacks(any())
    }
}
