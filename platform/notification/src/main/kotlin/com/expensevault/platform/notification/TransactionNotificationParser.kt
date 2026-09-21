package com.expensevault.platform.notification

import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale
import java.util.regex.Pattern
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

data class ParsedTransaction(
    val amount: BigDecimal,
    val merchant: String,
    val suggestedCategoryName: String,
    val accountSuffix: String?,
    val deduplicationKey: String,
    val rawText: String,
    val timestamp: Instant = Clock.System.now()
)

object TransactionNotificationParser {

    private const val CURRENCY_TOKENS = "(?:INR|Rs\\.?|₹|USD|\\$|EUR|€|GBP|£|CAD|AUD|SGD|AED|SAR|JPY|¥|CHF|NZD)"

    // Regex patterns for amounts supporting global currencies:
    // e.g. "debited by Rs 500", "spent $45.50", "paid €12.00", "charged £30", "500.00 INR debited"
    private val AMOUNT_PATTERNS = listOf(
        // "debited by/for/of Rs 500" or "paid $1,200.50" or "charged £45.00"
        Pattern.compile(
            "(?:debited|spent|paid|transferred|sent|purchase of|charged|withdrawn|deducted)(?:\\s+(?:by|for|of|with))?\\s*$CURRENCY_TOKENS?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)",
            Pattern.CASE_INSENSITIVE
        ),
        // "$500.00 debited/spent/paid/charged"
        Pattern.compile(
            "$CURRENCY_TOKENS\\s*([0-9,]+(?:\\.[0-9]{1,2})?)\\s*(?:debited|spent|paid|transferred|sent|deducted|withdrawn|charged)",
            Pattern.CASE_INSENSITIVE
        ),
        // "500.00 USD debited/spent/paid"
        Pattern.compile(
            "([0-9,]+(?:\\.[0-9]{1,2})?)\\s*$CURRENCY_TOKENS\\s*(?:debited|spent|paid|transferred|sent|deducted|withdrawn|charged)",
            Pattern.CASE_INSENSITIVE
        ),
        // "Payment of $500 to" or "Paid €250 to"
        Pattern.compile(
            "(?:payment of|payment to|paid|spent|purchase of)\\s+$CURRENCY_TOKENS?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)",
            Pattern.CASE_INSENSITIVE
        ),
        // Fallback: any "$500" or "Rs. 500" if word "debited/paid/spent" exists anywhere in text
        Pattern.compile(
            "$CURRENCY_TOKENS\\s*([0-9,]+(?:\\.[0-9]{1,2})?)",
            Pattern.CASE_INSENSITIVE
        ),
        // Fallback: "500.00 USD"
        Pattern.compile(
            "([0-9,]+(?:\\.[0-9]{1,2})?)\\s*$CURRENCY_TOKENS",
            Pattern.CASE_INSENSITIVE
        )
    )

    // Patterns to extract payee / merchant
    private val MERCHANT_PATTERNS = listOf(
        // "to VPA rahul@okaxis"
        Pattern.compile("to\\s+VPA\\s+([a-zA-Z0-9.\\-_@]+)", Pattern.CASE_INSENSITIVE),
        // "to Swiggy" or "paid to Ramesh Kumar using" or "paid to Uber India from"
        Pattern.compile("to\\s+([A-Za-z0-9\\s&'._-]+?)(?:\\s+(?:using|on|ref|from|via|towards|was|is|for|at|with|through|in)\\b|\\.|\\n|$)", Pattern.CASE_INSENSITIVE),
        // "at STARBUCKS COFFEE on"
        Pattern.compile("at\\s+([A-Za-z0-9\\s&'._-]+?)(?:\\s+(?:using|on|ref|from|via|towards|was|is|for|at|with|through|in)\\b|\\.|\\n|$)", Pattern.CASE_INSENSITIVE),
        // "towards swiggy@icici"
        Pattern.compile("towards\\s+([A-Za-z0-9\\s&'._-]+?)(?:\\s+(?:using|on|ref|from|via|towards|was|is|for|at|with|through|in)\\b|\\.|\\n|$)", Pattern.CASE_INSENSITIVE),
        // "Info: UPI/123456/Swiggy"
        Pattern.compile("Info:\\s*(?:UPI/[0-9]+/)?([A-Za-z0-9\\s&'._-]+?)(?:\\.|\\n|$)", Pattern.CASE_INSENSITIVE)
    )

