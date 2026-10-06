package com.qoody.shared.data

import com.qoody.shared.domain.model.Category
import com.qoody.shared.domain.model.MerchantKey
import com.qoody.shared.domain.repository.MerchantCategoryRepository

/** [MerchantCategoryRepository] kept in memory, for tests and screenshot fixtures. */
class InMemoryMerchantCategoryRepository : MerchantCategoryRepository {
    private val categories = mutableMapOf<String, Category>()

    override suspend fun categoryFor(merchant: String): Category? = categories[MerchantKey.of(merchant)]

    override suspend fun remember(
        merchant: String,
        category: Category,
    ) {
        val key = MerchantKey.of(merchant)
        if (key.isNotEmpty()) categories[key] = category
    }

    override suspend fun forget(category: Category) {
        categories.values.removeAll { it == category }
    }
}
