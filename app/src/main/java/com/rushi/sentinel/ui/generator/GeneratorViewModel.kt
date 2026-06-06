package com.rushi.sentinel.ui.generator

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.SecureRandom
import javax.inject.Inject

enum class PasswordStrength {
    WEAK,
    MEDIUM,
    STRONG,
    VERY_STRONG
}

@HiltViewModel
class GeneratorViewModel @Inject constructor() : ViewModel() {

    private val random = SecureRandom()

    private val _length = MutableStateFlow(16)
    val length: StateFlow<Int> = _length.asStateFlow()

    private val _includeUppercase = MutableStateFlow(true)
    val includeUppercase: StateFlow<Boolean> = _includeUppercase.asStateFlow()

    private val _includeLowercase = MutableStateFlow(true)
    val includeLowercase: StateFlow<Boolean> = _includeLowercase.asStateFlow()

    private val _includeNumbers = MutableStateFlow(true)
    val includeNumbers: StateFlow<Boolean> = _includeNumbers.asStateFlow()

    private val _includeSymbols = MutableStateFlow(true)
    val includeSymbols: StateFlow<Boolean> = _includeSymbols.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    private val _strength = MutableStateFlow(PasswordStrength.STRONG)
    val strength: StateFlow<PasswordStrength> = _strength.asStateFlow()

    init {
        generatePassword()
    }

    fun setLength(newLength: Int) {
        if (newLength in 8..64) {
            _length.value = newLength
            generatePassword()
        }
    }

    fun toggleUppercase() {
        // Enforce at least one character set active
        if (_includeUppercase.value && !isAnyOtherActive(excludeUpper = true)) return
        _includeUppercase.value = !_includeUppercase.value
        generatePassword()
    }

    fun toggleLowercase() {
        if (_includeLowercase.value && !isAnyOtherActive(excludeLower = true)) return
        _includeLowercase.value = !_includeLowercase.value
        generatePassword()
    }

    fun toggleNumbers() {
        if (_includeNumbers.value && !isAnyOtherActive(excludeNumbers = true)) return
        _includeNumbers.value = !_includeNumbers.value
        generatePassword()
    }

    fun toggleSymbols() {
        if (_includeSymbols.value && !isAnyOtherActive(excludeSymbols = true)) return
        _includeSymbols.value = !_includeSymbols.value
        generatePassword()
    }

    private fun isAnyOtherActive(
        excludeUpper: Boolean = false,
        excludeLower: Boolean = false,
        excludeNumbers: Boolean = false,
        excludeSymbols: Boolean = false
    ): Boolean {
        return (!excludeUpper && _includeUppercase.value) ||
                (!excludeLower && _includeLowercase.value) ||
                (!excludeNumbers && _includeNumbers.value) ||
                (!excludeSymbols && _includeSymbols.value)
    }

    fun generatePassword() {
        val uppercaseChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        val lowercaseChars = "abcdefghijklmnopqrstuvwxyz"
        val digitChars = "0123456789"
        val symbolChars = "!@#$%^&*()_-+=[]{}|;:',.<>?/"

        val charPool = StringBuilder()
        val guaranteedChars = mutableListOf<Char>()

        if (_includeUppercase.value) {
            charPool.append(uppercaseChars)
            guaranteedChars.add(uppercaseChars[random.nextInt(uppercaseChars.length)])
        }
        if (_includeLowercase.value) {
            charPool.append(lowercaseChars)
            guaranteedChars.add(lowercaseChars[random.nextInt(lowercaseChars.length)])
        }
        if (_includeNumbers.value) {
            charPool.append(digitChars)
            guaranteedChars.add(digitChars[random.nextInt(digitChars.length)])
        }
        if (_includeSymbols.value) {
            charPool.append(symbolChars)
            guaranteedChars.add(symbolChars[random.nextInt(symbolChars.length)])
        }

        if (charPool.isEmpty()) {
            _password.value = ""
            calculateStrength()
            return
        }

        val len = _length.value
        val remainingLength = len - guaranteedChars.size
        for (i in 0 until remainingLength) {
            guaranteedChars.add(charPool[random.nextInt(charPool.length)])
        }

        // Shuffle securely
        guaranteedChars.shuffle(random)
        _password.value = guaranteedChars.joinToString("")
        calculateStrength()
    }

    private fun calculateStrength() {
        val len = _length.value
        val activeCharsets = listOf(
            _includeUppercase.value,
            _includeLowercase.value,
            _includeNumbers.value,
            _includeSymbols.value
        ).count { it }

        _strength.value = when {
            len < 10 || activeCharsets <= 1 -> PasswordStrength.WEAK
            len in 10..13 && activeCharsets >= 2 -> PasswordStrength.MEDIUM
            len in 14..17 && activeCharsets >= 3 -> PasswordStrength.STRONG
            len >= 18 && activeCharsets >= 3 -> PasswordStrength.VERY_STRONG
            else -> PasswordStrength.MEDIUM
        }
    }
}
