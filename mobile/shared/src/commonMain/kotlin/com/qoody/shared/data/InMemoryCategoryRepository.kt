package com.qoody.shared.data

import com.qoody.shared.domain.model.CustomCategory
import com.qoody.shared.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/** [CategoryRepository] kept in memory, for tests and screenshot fixtures. */
class InMemoryCategoryRepository(
    seed: List<CustomCategory> = emptyList(),
) : CategoryRepository {
    private val state = MutableStateFlow(seed)

    override val custom: Flow<List<CustomCategory>> = state

    override suspend fun create(
        name: String,
        emoji: String,
    ): CustomCategory {
        val created = CustomCategory(id = (state.value.maxOfOrNull { it.id } ?: 0L) + 1, name = name, emoji = emoji)
        state.update { it + created }
        return created
    }

    override suspend fun update(category: CustomCategory) =
        state.update { list -> list.map { if (it.id == category.id) category else it } }

    override suspend fun delete(id: Long) = state.update { list -> list.filterNot { it.id == id } }
}
