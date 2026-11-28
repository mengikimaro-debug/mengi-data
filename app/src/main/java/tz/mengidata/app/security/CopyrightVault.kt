/*
 * Copyright (c) 2024–2026 Mr Reg Kimaro · VaultLedger™ (tz.mengidata.app).
 * All Rights Reserved / Hatimiliki Zote Zimehifadhiwa.
 *
 * Proprietary and confidential intellectual property. Unauthorized copying,
 * cloning, repackaging, decompiling, reverse engineering, or redistribution
 * of this application or any part of its system is strictly prohibited.
 */
package tz.mengidata.app.security

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import tz.mengidata.app.BuildConfig
import tz.mengidata.app.i18n.AppLanguage
import tz.mengidata.app.i18n.tr
import java.security.MessageDigest
import java.util.Locale

data class CopyrightIntegrityReport(
    val isAuthentic: Boolean = true,
    val packageMatched: Boolean = true,
    val buildConfigMatched: Boolean = true,
    val hasValidSignature: Boolean = true,
    val installedPackageId: String = CopyrightVault.AUTHORIZED_PACKAGE_ID,
    val signingCertSha256: String = "",
    val signingCertShort: String = "VERIFIED",
    val ownershipSeal: String = "",
    val tamperReasonSw: String? = null,
    val tamperReasonEn: String? = null
) {
    fun tamperReason(language: AppLanguage): String? =
        if (language == AppLanguage.EN) tamperReasonEn else tamperReasonSw
}

/**
 * Centralized Copyright, Intellectual Property & Anti-Cloning Integrity Registry
 * for VaultLedger™ (Mengidata) — © 2024–2026 Mr Reg Kimaro.
 */
object CopyrightVault {

    const val APP_BRAND = "VaultLedger™ (Mengidata)"
    const val COPYRIGHT_HOLDER = "Mr Reg Kimaro"
    const val COPYRIGHT_EMAIL = "mengikimaro@gmail.com"
    const val COPYRIGHT_PHONE = "+255 675 160 551"
    const val COPYRIGHT_YEARS = "2024–2026"
    const val AUTHORIZED_PACKAGE_ID = "tz.mengidata.app"
    const val REGISTRATION_CODE = "VL-TZ-2026-MRK"

    const val NOTICE_SHORT_SW = "© 2024–2026 VaultLedger™ · Mr Reg Kimaro. Hatimiliki Zote Zimehifadhiwa."
    const val NOTICE_SHORT_EN = "© 2024–2026 VaultLedger™ · Mr Reg Kimaro. All Rights Reserved."

    const val PROHIBITION_SW =
        "Programu hii na mfumo wake wote (msimbo chanzo, muundo, injini ya hesabu ya 0% upotevu, ankara za PDF, hifadhi ya mfumo na ulinzi) vinalindwa na sheria ya Hatimiliki. Ni marufuku kunakili (copy), kuiga (clone), kubadilisha Package ID, au kufungua msimbo (decompile) bila kibali cha maandishi cha Mr Reg Kimaro."

    const val PROHIBITION_EN =
        "This application and its complete system architecture (source code, UI design, Zero-Loss accounting engine, PDF/Excel generators, backup vault, and security guards) are protected by Copyright Law. Copying, cloning, repackaging, altering the Package ID, or reverse engineering without written permission from Mr Reg Kimaro is strictly prohibited."

    fun shortNotice(language: AppLanguage): String =
        if (language == AppLanguage.EN) NOTICE_SHORT_EN else NOTICE_SHORT_SW

    fun prohibitionStatement(language: AppLanguage): String =
        if (language == AppLanguage.EN) PROHIBITION_EN else PROHIBITION_SW

    fun pdfFooterBrand(language: AppLanguage): String =
        if (language == AppLanguage.EN) {
            "VaultLedger™ · © $COPYRIGHT_YEARS $COPYRIGHT_HOLDER · All Rights Reserved"
        } else {
            "VaultLedger™ · © $COPYRIGHT_YEARS $COPYRIGHT_HOLDER · Hatimiliki Zimehifadhiwa"
        }

    fun verifyIntegrity(context: Context): CopyrightIntegrityReport {
        val installedPkg = runCatching { context.packageName }.getOrDefault(AUTHORIZED_PACKAGE_ID)
        val buildPkg = BuildConfig.APPLICATION_ID
        val certSha256 = extractInstalledCertSha256(context)
        val isUnitTestRuntime = Build.FINGERPRINT.contains("robolectric", ignoreCase = true) ||
            Build.PRODUCT.contains("robolectric", ignoreCase = true)

        return evaluateIntegrity(
            installedPackageId = installedPkg,
            buildConfigAppId = buildPkg,
            certSha256 = certSha256,
            allowEmptySignature = isUnitTestRuntime
        )
    }

