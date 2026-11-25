package tz.mengidata.app.data.prefs

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import tz.mengidata.app.i18n.AppLanguage
import tz.mengidata.app.i18n.ThemeMode

class AppPreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(ThemeMode.fromKey(prefs.getString(KEY_THEME, ThemeMode.SYSTEM.key)))
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _dynamicColorEnabled = MutableStateFlow(prefs.getBoolean(KEY_DYNAMIC_COLOR, false))
    val dynamicColorEnabled: StateFlow<Boolean> = _dynamicColorEnabled.asStateFlow()

    private val _highContrastEnabled = MutableStateFlow(prefs.getBoolean(KEY_HIGH_CONTRAST, false))
    val highContrastEnabled: StateFlow<Boolean> = _highContrastEnabled.asStateFlow()

    private val _language = MutableStateFlow(
        if (prefs.getString(KEY_LANGUAGE, AppLanguage.SW.code) == AppLanguage.EN.code) {
            AppLanguage.EN
        } else {
            AppLanguage.SW
        }
    )
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    private val _autoUpdateEnabled = MutableStateFlow(prefs.getBoolean(KEY_AUTO_UPDATE, false))
    val autoUpdateEnabled: StateFlow<Boolean> = _autoUpdateEnabled.asStateFlow()

    private val _autoPhoneBackupEnabled = MutableStateFlow(prefs.getBoolean(KEY_AUTO_PHONE_BACKUP, true))
    val autoPhoneBackupEnabled: StateFlow<Boolean> = _autoPhoneBackupEnabled.asStateFlow()

    private val _customBackupFolderUri = MutableStateFlow(prefs.getString(KEY_CUSTOM_BACKUP_FOLDER_URI, "") ?: "")
    val customBackupFolderUri: StateFlow<String> = _customBackupFolderUri.asStateFlow()

    private val _privacyMaskEnabled = MutableStateFlow(prefs.getBoolean(KEY_PRIVACY_MASK, false))
    val privacyMaskEnabled: StateFlow<Boolean> = _privacyMaskEnabled.asStateFlow()

    private val _pinLockEnabled = MutableStateFlow(
        !prefs.getString(KEY_PIN_HASH, null).isNullOrBlank() &&
            !prefs.getString(KEY_PIN_SALT, null).isNullOrBlank()
    )
    val pinLockEnabled: StateFlow<Boolean> = _pinLockEnabled.asStateFlow()

    private val _notificationsEnabled = MutableStateFlow(prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true))
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    private val _billAlertsEnabled = MutableStateFlow(prefs.getBoolean(KEY_BILL_ALERTS_ENABLED, true))
    val billAlertsEnabled: StateFlow<Boolean> = _billAlertsEnabled.asStateFlow()

    private val _transactionAlertsEnabled = MutableStateFlow(prefs.getBoolean(KEY_TXN_ALERTS_ENABLED, true))
    val transactionAlertsEnabled: StateFlow<Boolean> = _transactionAlertsEnabled.asStateFlow()

    private val _savingsAlertsEnabled = MutableStateFlow(prefs.getBoolean(KEY_SAVINGS_ALERTS_ENABLED, true))
    val savingsAlertsEnabled: StateFlow<Boolean> = _savingsAlertsEnabled.asStateFlow()

    private val _systemAlertsEnabled = MutableStateFlow(prefs.getBoolean(KEY_SYSTEM_ALERTS_ENABLED, true))
    val systemAlertsEnabled: StateFlow<Boolean> = _systemAlertsEnabled.asStateFlow()

    private val _largeBillThreshold = MutableStateFlow(
        prefs.getFloat(KEY_LARGE_BILL_THRESHOLD, DEFAULT_LARGE_BILL_THRESHOLD.toFloat()).toDouble()
    )
    val largeBillThreshold: StateFlow<Double> = _largeBillThreshold.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME, mode.key).apply()
        _themeMode.value = mode
    }

    fun setDynamicColorEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DYNAMIC_COLOR, enabled).apply()
        _dynamicColorEnabled.value = enabled
    }

    fun setHighContrastEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_HIGH_CONTRAST, enabled).apply()
        _highContrastEnabled.value = enabled
    }

    fun setLanguage(lang: AppLanguage) {
        prefs.edit().putString(KEY_LANGUAGE, lang.code).apply()
        _language.value = lang
    }

    fun toggleLanguage() {
        val next = if (_language.value == AppLanguage.SW) AppLanguage.EN else AppLanguage.SW
        setLanguage(next)
    }

    fun setAutoUpdateEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_UPDATE, enabled).apply()
        _autoUpdateEnabled.value = enabled
    }

    fun setAutoPhoneBackupEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_PHONE_BACKUP, enabled).apply()
        _autoPhoneBackupEnabled.value = enabled
    }

    fun setCustomBackupFolderUri(uriString: String) {
        prefs.edit().putString(KEY_CUSTOM_BACKUP_FOLDER_URI, uriString).apply()
        _customBackupFolderUri.value = uriString
    }

    fun setPrivacyMaskEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_PRIVACY_MASK, enabled).apply()
        _privacyMaskEnabled.value = enabled
    }

    fun togglePrivacyMask() {
        setPrivacyMaskEnabled(!_privacyMaskEnabled.value)
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled).apply()
        _notificationsEnabled.value = enabled
    }

    fun setBillAlertsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BILL_ALERTS_ENABLED, enabled).apply()
        _billAlertsEnabled.value = enabled
    }

    fun setTransactionAlertsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_TXN_ALERTS_ENABLED, enabled).apply()
        _transactionAlertsEnabled.value = enabled
    }

    fun setSavingsAlertsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SAVINGS_ALERTS_ENABLED, enabled).apply()
        _savingsAlertsEnabled.value = enabled
    }

    fun setSystemAlertsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SYSTEM_ALERTS_ENABLED, enabled).apply()
        _systemAlertsEnabled.value = enabled
    }

    fun setLargeBillThreshold(amount: Double) {
        val safe = amount.coerceAtLeast(1000.0)
        prefs.edit().putFloat(KEY_LARGE_BILL_THRESHOLD, safe.toFloat()).apply()
        _largeBillThreshold.value = safe
    }

    fun saveAppPin(pin: String): Boolean {
        if (!PinSecurityHelper.isValidPin(pin)) return false
        val salt = PinSecurityHelper.generateSalt()
        val hash = PinSecurityHelper.hashPin(pin, salt)
        prefs.edit()
            .putString(KEY_PIN_SALT, salt)
            .putString(KEY_PIN_HASH, hash)
            .apply()
        _pinLockEnabled.value = true
        return true
    }

    fun verifyAppPin(pin: String): Boolean {
        val salt = prefs.getString(KEY_PIN_SALT, "") ?: ""
        val hash = prefs.getString(KEY_PIN_HASH, "") ?: ""
        return PinSecurityHelper.verifyPin(pin, salt, hash)
    }

    fun clearAppPin() {
        prefs.edit()
            .remove(KEY_PIN_SALT)
            .remove(KEY_PIN_HASH)
            .apply()
        _pinLockEnabled.value = false
    }

    fun shouldAutoCheckUpdates(): Boolean {
        if (!_autoUpdateEnabled.value) return false
        val now = System.currentTimeMillis()
        val last = prefs.getLong(KEY_LAST_UPDATE_CHECK, 0L)
        if (now - last < TWELVE_HOURS_MS) return false
        prefs.edit().putLong(KEY_LAST_UPDATE_CHECK, now).apply()
        return true
    }

    fun shouldRunDailyDueReminder(todayIso: String): Boolean {
        val last = prefs.getString(KEY_LAST_DUE_REMINDER_DATE, null)
        if (last == todayIso) return false
        prefs.edit().putString(KEY_LAST_DUE_REMINDER_DATE, todayIso).apply()
        return true
    }

    fun isSeeded(): Boolean = prefs.getBoolean(KEY_INITIAL_SEEDED, false)

    fun markSeeded() {
        prefs.edit().putBoolean(KEY_INITIAL_SEEDED, true).apply()
    }

    companion object {
        const val DEFAULT_LARGE_BILL_THRESHOLD = 100_000.0
        private const val PREFS_NAME = "mengidata_preferences"
        private const val KEY_THEME = "theme_mode"
        private const val KEY_DYNAMIC_COLOR = "dynamic_color_enabled"
        private const val KEY_HIGH_CONTRAST = "high_contrast_enabled"
        private const val KEY_LANGUAGE = "app_language"
        private const val KEY_AUTO_UPDATE = "auto_update_enabled"
        private const val KEY_AUTO_PHONE_BACKUP = "auto_phone_backup_enabled"
        private const val KEY_CUSTOM_BACKUP_FOLDER_URI = "custom_backup_folder_uri"
        private const val KEY_PRIVACY_MASK = "privacy_mask_enabled"
        private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
        private const val KEY_BILL_ALERTS_ENABLED = "bill_alerts_enabled"
        private const val KEY_TXN_ALERTS_ENABLED = "txn_alerts_enabled"
        private const val KEY_SAVINGS_ALERTS_ENABLED = "savings_alerts_enabled"
        private const val KEY_SYSTEM_ALERTS_ENABLED = "system_alerts_enabled"
        private const val KEY_LARGE_BILL_THRESHOLD = "large_bill_threshold_tzs"
        private const val KEY_PIN_SALT = "app_pin_salt"
        private const val KEY_PIN_HASH = "app_pin_hash"
        private const val KEY_LAST_UPDATE_CHECK = "last_update_check_ms"
        private const val KEY_LAST_DUE_REMINDER_DATE = "last_due_reminder_date"
        private const val KEY_INITIAL_SEEDED = "initial_db_seeded"
        private const val TWELVE_HOURS_MS = 12L * 60L * 60L * 1000L
    }
}
