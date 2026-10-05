package com.qoody.shared.capture

import com.qoody.shared.domain.model.Category
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MerchantCategoryRulesTest {
    private fun category(merchant: String?) = MerchantCategoryRules.categorise(merchant)?.category

    @Test
    fun commonMerchantsMapToCategories() {
        assertEquals(Category.FoodAndDrink, category("Swiggy Instamart"))
        assertEquals(Category.FoodAndDrink, category("ZOMATO"))
        assertEquals(Category.Transport, category("Uber India"))
        assertEquals(Category.Transport, category("IRCTC"))
        assertEquals(Category.Shopping, category("Amazon Pay"))
        assertEquals(Category.Bills, category("Airtel Postpaid"))
        assertEquals(Category.Subscriptions, category("Netflix"))
    }

    @Test
    fun specificGroupsBeatGeneralOnes() {
        assertEquals(Category.Subscriptions, category("Amazon Prime"))
        assertEquals(Category.Subscriptions, category("JioCinema"))
    }

    @Test
    fun shortKeywordsNeedAWholeWord() {
        assertNull(category("Olay Store"))
        assertNull(category("Licious"))
        assertNull(category("Credence Traders"))
        assertEquals(Category.Transport, category("Ola Cabs"))
    }

    @Test
    fun unknownOrMissingMerchantHasNoMatch() {
        assertNull(category("Ravi Tailors"))
        assertNull(category(null))
        assertNull(category("  "))
    }

    @Test
    fun matchReportsTheRuleThatFired() {
        assertEquals("swiggy", MerchantCategoryRules.categorise("Swiggy")?.ruleId)
    }
}