    /**
     * Pure deterministic integrity evaluator (also directly unit-testable).
     */
    fun evaluateIntegrity(
        installedPackageId: String,
        buildConfigAppId: String,
        certSha256: String?,
        allowEmptySignature: Boolean = false
    ): CopyrightIntegrityReport {
        val pkgOk = installedPackageId == AUTHORIZED_PACKAGE_ID
        val buildOk = buildConfigAppId == AUTHORIZED_PACKAGE_ID
        val cleanSha = certSha256?.trim()?.uppercase(Locale.US).orEmpty()
        val sigOk = cleanSha.isNotEmpty() || allowEmptySignature

        val authentic = pkgOk && buildOk && sigOk
        val shortCert = if (cleanSha.length >= 16) {
            "${cleanSha.substring(0, 4)}:${cleanSha.substring(4, 8)}:${cleanSha.substring(8, 12)}:${cleanSha.substring(12, 16)}"
        } else if (cleanSha.isNotEmpty()) {
            cleanSha
        } else if (allowEmptySignature) {
            "TEST-RUNTIME"
        } else {
            "UNVERIFIED"
        }

        val seal = computeOwnershipSeal(
            packageId = installedPackageId,
            certSha256 = cleanSha.ifEmpty { "LOCAL" }
        )

        val (reasonSw, reasonEn) = when {
            !pkgOk -> "Nakala isiyo halali: Package ID '$installedPackageId' si '$AUTHORIZED_PACKAGE_ID'." to
                "Unauthorized clone detected: Package ID '$installedPackageId' does not match '$AUTHORIZED_PACKAGE_ID'."
            !buildOk -> "Msimbo wa ujenzi umebadilishwa ($buildConfigAppId)." to
                "Build identity has been tampered with ($buildConfigAppId)."
            !sigOk -> "Saini ya APK (Signing Certificate) haikuthibitishwa." to
                "APK signing certificate could not be verified."
            else -> null to null
        }

        return CopyrightIntegrityReport(
            isAuthentic = authentic,
            packageMatched = pkgOk,
            buildConfigMatched = buildOk,
            hasValidSignature = sigOk,
            installedPackageId = installedPackageId,
            signingCertSha256 = cleanSha,
            signingCertShort = shortCert,
            ownershipSeal = seal,
            tamperReasonSw = reasonSw,
            tamperReasonEn = reasonEn
        )
    }

    fun computeOwnershipSeal(packageId: String, certSha256: String): String {
        val payload = "$APP_BRAND|$COPYRIGHT_HOLDER|$COPYRIGHT_EMAIL|$REGISTRATION_CODE|$packageId|$certSha256"
        val digest = MessageDigest.getInstance("SHA-256").digest(payload.toByteArray(Charsets.UTF_8))
        val hex = digest.joinToString("") { "%02X".format(it) }
        return "${hex.substring(0, 4)}-${hex.substring(4, 8)}-${hex.substring(8, 12)}-${hex.substring(12, 16)}"
    }

    fun buildCertificateText(report: CopyrightIntegrityReport, language: AppLanguage): String {
        val statusText = if (report.isAuthentic) {
            tr(language, "HALISI & IMELINDWA (AUTHENTIC)", "AUTHENTIC & PROTECTED")
        } else {
            tr(language, "IMECHEZEWA / NAKALA ISIYO HALALI", "TAMPERED / UNAUTHORIZED CLONE")
        }
        return buildString {
            appendLine("==================================================")
            appendLine(tr(language, "HATI RASMI YA HATIMILIKI NA HAKI MILIKI", "OFFICIAL COPYRIGHT & IP CERTIFICATE"))
            appendLine("==================================================")
            appendLine("${tr(language, "Programu", "Application")}: $APP_BRAND v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            appendLine("${tr(language, "Mmiliki / Mwanzilishi", "Copyright Owner")}: $COPYRIGHT_HOLDER")
            appendLine("${tr(language, "Mawasiliano", "Contact")}: $COPYRIGHT_EMAIL | $COPYRIGHT_PHONE")
            appendLine("${tr(language, "Namba ya Usajili wa Mfumo", "System Registry ID")}: $REGISTRATION_CODE")
            appendLine("${tr(language, "Package ID Halisi", "Authorized Package")}: ${report.installedPackageId}")
            appendLine("${tr(language, "Hali ya Ulinzi", "Integrity Status")}: $statusText")
            appendLine("${tr(language, "Muhuri wa Umiliki (Seal)", "Ownership Seal")}: ${report.ownershipSeal}")
            appendLine("${tr(language, "Alama ya Cheti (SHA-256)", "Cert Fingerprint")}: ${report.signingCertShort}")
            appendLine()
            appendLine(shortNotice(language))
            appendLine(prohibitionStatement(language))
        }
    }

    private fun extractInstalledCertSha256(context: Context): String? {
        return runCatching {
            val pm = context.packageManager
            val info = getPackageInfoWithCerts(pm, context.packageName) ?: return@runCatching null
            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val signingInfo = info.signingInfo
                val modern = if (signingInfo != null) {
                    if (signingInfo.hasMultipleSigners()) signingInfo.apkContentsSigners
                    else signingInfo.signingCertificateHistory
                } else {
                    null
                }
                @Suppress("DEPRECATION")
                modern?.takeIf { it.isNotEmpty() } ?: info.signatures
            } else {
                @Suppress("DEPRECATION")
                info.signatures
            }
            val firstSig = signatures?.firstOrNull() ?: return@runCatching null
            val digest = MessageDigest.getInstance("SHA-256").digest(firstSig.toByteArray())
            digest.joinToString("") { "%02X".format(it) }
        }.getOrNull()
    }

    private fun getPackageInfoWithCerts(pm: PackageManager, pkg: String): PackageInfo? {
        val flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            @Suppress("DEPRECATION")
            PackageManager.GET_SIGNATURES
        }
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(pkg, PackageManager.PackageInfoFlags.of(flag.toLong()))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(pkg, flag)
            }
        }.getOrNull()
    }
}
