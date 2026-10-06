package com.qoody.shared.domain.model

import kotlin.jvm.JvmInline

/**
 * A spending category: one of the built-ins below, or one the user created (D25). [key] is what is
 * stored; built-in keys are the names the old enum persisted, so existing data reads unchanged.
 */
@JvmInline
value class Category private constructor(
    val key: String,
) {
    val isCustom: Boolean get() = key.startsWith(CUSTOM_PREFIX)

    /** The [CustomCategory.id] of a user-made category, `null` for built-ins. */
    val customId: Long? get() = if (isCustom) key.removePrefix(CUSTOM_PREFIX).toLongOrNull() else null

    override fun toString(): String = key

    companion object {
        private const val CUSTOM_PREFIX = "custom:"

        val FoodAndDrink = Category("FoodAndDrink")
        val Transport = Category("Transport")
        val Shopping = Category("Shopping")
        val Bills = Category("Bills")
        val Friends = Category("Friends")
        val Subscriptions = Category("Subscriptions")
        val Uncategorized = Category("Uncategorized")

        /** Built-ins in the order shown in chips and pickers. */
        val builtIns: List<Category> =
            listOf(FoodAndDrink, Transport, Shopping, Bills, Friends, Subscriptions, Uncategorized)

        fun custom(id: Long): Category = Category(CUSTOM_PREFIX + id)

        /** The category a stored key names, or `null` when the key is not a category. */
        fun fromKey(key: String): Category? =
            builtIns.firstOrNull { it.key == key }
                ?: key
                    .takeIf { it.startsWith(CUSTOM_PREFIX) }
                    ?.removePrefix(CUSTOM_PREFIX)
                    ?.toLongOrNull()
                    ?.let(::custom)

        /** Every category to offer: built-ins, then the user's own, with Uncategorized last. */
        fun all(custom: List<CustomCategory>): List<Category> =
            builtIns.filter { it != Uncategorized } + custom.map { it.category } + Uncategorized
    }
}

/** A category the user made: a name and an emoji. Its colour comes from the theme palette. */
data class CustomCategory(
    val id: Long,
    val name: String,
    val emoji: String,
) {
    val category: Category get() = Category.custom(id)

    companion object {
        /** Longest name accepted, so it still fits a chip. */
        const val NAME_MAX_LENGTH = 24
    }
}
