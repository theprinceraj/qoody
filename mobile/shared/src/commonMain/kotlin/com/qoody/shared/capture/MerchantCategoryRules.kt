package com.qoody.shared.capture

import com.qoody.shared.domain.model.Category

/** The rule that picked a category, kept so the UI can say where the guess came from. */
data class RuleMatch(
    val category: Category,
    /** The keyword that matched, which doubles as a stable rule id. */
    val ruleId: String,
)

/**
 * Keyword table mapping a merchant name to a [Category]. First match wins, so groups are ordered
 * from most to least specific (e.g. "amazon prime" must reach Subscriptions before "amazon" reaches
 * Shopping). A keyword matches at the start of a word, so "swiggy" matches "Swiggy Instamart".
 */
object MerchantCategoryRules {
    /** Keywords this short must match a whole word, so "ola" does not match "Olay" or "lic" "Licious". */
    private const val WHOLE_WORD_MAX_LENGTH = 4

    private val groups: List<Pair<Category, List<String>>> =
        listOf(
            Category.Subscriptions to
                listOf(
                    "netflix",
                    "spotify",
                    "hotstar",
                    "jiocinema",
                    "prime video",
                    "amazon prime",
                    "youtube",
                    "google one",
                    "icloud",
                    "apple media",
                    "sonyliv",
                    "zee5",
                    "audible",
                    "chatgpt",
                    "openai",
                    "canva",
                    "gaana",
                    "wynk",
                ),
            Category.Bills to
                listOf(
                    "electricity",
                    "bescom",
                    "mseb",
                    "tata power",
                    "adani electricity",
                    "bses",
                    "airtel",
                    "jio",
                    "vodafone",
                    "bsnl",
                    "recharge",
                    "broadband",
                    "fibernet",
                    "postpaid",
                    "indane",
                    "water board",
                    "rent",
                    "nobroker",
                    "insurance",
                    "lic",
                    "cred",
                    "dth",
                    "tata play",
                    "municipal",
                    "property tax",
                    "emi",
                    "loan",
                    "bill",
                ),
            Category.Transport to
                listOf(
                    "uber",
                    "ola",
                    "rapido",
                    "irctc",
                    "redbus",
                    "metro",
                    "fastag",
                    "indigo",
                    "air india",
                    "akasa",
                    "makemytrip",
                    "cleartrip",
                    "ixigo",
                    "abhibus",
                    "petrol",
                    "fuel",
                    "indian oil",
                    "bharat petroleum",
                    "hp pay",
                    "shell",
                    "parking",
                    "namma yatri",
                    "blusmart",
                    "dmrc",
                ),
            Category.FoodAndDrink to
                listOf(
                    "swiggy",
                    "zomato",
                    "domino",
                    "mcdonald",
                    "kfc",
                    "starbucks",
                    "cafe",
                    "restaurant",
                    "pizza",
                    "burger",
                    "bakery",
                    "barista",
                    "chai",
                    "blinkit",
                    "zepto",
                    "bigbasket",
                    "instamart",
                    "eatsure",
                    "faasos",
                    "haldiram",
                    "subway",
                    "dunzo",
                    "food",
                ),
            Category.Shopping to
                listOf(
                    "amazon",
                    "flipkart",
                    "myntra",
                    "ajio",
                    "meesho",
                    "nykaa",
                    "croma",
                    "reliance digital",
                    "decathlon",
                    "dmart",
                    "lenskart",
                    "tata cliq",
                    "ikea",
                ),
        )

    private val compiled: List<Triple<Category, String, Regex>> =
        groups.flatMap { (category, keywords) ->
            keywords.map { keyword ->
                val tail = if (keyword.length <= WHOLE_WORD_MAX_LENGTH) "\\b" else ""
                Triple(category, keyword, Regex("\\b${Regex.escape(keyword)}$tail", RegexOption.IGNORE_CASE))
            }
        }

    /** The matching rule, or `null` when no keyword matches (the caller keeps `Uncategorized`). */
    fun categorise(merchant: String?): RuleMatch? {
        if (merchant.isNullOrBlank()) return null
        return compiled
            .firstOrNull { (_, _, pattern) -> pattern.containsMatchIn(merchant) }
            ?.let { (category, keyword, _) -> RuleMatch(category, keyword) }
    }
}
