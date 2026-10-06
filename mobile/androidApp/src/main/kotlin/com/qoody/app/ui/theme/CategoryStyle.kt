package com.qoody.app.ui.theme

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.res.stringResource
import com.qoody.app.R
import com.qoody.shared.domain.model.Category
import com.qoody.shared.domain.model.CustomCategory

/** The user's own categories, provided once at the app root so every screen can name them. */
val LocalCustomCategories = compositionLocalOf { emptyList<CustomCategory>() }

/** Every category to offer in pickers and filters, built-ins first and Uncategorized last. */
@Composable
@ReadOnlyComposable
fun allCategories(): List<Category> = Category.all(LocalCustomCategories.current)

/** The full, human-readable name of [category]. */
@Composable
@ReadOnlyComposable
fun categoryName(category: Category): String = category.custom()?.name ?: stringResource(builtIn(category).name)

/** The short lowercase label used on filter chips. */
@Composable
@ReadOnlyComposable
fun categoryChipLabel(category: Category): String =
    category.custom()?.name?.lowercase() ?: stringResource(builtIn(category).chipLabel)

@Composable
@ReadOnlyComposable
fun categoryEmoji(category: Category): String = category.custom()?.emoji ?: stringResource(builtIn(category).emoji)

@Composable
@ReadOnlyComposable
private fun Category.custom(): CustomCategory? = LocalCustomCategories.current.firstOrNull { it.category == this }

private class BuiltInStrings(
    @StringRes val name: Int,
    @StringRes val chipLabel: Int,
    @StringRes val emoji: Int,
)

/** A custom category that is not (or no longer) defined reads as Uncategorized. */
private fun builtIn(category: Category): BuiltInStrings =
    when (category) {
        Category.FoodAndDrink -> {
            BuiltInStrings(
                R.string.category_food_and_drink,
                R.string.category_chip_food_and_drink,
                R.string.category_emoji_food_and_drink,
            )
        }

        Category.Transport -> {
            BuiltInStrings(
                R.string.category_transport,
                R.string.category_chip_transport,
                R.string.category_emoji_transport,
            )
        }

        Category.Shopping -> {
            BuiltInStrings(
                R.string.category_shopping,
                R.string.category_chip_shopping,
                R.string.category_emoji_shopping,
            )
        }

        Category.Bills -> {
            BuiltInStrings(R.string.category_bills, R.string.category_chip_bills, R.string.category_emoji_bills)
        }

        Category.Friends -> {
            BuiltInStrings(R.string.category_friends, R.string.category_chip_friends, R.string.category_emoji_friends)
        }

        Category.Subscriptions -> {
            BuiltInStrings(
                R.string.category_subscriptions,
                R.string.category_chip_subscriptions,
                R.string.category_emoji_subscriptions,
            )
        }

        else -> {
            BuiltInStrings(
                R.string.category_uncategorized,
                R.string.category_chip_uncategorized,
                R.string.category_emoji_uncategorized,
            )
        }
    }
