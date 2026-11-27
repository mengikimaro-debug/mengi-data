package tz.mengidata.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import tz.mengidata.app.data.currency.CurrencyExchangeService
import tz.mengidata.app.i18n.FormatUtils

class CurrencyFormattingTest {

    @Test
    fun supportedCurrenciesIncludeFiveInternationalPlusTzs() {
        val codes = FormatUtils.supportedCurrencies.map { it.code }
        assertEquals(listOf("TZS", "USD", "EUR", "GBP", "AED", "CNY"), codes)

        val intlCodes = FormatUtils.internationalCurrencies.map { it.code }
        assertEquals(5, intlCodes.size)
        assertEquals(listOf("USD", "EUR", "GBP", "AED", "CNY"), intlCodes)

        assertFalse(FormatUtils.findCurrency("TZS").isInternational)
        assertTrue(FormatUtils.findCurrency("USD").isInternational)
        assertEquals("د.إ", FormatUtils.findCurrency("AED").symbol)
    }

    @Test
    fun normalizeCurrencyCodeHandlesCaseWhitespaceAndUnknown() {
        assertEquals("USD", FormatUtils.normalizeCurrencyCode(" usd "))
        assertEquals("EUR", FormatUtils.normalizeCurrencyCode("eur"))
        assertEquals("GBP", FormatUtils.normalizeCurrencyCode("GBP"))
        assertEquals("AED", FormatUtils.normalizeCurrencyCode("aed"))
        assertEquals("CNY", FormatUtils.normalizeCurrencyCode("cny"))
        assertEquals("TZS", FormatUtils.normalizeCurrencyCode(""))
        assertEquals("TZS", FormatUtils.normalizeCurrencyCode("UNKNOWN"))
    }

    @Test
    fun formatMoneyPreservesWholeTzsAndSupportsInternationalDecimals() {
        assertEquals("TZS 50,000", FormatUtils.formatMoney(50000.0, "TZS"))
        assertEquals("-TZS 12,500", FormatUtils.formatMoney(-12500.0, "TZS"))

        assertEquals("USD 1,250", FormatUtils.formatMoney(1250.0, "USD"))
        assertEquals("USD 1,250.75", FormatUtils.formatMoney(1250.75, "USD"))
        assertEquals("-EUR 99.50", FormatUtils.formatMoney(-99.5, "EUR"))
        assertEquals("GBP 0", FormatUtils.formatMoney(-0.0, "GBP"))
        assertEquals("AED 450.25", FormatUtils.formatMoney(450.25, "AED"))
        assertEquals("CNY 8,800", FormatUtils.formatMoney(8800.0, "CNY"))
    }

    @Test
    fun formatCompactMoneyUsesActiveCurrencyCode() {
        assertEquals("USD 2.5M", FormatUtils.formatCompactMoney(2_500_000.0, "USD"))
        assertEquals("EUR 25K", FormatUtils.formatCompactMoney(25_000.0, "EUR"))
        assertEquals("GBP 450.50", FormatUtils.formatCompactMoney(450.5, "GBP"))
    }

    @Test
    fun formatEditableAmountPreservesInternationalDecimals() {
        assertEquals("50000", FormatUtils.formatEditableAmount(50000.4, "TZS"))
        assertEquals("18.87", FormatUtils.formatEditableAmount(18.8679, "USD"))
        assertEquals("125", FormatUtils.formatEditableAmount(125.0, "EUR"))
        assertEquals("", FormatUtils.formatEditableAmount(0.0, "USD"))
    }

    @Test
    fun conversionFactorIsAccurateAndReversibleAcrossAllCurrencies() {
        val rates = mapOf(
            "USD" to 1.0,
            "TZS" to 2650.0,
            "EUR" to 0.92,
            "GBP" to 0.79,
            "AED" to 3.6725,
            "CNY" to 7.25
        )
        val initialTzs = 265_000.0
        val inUsd = initialTzs * CurrencyExchangeService.conversionFactor("TZS", "USD", rates)
        assertEquals(100.0, inUsd, 0.0001)

        val inEur = inUsd * CurrencyExchangeService.conversionFactor("USD", "EUR", rates)
        assertEquals(92.0, inEur, 0.0001)

        val inGbp = inEur * CurrencyExchangeService.conversionFactor("EUR", "GBP", rates)
        assertEquals(79.0, inGbp, 0.0001)

        val inAed = inGbp * CurrencyExchangeService.conversionFactor("GBP", "AED", rates)
        assertEquals(367.25, inAed, 0.0001)

        val inCny = inAed * CurrencyExchangeService.conversionFactor("AED", "CNY", rates)
        assertEquals(725.0, inCny, 0.0001)

        val backToTzs = inCny * CurrencyExchangeService.conversionFactor("CNY", "TZS", rates)
        assertEquals(initialTzs, backToTzs, 0.0001)
    }

    @Test
    fun parseRatesJsonValidatesLiveMarketPayload() {
        val samplePayload = """
            {
              "result": "success",
              "base_code": "USD",
              "rates": {
                "USD": 1,
                "TZS": 2680.5,
                "EUR": 0.915,
                "GBP": 0.785,
                "AED": 3.6725,
                "CNY": 7.21
              }
            }
        """.trimIndent()
        val parsed = CurrencyExchangeService.parseRatesJson(samplePayload)
        assertNotNull(parsed)
        assertEquals(2680.5, parsed!!["TZS"]!!, 0.0001)
        assertEquals(0.915, parsed["EUR"]!!, 0.0001)
        assertEquals(3.6725, parsed["AED"]!!, 0.0001)

        assertNull(CurrencyExchangeService.parseRatesJson("{}"))
        assertNull(CurrencyExchangeService.parseRatesJson("not-json"))
    }

    @Test
    fun formatRatePairUsesSymbolsCleanly() {
        val rates = CurrencyExchangeService.DEFAULT_RATES_PER_USD
        assertEquals("1 $ ≈ 2,650 TSh", CurrencyExchangeService.formatRatePair("TZS", "USD", rates))
        assertEquals("1 $ ≈ 2,650 TSh", CurrencyExchangeService.formatRatePair("USD", "TZS", rates))
    }
}
