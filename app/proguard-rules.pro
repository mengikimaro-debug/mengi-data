# Copyright (c) 2024-2026 Mr Reg Kimaro · VaultLedger (tz.mengidata.app). All Rights Reserved.
# Hardened R8 / ProGuard Obfuscation & Anti-Reverse-Engineering Rules

# Obfuscation & repackaging for release builds
-repackageclasses 'vl'
-allowaccessmodification
-optimizationpasses 3
-renamesourcefileattribute SourceFile
-keepattributes *Annotation*, InnerClasses, Signature

# Strip debug & info logging in release builds to protect internal logic
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

# Preserve Room, Serialization, Navigation, Update Receiver, Backup & System Security Entry Points
-keep class tz.mengidata.app.BuildConfig { *; }
-keep class tz.mengidata.app.data.** { *; }
-keep class tz.mengidata.app.ui.navigation.** { *; }
-keep class tz.mengidata.app.updater.UpdateInstallReceiver { *; }
-keep class tz.mengidata.app.backup.VaultAndroidBackupAgent { *; }
-keep class tz.mengidata.app.backup.VaultAutoBackupJobService { *; }
-keep class tz.mengidata.app.notifications.EveningReminderReceiver { *; }
-keep class tz.mengidata.app.security.VaultUninstallGuardAdminReceiver { *; }
-keep class tz.mengidata.app.security.VaultUninstallAccessibilityService { *; }
-keep class tz.mengidata.app.security.UninstallAuthActivity { *; }
-keep class tz.mengidata.app.security.CopyrightVault { *; }
-keep class tz.mengidata.app.security.CopyrightIntegrityReport { *; }
