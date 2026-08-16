package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("creatorkit_prefs", Context.MODE_PRIVATE)

    private val _aiCredits = MutableStateFlow(getRemainingCreditsInternal())
    val aiCredits: StateFlow<Int> = _aiCredits.asStateFlow()

    private val _isPremium = MutableStateFlow(prefs.getBoolean(KEY_IS_PREMIUM, false))
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    private val _themeMode = MutableStateFlow(prefs.getString(KEY_THEME, "dark") ?: "dark")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _isOnboardingCompleted = MutableStateFlow(prefs.getBoolean(KEY_ONBOARDING_DONE, false))
    val isOnboardingCompleted: StateFlow<Boolean> = _isOnboardingCompleted.asStateFlow()

    private val _defaultFormat = MutableStateFlow(prefs.getString(KEY_DEFAULT_FORMAT, "WEBP") ?: "WEBP")
    val defaultFormat: StateFlow<String> = _defaultFormat.asStateFlow()

    private val _defaultQuality = MutableStateFlow(prefs.getInt(KEY_DEFAULT_QUALITY, 90))
    val defaultQuality: StateFlow<Int> = _defaultQuality.asStateFlow()

    init {
        checkAndResetDailyCredits()
    }

    private fun getTodayDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(Date())
    }

    private fun checkAndResetDailyCredits() {
        val lastDate = prefs.getString(KEY_LAST_CREDIT_DATE, "")
        val today = getTodayDateString()
        if (lastDate != today) {
            val dailyCredits = if (prefs.getBoolean(KEY_IS_PREMIUM, false)) PREMIUM_DAILY_CREDITS else FREE_DAILY_CREDITS
            prefs.edit()
                .putString(KEY_LAST_CREDIT_DATE, today)
                .putInt(KEY_AI_CREDITS, dailyCredits)
                .apply()
            _aiCredits.value = dailyCredits
        }
    }

    private fun getRemainingCreditsInternal(): Int {
        val lastDate = prefs.getString(KEY_LAST_CREDIT_DATE, "")
        val today = getTodayDateString()
        if (lastDate != today) {
            val dailyCredits = if (prefs.getBoolean(KEY_IS_PREMIUM, false)) PREMIUM_DAILY_CREDITS else FREE_DAILY_CREDITS
            prefs.edit()
                .putString(KEY_LAST_CREDIT_DATE, today)
                .putInt(KEY_AI_CREDITS, dailyCredits)
                .apply()
            return dailyCredits
        }
        return prefs.getInt(KEY_AI_CREDITS, if (prefs.getBoolean(KEY_IS_PREMIUM, false)) PREMIUM_DAILY_CREDITS else FREE_DAILY_CREDITS)
    }

    fun useCredit(): Boolean {
        if (_isPremium.value) return true // Unlimited / generous for premium
        val current = _aiCredits.value
        if (current > 0) {
            val updated = current - 1
            prefs.edit().putInt(KEY_AI_CREDITS, updated).apply()
            _aiCredits.value = updated
            return true
        }
        return false
    }

    fun addBonusCredits(amount: Int) {
        val updated = _aiCredits.value + amount
        prefs.edit().putInt(KEY_AI_CREDITS, updated).apply()
        _aiCredits.value = updated
    }

    fun setPremium(isPremium: Boolean) {
        prefs.edit().putBoolean(KEY_IS_PREMIUM, isPremium).apply()
        _isPremium.value = isPremium
        if (isPremium) {
            _aiCredits.value = PREMIUM_DAILY_CREDITS
            prefs.edit().putInt(KEY_AI_CREDITS, PREMIUM_DAILY_CREDITS).apply()
        }
    }

    fun setTheme(theme: String) {
        prefs.edit().putString(KEY_THEME, theme).apply()
        _themeMode.value = theme
    }

    fun setOnboardingCompleted(completed: Boolean) {
        prefs.edit().putBoolean(KEY_ONBOARDING_DONE, completed).apply()
        _isOnboardingCompleted.value = completed
    }

    fun setDefaultFormat(format: String) {
        prefs.edit().putString(KEY_DEFAULT_FORMAT, format).apply()
        _defaultFormat.value = format
    }

    fun setDefaultQuality(quality: Int) {
        prefs.edit().putInt(KEY_DEFAULT_QUALITY, quality).apply()
        _defaultQuality.value = quality
    }

    companion object {
        const val FREE_DAILY_CREDITS = 5
        const val PREMIUM_DAILY_CREDITS = 999

        private const val KEY_AI_CREDITS = "key_ai_credits"
        private const val KEY_LAST_CREDIT_DATE = "key_last_credit_date"
        private const val KEY_IS_PREMIUM = "key_is_premium"
        private const val KEY_THEME = "key_theme_mode"
        private const val KEY_ONBOARDING_DONE = "key_onboarding_done"
        private const val KEY_DEFAULT_FORMAT = "key_default_format"
        private const val KEY_DEFAULT_QUALITY = "key_default_quality"
    }
}
