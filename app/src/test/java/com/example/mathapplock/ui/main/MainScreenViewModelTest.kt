package com.example.mathapplock.ui.main

import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import com.example.mathapplock.Grade8MathEngine
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class MainScreenViewModelTest {

    private val fakePrefs = FakeSharedPreferences()
    private val sampleApps = listOf(
        AppInfoItem("Calculator", "com.android.calculator2", ColorDrawable(Color.RED), false),
        AppInfoItem("Settings", "com.android.settings", ColorDrawable(Color.BLUE), false)
    )

    private var broadcastSentCount = 0
    private val sendBroadcast: () -> Unit = { 
        broadcastSentCount++
    }

    // Mock string-based math engine
    private val mockMathEngine = Grade8MathEngine { _, _ -> "Mock Math Question" }

    @Test
    fun testViewModelLoadsApps() = runTest {
        val viewModel = MainScreenViewModel(
            sharedPrefs = fakePrefs,
            fetchApps = { sampleApps },
            sendUpdateBroadcast = sendBroadcast,
            mathEngine = mockMathEngine
        )

        // Wait for Success state
        val state = viewModel.uiState.first { it is MainScreenUiState.Success } as MainScreenUiState.Success
        assertEquals(2, state.apps.size)
        assertEquals("Calculator", state.apps[0].name)
        assertEquals("Settings", state.apps[1].name)
    }

    @Test
    fun testToggleAppLockImmediatelyLocks() = runTest {
        val viewModel = MainScreenViewModel(
            sharedPrefs = fakePrefs,
            fetchApps = { sampleApps },
            sendUpdateBroadcast = sendBroadcast,
            mathEngine = mockMathEngine
        )

        // Wait for Success state
        viewModel.uiState.first { it is MainScreenUiState.Success }

        // Toggle Calculator lock (initially false)
        viewModel.toggleAppLock("com.android.calculator2")

        // Verify state is updated and locked immediately
        val state = viewModel.uiState.value as MainScreenUiState.Success
        assertTrue("Calculator should be locked", state.apps.first { it.packageName == "com.android.calculator2" }.isLocked)

        // Verify persisted to fakePrefs
        val lockedSet = fakePrefs.getStringSet("locked_packages", emptySet()) ?: emptySet()
        assertTrue(lockedSet.contains("com.android.calculator2"))

        // Verify broadcast is sent
        assertEquals(1, broadcastSentCount)
    }

    @Test
    fun testUnlockRequiresChallengeVerifyAndCommit() = runTest {
        // Prepare pre-locked apps
        val preLockedApps = listOf(
            AppInfoItem("Calculator", "com.android.calculator2", ColorDrawable(Color.RED), true)
        )

        val viewModel = MainScreenViewModel(
            sharedPrefs = fakePrefs,
            fetchApps = { preLockedApps },
            sendUpdateBroadcast = sendBroadcast,
            mathEngine = mockMathEngine
        )

        // Wait for Success state
        viewModel.uiState.first { it is MainScreenUiState.Success }

        // Toggle Calculator lock (initially true) -> triggers unlock challenge
        viewModel.toggleAppLock("com.android.calculator2")

        // Verify it did NOT unlock immediately
        var state = viewModel.uiState.value as MainScreenUiState.Success
        assertTrue("Calculator should still be locked before verification", state.apps.first { it.packageName == "com.android.calculator2" }.isLocked)

        // Verify challenge state is populated
        val challenge = viewModel.challengeState.value
        assertNotNull("Unlock challenge should be active", challenge)
        assertEquals("com.android.calculator2", challenge?.packageName)
        
        val correctAnswer = challenge!!.question.correctAnswer

        // Verify wrong answer does not unlock
        val isCorrectWrong = viewModel.verifyUnlockChallenge(correctAnswer + 1)
        assertFalse(isCorrectWrong)
        state = viewModel.uiState.value as MainScreenUiState.Success
        assertTrue("Calculator should remain locked after incorrect answer", state.apps.first { it.packageName == "com.android.calculator2" }.isLocked)
        assertNotNull("Challenge should still be active", viewModel.challengeState.value)

        // Verify correct answer verifies, dismisses challenge, and commits unlock
        val isCorrectRight = viewModel.verifyUnlockChallenge(correctAnswer)
        assertTrue(isCorrectRight)
        state = viewModel.uiState.value as MainScreenUiState.Success
        assertFalse("Calculator should be unlocked now", state.apps.first { it.packageName == "com.android.calculator2" }.isLocked)
        assertNull("Challenge should be cleared", viewModel.challengeState.value)
    }
}

class FakeSharedPreferences : SharedPreferences {
    private val map = mutableMapOf<String, Any?>()

    override fun getAll(): Map<String, *> = map
    override fun getString(key: String, defValue: String?): String? = map[key] as? String ?: defValue
    override fun getStringSet(key: String, defValues: Set<String>?): Set<String>? = map[key] as? Set<String> ?: defValues
    override fun getInt(key: String, defValue: Int): Int = map[key] as? Int ?: defValue
    override fun getLong(key: String, defValue: Long): Long = map[key] as? Long ?: defValue
    override fun getFloat(key: String, defValue: Float): Float = map[key] as? Float ?: defValue
    override fun getBoolean(key: String, defValue: Boolean): Boolean = map[key] as? Boolean ?: defValue
    override fun contains(key: String): Boolean = map.containsKey(key)
    override fun edit(): SharedPreferences.Editor = FakeEditor(map)
    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

    class FakeEditor(private val map: MutableMap<String, Any?>) : SharedPreferences.Editor {
        private val tempMap = mutableMapOf<String, Any?>()

        override fun putString(key: String, value: String?): SharedPreferences.Editor { tempMap[key] = value; return this }
        override fun putStringSet(key: String, values: Set<String>?): SharedPreferences.Editor { tempMap[key] = values; return this }
        override fun putInt(key: String, value: Int): SharedPreferences.Editor { tempMap[key] = value; return this }
        override fun putLong(key: String, value: Long): SharedPreferences.Editor { tempMap[key] = value; return this }
        override fun putFloat(key: String, value: Float): SharedPreferences.Editor { tempMap[key] = value; return this }
        override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor { tempMap[key] = value; return this }
        override fun remove(key: String): SharedPreferences.Editor { tempMap.remove(key); return this }
        override fun clear(): SharedPreferences.Editor { tempMap.clear(); return this }
        override fun commit(): Boolean { map.putAll(tempMap); return true }
        override fun apply() { map.putAll(tempMap) }
    }
}
