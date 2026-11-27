package tz.mengidata.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import tz.mengidata.app.data.db.PendingTransactionEntity
import tz.mengidata.app.i18n.FormatUtils
import tz.mengidata.app.notifications.EveningReminderScheduler
import java.util.Calendar
import java.util.TimeZone

class EveningReminderSchedulerTest {

    private val tz: TimeZone = TimeZone.getTimeZone("Africa/Dar_es_Salaam")

    private fun millisFor(year: Int, monthZeroBased: Int, day: Int, hour: Int, minute: Int, second: Int = 0): Long {
        return Calendar.getInstance(tz).apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthZeroBased)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, second)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    @Test
    fun schedulesSameDayEveningWhenCurrentTimeIsMorning() {
        val now = millisFor(2026, Calendar.NOVEMBER, 27, 10, 15)
        val trigger = EveningReminderScheduler.computeNextEveningTriggerMillis(
            nowMillis = now,
            hourOfDay = 19,
            minute = 0,
            timeZone = tz
        )
        val result = Calendar.getInstance(tz).apply { timeInMillis = trigger }
        assertEquals(2026, result.get(Calendar.YEAR))
        assertEquals(Calendar.NOVEMBER, result.get(Calendar.MONTH))
        assertEquals(27, result.get(Calendar.DAY_OF_MONTH))
        assertEquals(19, result.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, result.get(Calendar.MINUTE))
        assertEquals(0, result.get(Calendar.SECOND))
    }

    @Test
    fun schedulesNextDayEveningWhenCurrentTimeIsAfter7Pm() {
        val now = millisFor(2026, Calendar.NOVEMBER, 27, 19, 5)
        val trigger = EveningReminderScheduler.computeNextEveningTriggerMillis(
            nowMillis = now,
            hourOfDay = 19,
            minute = 0,
            timeZone = tz
        )
        val result = Calendar.getInstance(tz).apply { timeInMillis = trigger }
        assertEquals(2026, result.get(Calendar.YEAR))
        assertEquals(Calendar.NOVEMBER, result.get(Calendar.MONTH))
        assertEquals(28, result.get(Calendar.DAY_OF_MONTH))
        assertEquals(19, result.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, result.get(Calendar.MINUTE))
    }

    @Test
    fun schedulesNextDayEveningWhenWithin60SecondBuffer() {
        val now = millisFor(2026, Calendar.NOVEMBER, 27, 18, 59, 30)
        val trigger = EveningReminderScheduler.computeNextEveningTriggerMillis(
            nowMillis = now,
            hourOfDay = 19,
            minute = 0,
            timeZone = tz
        )
        val result = Calendar.getInstance(tz).apply { timeInMillis = trigger }
        assertEquals(28, result.get(Calendar.DAY_OF_MONTH))
        assertEquals(19, result.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, result.get(Calendar.MINUTE))
    }

    @Test
    fun pendingTransactionPreservesDetailsAndFlags() {
        val yesterday = FormatUtils.addDaysIso(FormatUtils.todayIso(), -1)
        val tomorrow = FormatUtils.addDaysIso(FormatUtils.todayIso(), 1)

        val overdueExpense = PendingTransactionEntity(
            id = "ptx_1",
            type = "expense",
            description = "Malipo ya mzigo",
            details = "Lipa kwa M-Pesa baada ya risiti ya duka kuthibitishwa.",
            party = "Kariakoo Suppliers",
            category = "Manunuzi / stock",
            amount = 450_000.0,
            method = "M-Pesa",
            expectedDate = yesterday
        )
        val upcomingIncome = PendingTransactionEntity(
            id = "ptx_2",
            type = "income",
            description = "Malipo ya mteja Juma",
            details = "Anatarajia kutuma awamu ya pili jioni.",
            party = "Juma Hamisi",
            category = "Huduma",
            amount = 250_000.0,
            method = "Benki",
            expectedDate = tomorrow
        )

        assertFalse(overdueExpense.isIncome)
        assertTrue(overdueExpense.isOverdue)
        assertTrue(overdueExpense.details.contains("M-Pesa"))

        assertTrue(upcomingIncome.isIncome)
        assertFalse(upcomingIncome.isOverdue)
        assertEquals("Anatarajia kutuma awamu ya pili jioni.", upcomingIncome.details)
    }

    @Test
    fun pendingTransactionTracksContactInfoPartialAndFullPayment() {
        val yesterday = FormatUtils.addDaysIso(FormatUtils.todayIso(), -1)

        val partial = PendingTransactionEntity(
            id = "ptx_partial",
            type = "income",
            description = "Mauzo ya vifaa",
            details = "Atamalizia salio kesho jioni.",
            party = "Hassan Mwangi",
            phoneNumber = "0754 112 233",
            location = "Kariakoo, Dar es Salaam",
            amount = 500_000.0,
            paidAmount = 200_000.0,
            expectedDate = yesterday
        )
        assertEquals("Hassan Mwangi", partial.party)
        assertEquals("0754 112 233", partial.phoneNumber)
        assertEquals("Kariakoo, Dar es Salaam", partial.location)
        assertEquals(200_000.0, partial.effectivePaidAmount, 0.01)
        assertEquals(300_000.0, partial.remainingAmount, 0.01)
        assertTrue(partial.isPartiallyPaid)
        assertFalse(partial.isFullyPaid)
        assertFalse(partial.isUnpaid)
        assertTrue(partial.isOverdue)
        assertEquals(0.4f, partial.paymentProgressRatio, 0.01f)

        val paidInFull = partial.copy(paidAmount = 500_000.0)
        assertEquals(500_000.0, paidInFull.effectivePaidAmount, 0.01)
        assertEquals(0.0, paidInFull.remainingAmount, 0.01)
        assertTrue(paidInFull.isFullyPaid)
        assertFalse(paidInFull.isPartiallyPaid)
        assertFalse(paidInFull.isOverdue)
        assertEquals(1.0f, paidInFull.paymentProgressRatio, 0.01f)
    }
}
