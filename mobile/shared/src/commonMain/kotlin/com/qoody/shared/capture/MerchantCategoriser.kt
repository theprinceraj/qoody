package com.qoody.shared.capture

import com.qoody.shared.capture.classifier.MerchantClassifier
import com.qoody.shared.domain.model.Categorization
import com.qoody.shared.domain.model.Category
import com.qoody.shared.domain.repository.MerchantCategoryRepository

/**
 * Picks a category for a merchant: the user's earlier choice first, then the keyword rules, then
 * the on-device model when it is confident enough.
 */
class MerchantCategoriser(
    private val merchantCategories: MerchantCategoryRepository,
    private val classifier: MerchantClassifier = MerchantClassifier.None,
) {
    suspend fun categorise(merchant: String?): Pair<Category, Categorization> {
        val remembered = merchant?.let { merchantCategories.categoryFor(it) }
        val rule = if (remembered == null) MerchantCategoryRules.categorise(merchant) else null
        val guess = if (remembered == null && rule == null) merchant?.let { classifier.classify(it) } else null
        return when {
            remembered != null -> remembered to Categorization.Remembered
            rule != null -> rule.category to Categorization.Rule(rule.ruleId)
            guess != null -> guess.category to Categorization.Model(guess.modelName, guess.confidence)
            else -> Category.Uncategorized to Categorization.None
        }
    }
}
