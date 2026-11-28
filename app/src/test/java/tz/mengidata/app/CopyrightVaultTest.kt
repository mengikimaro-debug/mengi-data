package tz.mengidata.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import tz.mengidata.app.i18n.AppLanguage
import tz.mengidata.app.security.CopyrightVault

class CopyrightVaultTest {

    @Test
    fun authenticBuildPassesAllCopyrightAndAntiCloneChecks() {
        val sampleCert = "A1B2C3D4E5F60718293A4B5C6D7E8F90112233445566778899AABBCCDDEEFF00"
        val report = CopyrightVault.evaluateIntegrity(
            installedPackageId = CopyrightVault.AUTHORIZED_PACKAGE_ID,
            buildConfigAppId = CopyrightVault.AUTHORIZED_PACKAGE_ID,
            certSha256 = sampleCert,
            allowEmptySignature = false
        )

        assertTrue(report.isAuthentic)
        assertTrue(report.packageMatched)
        assertTrue(report.buildConfigMatched)
        assertTrue(report.hasValidSignature)
        assertEquals("A1B2:C3D4:E5F6:0718", report.signingCertShort)
        assertTrue(report.ownershipSeal.matches(Regex("^[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{4}$")))
    }

    @Test
    fun clonedPackageIdIsBlockedByAntiCloneGuard() {
        val sampleCert = "A1B2C3D4E5F60718293A4B5C6D7E8F90112233445566778899AABBCCDDEEFF00"
        val report = CopyrightVault.evaluateIntegrity(
            installedPackageId = "com.pirate.clonedledger",
            buildConfigAppId = CopyrightVault.AUTHORIZED_PACKAGE_ID,
            certSha256 = sampleCert,
            allowEmptySignature = false
        )

        assertFalse(report.isAuthentic)
        assertFalse(report.packageMatched)
        assertNotNull(report.tamperReason(AppLanguage.SW))
        assertNotNull(report.tamperReason(AppLanguage.EN))
        assertTrue(report.tamperReason(AppLanguage.EN)!!.contains("com.pirate.clonedledger"))
    }

    @Test
    fun missingSignatureOnRealDeviceIsFlagged() {
        val report = CopyrightVault.evaluateIntegrity(
            installedPackageId = CopyrightVault.AUTHORIZED_PACKAGE_ID,
            buildConfigAppId = CopyrightVault.AUTHORIZED_PACKAGE_ID,
            certSha256 = null,
            allowEmptySignature = false
        )

        assertFalse(report.isAuthentic)
        assertFalse(report.hasValidSignature)
        assertEquals("UNVERIFIED", report.signingCertShort)
    }

    @Test
    fun ownershipSealAndBilingualCertificateIncludeOwnerAndProhibition() {
        val sealA = CopyrightVault.computeOwnershipSeal("tz.mengidata.app", "CERT_1")
        val sealB = CopyrightVault.computeOwnershipSeal("tz.mengidata.app", "CERT_2")
        assertNotEquals(sealA, sealB)

        val report = CopyrightVault.evaluateIntegrity(
            installedPackageId = CopyrightVault.AUTHORIZED_PACKAGE_ID,
            buildConfigAppId = CopyrightVault.AUTHORIZED_PACKAGE_ID,
            certSha256 = "0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF",
            allowEmptySignature = false
        )
        val certSw = CopyrightVault.buildCertificateText(report, AppLanguage.SW)
        val certEn = CopyrightVault.buildCertificateText(report, AppLanguage.EN)

        assertTrue(certSw.contains(CopyrightVault.COPYRIGHT_HOLDER))
        assertTrue(certSw.contains(CopyrightVault.REGISTRATION_CODE))
        assertTrue(certSw.contains("Hatimiliki Zote Zimehifadhiwa"))
        assertTrue(certEn.contains(CopyrightVault.COPYRIGHT_HOLDER))
        assertTrue(certEn.contains("All Rights Reserved"))
        assertTrue(certEn.contains(report.ownershipSeal))
    }
}
