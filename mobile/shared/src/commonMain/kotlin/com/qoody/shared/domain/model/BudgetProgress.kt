package com.qoody.shared.domain.model

/** How spending this month compares with a monthly limit. */
data class BudgetProgress(
    val spent: Money,
    /** `null` when there is no budget. */
    val limit: Money?,
) {
    /** Share of the limit used; above [Permille.Full] when over budget. `null` without a limit. */
    val used: Permille? get() = limit?.let { Permille.of(spent, it) }

    val isOver: Boolean get() = limit != null && spent > limit
}
