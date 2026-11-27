package tz.mengidata.app.data.currency

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import tz.mengidata.app.BuildConfig
import tz.mengidata.app.i18n.FormatUtils
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.math.abs

data class ExchangeRateSnapshot(
    val ratesPerUsd: Map<String, Double>,
    val updatedAtMillis: Long,
    val isLive: Boolean
)

object CurrencyExchangeService {

    /**
     * Baseline market exchange rates relative to 1 USD.
     * Used only as an instant fallback when the device is 100% offline and no cached live rate exists yet.
     */
    val DEFAULT_RATES_PER_USD: Map<String, Double> = mapOf(
        "USD" to 1.0,
        "TZS" to 2650.0,
        "EUR" to 0.92,
        "GBP" to 0.79,
        "AED" to 3.6725,
        "CNY" to 7.25
    )

    private val LIVE_ENDPOINTS = listOf(
        "https://open.er-api.com/v6/latest/USD",
        "https://api.exchangerate-api.com/v4/latest/USD"
    )

    private const val CONNECT_TIMEOUT_MS = 3_500
    private const val READ_TIMEOUT_MS = 4_000

    /**
     * Fetches real-time exchange rates relative to 1 USD from public HTTPS market feeds.
     * Returns null if offline or unreachable so caller can seamlessly fall back to cached rates.
     */
    fun fetchLiveRatesPerUsd(): ExchangeRateSnapshot? {
        for (endpoint in LIVE_ENDPOINTS) {
            val parsed = runCatching {
                val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = CONNECT_TIMEOUT_MS
                    readTimeout = READ_TIMEOUT_MS
                    instanceFollowRedirects = true
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("User-Agent", "VaultLedger-Android/${BuildConfig.VERSION_NAME}")
                }
                try {
                    val code = conn.responseCode
                    if (code !in 200..299) return@runCatching null
                    val body = BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8)).use {
                        it.readText()
                    }
                    parseRatesJson(body)
                } finally {
                    conn.disconnect()
                }
            }.getOrNull()

            if (parsed != null && parsed.size >= FormatUtils.supportedCurrencies.size) {
                return ExchangeRateSnapshot(
                    ratesPerUsd = parsed,
                    updatedAtMillis = System.currentTimeMillis(),
                    isLive = true
                )
            }
        }
        return null
    }

    /**
     * Parses a JSON object containing a `"rates"` map (or flat currency map) into a validated
     * map of all supported currencies per 1 USD.
     */
    fun parseRatesJson(json: String): Map<String, Double>? {
        if (json.isBlank()) return null
        return runCatching {
            val root = Json.parseToJsonElement(json).jsonObject
            val ratesObj = (root["rates"] as? JsonObject) ?: root
            val result = LinkedHashMap<String, Double>()
            for (option in FormatUtils.supportedCurrencies) {
                val value = ratesObj[option.code]?.jsonPrimitive?.doubleOrNull
                if (value != null && value.isFinite() && value > 0.0) {
                    result[option.code] = value
                }
            }
            if (!result.containsKey("USD") && result.isNotEmpty()) {
                result["USD"] = 1.0
            }
            val allPresent = FormatUtils.supportedCurrencies.all { (result[it.code] ?: 0.0) > 0.0 }
            if (allPresent) result else null
        }.getOrNull()
    }

    fun serializeRatesJson(rates: Map<String, Double>): String {
        val entries = LinkedHashMap<String, JsonPrimitive>()
        for (option in FormatUtils.supportedCurrencies) {
            val value = rates[option.code]?.takeIf { it.isFinite() && it > 0.0 }
                ?: DEFAULT_RATES_PER_USD[option.code]
                ?: 1.0
            entries[option.code] = JsonPrimitive(value)
        }
        return JsonObject(entries).toString()
    }

    /**
     * Computes exact multiplicative factor to convert an amount in [fromCode] to [toCode].
     * Example: TZS -> USD with TZS=2650, USD=1.0 yields 1.0 / 2650.0.
     */
    fun conversionFactor(
        fromCode: String,
        toCode: String,
        ratesPerUsd: Map<String, Double>
    ): Double {
        val from = FormatUtils.normalizeCurrencyCode(fromCode)
        val to = FormatUtils.normalizeCurrencyCode(toCode)
        if (from == to) return 1.0
        val fromRate = ratesPerUsd[from]?.takeIf { it.isFinite() && it > 0.0 }
            ?: DEFAULT_RATES_PER_USD[from]
            ?: 1.0
        val toRate = ratesPerUsd[to]?.takeIf { it.isFinite() && it > 0.0 }
            ?: DEFAULT_RATES_PER_USD[to]
            ?: 1.0
        val factor = toRate / fromRate
        return if (factor.isFinite() && factor > 0.0) factor else 1.0
    }

    /**
     * Human-readable compact rate string using symbols (e.g. "1 $ ≈ 2,650 TSh" or "1 € ≈ 1.09 $").
     */
    fun formatRatePair(
        fromCode: String,
        toCode: String,
        ratesPerUsd: Map<String, Double>
    ): String {
        val fromOpt = FormatUtils.findCurrency(fromCode)
        val toOpt = FormatUtils.findCurrency(toCode)
        if (fromOpt.code == toOpt.code) return "1 ${fromOpt.symbol}"
        val factor = conversionFactor(fromOpt.code, toOpt.code, ratesPerUsd)
        return if (factor >= 1.0) {
            "1 ${fromOpt.symbol} ≈ ${formatRateNumber(factor)} ${toOpt.symbol}"
        } else {
            val inverse = 1.0 / factor
            "1 ${toOpt.symbol} ≈ ${formatRateNumber(inverse)} ${fromOpt.symbol}"
        }
    }

    private fun formatRateNumber(value: Double): String {
        return when {
            value >= 100.0 -> String.format(Locale.US, "%,.0f", value)
            abs(value - value.toLong()) < 0.005 -> String.format(Locale.US, "%,.0f", value)
            else -> String.format(Locale.US, "%,.2f", value)
        }
    }
}