    // Pattern to extract account last 4 digits: "A/c ending 1234", "Card XX4567", "A/C **9876", "A/c ...1234"
    private val ACCOUNT_PATTERNS = listOf(
        Pattern.compile("(?:A/c|Acct|Account|Card)\\s*(?:ending|no\\.?)?\\s*(?:in|with)?\\s*[*xX.]*([0-9]{3,4})", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?:ending\\s+in|ending\\s+with|ending)\\s+[*xX.]*([0-9]{3,4})", Pattern.CASE_INSENSITIVE),
        Pattern.compile("[*xX.]{2,}([0-9]{3,4})")
    )

    /**
     * Parses notification title and text.
     * Returns ParsedTransaction if text indicates a financial debit, or null if irrelevant/credit/OTP.
     */
    fun parse(title: String?, text: String?): ParsedTransaction? {
        val combined = "${title.orEmpty()} ${text.orEmpty()}".trim()
        if (combined.isBlank()) return null

        val lower = combined.lowercase(Locale.ROOT)

        // Ignore credits, refunds, OTPs, promotional alerts
        if (lower.contains("credited") || lower.contains("received from") || lower.contains("deposited") ||
            lower.contains("refund of") || lower.contains("cashback of") || lower.contains("otp is") ||
            lower.contains("verification code") || lower.contains("pre-approved") ||
            lower.contains("claim your") || lower.contains("congratulations")
        ) {
            return null
        }

        // Must contain at least one debit/spent/paid keyword
        val isDebit = lower.contains("debited") || lower.contains("spent") || 
                      lower.contains("paid") || lower.contains("transferred") ||
                      lower.contains("sent") || lower.contains("payment to") ||
                      lower.contains("money sent") || lower.contains("payment successful") ||
                      lower.contains("purchase") || lower.contains("charged") ||
                      lower.contains("deducted") || lower.contains("withdrawn") ||
                      lower.contains("txn")

        if (!isDebit) return null

        // 1. Extract Amount
        var amount: BigDecimal? = null
        for (pattern in AMOUNT_PATTERNS) {
            val matcher = pattern.matcher(combined)
            if (matcher.find()) {
                val rawAmountStr = matcher.group(1)?.replace(",", "")?.trim()
                try {
                    val parsed = BigDecimal(rawAmountStr).setScale(2, RoundingMode.HALF_UP)
                    if (parsed > BigDecimal.ZERO) {
                        amount = parsed
                        break
                    }
                } catch (_: Exception) {
                    // Try next pattern
                }
            }
        }

        if (amount == null) return null

        // 2. Extract Merchant / Payee
        var merchant = "Unknown Payee"
        for (pattern in MERCHANT_PATTERNS) {
            val matcher = pattern.matcher(combined)
            if (matcher.find()) {
                val extracted = matcher.group(1)?.trim()
                if (!extracted.isNullOrBlank() && extracted.length > 1 && !extracted.equals("Rs", ignoreCase = true)) {
                    merchant = cleanMerchantName(extracted)
                    break
                }
            }
        }

        // Fallback: If title is like "Paid to Zomato" or "Swiggy"
        if (merchant == "Unknown Payee" && !title.isNullOrBlank()) {
            val titleClean = title.trim()
            if (titleClean.startsWith("Paid to ", ignoreCase = true)) {
                merchant = cleanMerchantName(titleClean.substring(8).trim())
            } else if (!titleClean.contains("Alert", ignoreCase = true) && !titleClean.contains("Update", ignoreCase = true)) {
                merchant = cleanMerchantName(titleClean)
            }
        }

        // 3. Extract Account Suffix
        var accountSuffix: String? = null
        for (pattern in ACCOUNT_PATTERNS) {
            val matcher = pattern.matcher(combined)
            if (matcher.find()) {
                accountSuffix = matcher.group(1)?.trim()
                break
            }
        }

        // 4. Auto Categorization
        val suggestedCategory = categorizeMerchant(merchant, combined)

        // 5. Deduplication Key (e.g. noti_350.00_swiggy_4321)
        val cleanName = merchant.lowercase().replace(Regex("[^a-z0-9]"), "")
        val deduplicationKey = "noti_${amount.toPlainString()}_${cleanName}_${accountSuffix.orEmpty()}"

        return ParsedTransaction(
            amount = amount,
            merchant = merchant,
            suggestedCategoryName = suggestedCategory,
            accountSuffix = accountSuffix,
            deduplicationKey = deduplicationKey,
            rawText = combined
        )
    }

