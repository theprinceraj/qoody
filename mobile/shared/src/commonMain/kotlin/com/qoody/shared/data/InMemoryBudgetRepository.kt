package com.qoody.shared.data

import com.qoody.shared.domain.model.Category
import com.qoody.shared.domain.model.Money
import com.qoody.shared.domain.repository.BudgetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/** [BudgetRepository] kept in memory, for tests and screenshot fixtures. */
class InMemoryBudgetRepository(
    seed: Map<Category, Money> = emptyMap(),
) : BudgetRepository {
    private val stored = MutableStateFlow(seed)

    override val budgets: Flow<Map<Category, Money>> = stored

    override suspend fun setBudget(
        category: Category,
        limit: Money?,
    ) = stored.update { if (limit == null) it - category else it + (category to limit) }
}
