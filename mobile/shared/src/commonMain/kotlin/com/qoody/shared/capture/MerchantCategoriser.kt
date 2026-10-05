package com.qoody.shared.capture

import com.qoody.shared.domain.model.Categorization
import com.qoody.shared.domain.model.Category
import com.qoody.shared.domain.repository.MerchantCategoryRepository

/** Picks a category for a merchant: the user's earlier choice first, then the keyword rules. */
class MerchantCategoriser(
    private val merchantCategories: MerchantCategoryRepository,
) {
    suspend fun categorise(merchant: String?): Pair<Category, Categorization> {
        val remembered = merchant?.let { merchantCategories.categoryFor(it) }
        val rule = if (remembered == null) MerchantCategoryRules.categorise(merchant) else null
        return when {
            remembered != null -> remembered to Categorization.Remembered
            rule != null -> rule.category to Categorization.Rule(rule.ruleId)
            else -> Category.Uncategorized to Categorization.None
        }
    }
}