    private fun cleanMerchantName(raw: String): String {
        return raw.split(Regex("(?i)\\s+(using|on|ref|from|via|towards|was|is)\\s+"))[0]
            .replace(Regex("(?i)\\b(ltd|pvt|limited|private|upi|vpa|inc|corp|corporation|india)\\b"), "")
            .replace(Regex("(?i)\\b(account|acct|a/c)\\b.*"), "")
            .trim(' ', '.', '-', '_', ',', ':')
            .ifBlank { "Merchant" }
    }

    private fun categorizeMerchant(merchant: String, fullText: String): String {
        val target = "${merchant.lowercase()} ${fullText.lowercase()}"
        return when {
            listOf(
                "swiggy", "zomato", "mcdonald", "starbucks", "dominos", "kfc", "burger", "chai", 
                "cafe", "restaurant", "dine", "bakery", "pizza", "subway", "blinkit", "zepto", 
                "instamart", "chipotle", "pret", "costa", "wendy", "taco bell", "dunkin"
            ).any { target.contains(it) } -> "Food"

            listOf(
                "dmart", "bigbasket", "groceries", "supermarket", "grocery", "reliance fresh", 
                "vegetables", "fruits", "milk", "walmart", "target", "tesco", "sainsbury", 
                "aldi", "lidl", "trader joe", "kroger", "whole foods", "carrefour"
            ).any { target.contains(it) } -> "Groceries"

            listOf(
                "uber", "ola", "rapido", "metro", "irctc", "petrol", "fuel", "indian oil", 
                "bpcl", "hpcl", "shell", "fastag", "parking", "lyft", "transit", "train", 
                "exxon", "chevron", "gas"
            ).any { target.contains(it) } -> "Transport"

            listOf(
                "amazon", "flipkart", "myntra", "ajio", "zara", "h&m", "decathlon", "croma", 
                "reliance digital", "nykaa", "uniqlo", "ebay", "apple", "nike", "adidas", 
                "best buy", "sephora", "asos"
            ).any { target.contains(it) } -> "Shopping"

            listOf(
                "bescom", "electricity", "jio", "airtel", "vodafone", "vi", "billdesk", 
                "recharge", "water", "gas", "broadband", "tata play", "rent", "verizon", 
                "at&t", "t-mobile", "ee", "o2", "utilities", "internet"
            ).any { target.contains(it) } -> "Bills"

            listOf(
                "netflix", "spotify", "prime video", "pvr", "inox", "bookmyshow", "hotstar", 
                "youtube", "steam", "playstation", "cinema", "movie", "disney", "hulu", 
                "hbo", "amc", "odeon", "cineworld", "xbox", "nintendo"
            ).any { target.contains(it) } -> "Entertainment"

            listOf(
                "apollo", "pharma", "1mg", "pharmacy", "netmeds", "clinic", "hospital", 
                "doctor", "dental", "medical", "cvs", "walgreens", "boots"
            ).any { target.contains(it) } -> "Health"

            else -> "General"
        }
    }
}
