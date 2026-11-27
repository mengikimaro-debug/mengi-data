package tz.mengidata.app.security

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * In-memory coordination state between VaultUninstallAccessibilityService and UninstallAuthActivity.
 * Grants a short owner-verified grace window strictly after Fingerprint authentication succeeds,
 * and prevents duplicate auth prompts from rapid Android accessibility events.
 */
object UninstallGuardSession {
    private val unlockedUntilMs = AtomicLong(0L)
    private val lastPromptAtMs = AtomicLong(0L)
    private val authActivityVisible = AtomicBoolean(false)

    fun grantTemporaryUnlock(durationMs: Long = 45_000L) {
        unlockedUntilMs.set(System.currentTimeMillis() + durationMs)
    }

    fun revokeTemporaryUnlock() {
        unlockedUntilMs.set(0L)
    }

    fun isTemporarilyUnlocked(): Boolean {
        return System.currentTimeMillis() < unlockedUntilMs.get()
    }

    fun setAuthActivityVisible(visible: Boolean) {
        authActivityVisible.set(visible)
    }

    fun canLaunchAuthPromptNow(cooldownMs: Long = 1_800L): Boolean {
        if (authActivityVisible.get()) return false
        val now = System.currentTimeMillis()
        val prev = lastPromptAtMs.get()
        if (now - prev < cooldownMs) return false
        return lastPromptAtMs.compareAndSet(prev, now)
    }
}
