package com.example.mathapplock.ui.main

import android.content.SharedPreferences
import android.graphics.drawable.Drawable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mathapplock.Grade8MathEngine
import com.example.mathapplock.MathQuestion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AppInfoItem(
    val name: String,
    val packageName: String,
    val icon: Drawable,
    val isLocked: Boolean
)

sealed interface MainScreenUiState {
    object Loading : MainScreenUiState
    data class Error(val throwable: Throwable) : MainScreenUiState
    data class Success(val apps: List<AppInfoItem>) : MainScreenUiState
}

data class UnlockChallenge(
    val packageName: String,
    val question: MathQuestion
)

class MainScreenViewModel(
    private val sharedPrefs: SharedPreferences,
    private val fetchApps: () -> List<AppInfoItem>,
    private val sendUpdateBroadcast: () -> Unit,
    private val mathEngine: Grade8MathEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow<MainScreenUiState>(MainScreenUiState.Loading)
    val uiState: StateFlow<MainScreenUiState> = _uiState.asStateFlow()

    private val _challengeState = MutableStateFlow<UnlockChallenge?>(null)
    val challengeState: StateFlow<UnlockChallenge?> = _challengeState.asStateFlow()

    init {
        loadInstalledApps()
    }

    fun loadInstalledApps() {
        _uiState.value = MainScreenUiState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val list = fetchApps()
                _uiState.value = MainScreenUiState.Success(list)
            } catch (e: Exception) {
                _uiState.value = MainScreenUiState.Error(e)
            }
        }
    }

    fun toggleAppLock(packageName: String) {
        val currentState = _uiState.value
        if (currentState is MainScreenUiState.Success) {
            val app = currentState.apps.find { it.packageName == packageName } ?: return
            if (app.isLocked) {
                // Require verification challenge before unlocking/unchecking
                val question = mathEngine.generateRandomQuestion()
                _challengeState.value = UnlockChallenge(packageName, question)
            } else {
                // Lock app immediately
                val updatedList = currentState.apps.map {
                    if (it.packageName == packageName) {
                        it.copy(isLocked = true)
                    } else {
                        it
                    }
                }
                _uiState.value = MainScreenUiState.Success(updatedList)

                // Persist changes
                val lockedSet = updatedList.filter { it.isLocked }.map { it.packageName }.toSet()
                sharedPrefs.edit().putStringSet("locked_packages", lockedSet).apply()

                // Trigger broadcast callback
                sendUpdateBroadcast()
            }
        }
    }

    fun verifyUnlockChallenge(userAnswer: Int): Boolean {
        val challenge = _challengeState.value ?: return false
        if (userAnswer == challenge.question.correctAnswer) {
            // Correct answer: unlock app
            performUnlock(challenge.packageName)
            _challengeState.value = null
            return true
        }
        return false
    }

    fun cancelUnlockChallenge() {
        _challengeState.value = null
    }

    private fun performUnlock(packageName: String) {
        val currentState = _uiState.value
        if (currentState is MainScreenUiState.Success) {
            val updatedList = currentState.apps.map {
                if (it.packageName == packageName) {
                    it.copy(isLocked = false)
                } else {
                    it
                }
            }
            _uiState.value = MainScreenUiState.Success(updatedList)

            // Persist changes
            val lockedSet = updatedList.filter { it.isLocked }.map { it.packageName }.toSet()
            sharedPrefs.edit().putStringSet("locked_packages", lockedSet).apply()

            // Trigger broadcast callback
            sendUpdateBroadcast()
        }
    }
}
