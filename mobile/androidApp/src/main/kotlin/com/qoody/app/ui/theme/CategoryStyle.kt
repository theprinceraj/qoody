package com.qoody.app.ui.theme

import androidx.annotation.StringRes
import com.qoody.app.R
import com.qoody.shared.domain.model.Category

/** The full, human-readable name of the category. */
@get:StringRes
val Category.nameRes: Int
    get() =
        when (this) {
            Category.FoodAndDrink -> R.string.category_food_and_drink
            Category.Transport -> R.string.category_transport
            Category.Shopping -> R.string.category_shopping
            Category.Bills -> R.string.category_bills
            Category.Friends -> R.string.category_friends
            Category.Subscriptions -> R.string.category_subscriptions
            Category.Uncategorized -> R.string.category_uncategorized
        }

/** The short lowercase label used on filter chips. */
@get:StringRes
val Category.chipLabelRes: Int
    get() =
        when (this) {
            Category.FoodAndDrink -> R.string.category_chip_food_and_drink
            Category.Transport -> R.string.category_chip_transport
            Category.Shopping -> R.string.category_chip_shopping
            Category.Bills -> R.string.category_chip_bills
            Category.Friends -> R.string.category_chip_friends
            Category.Subscriptions -> R.string.category_chip_subscriptions
            Category.Uncategorized -> R.string.category_chip_uncategorized
        }

@get:StringRes
val Category.emojiRes: Int
    get() =
        when (this) {
            Category.FoodAndDrink -> R.string.category_emoji_food_and_drink
            Category.Transport -> R.string.category_emoji_transport
            Category.Shopping -> R.string.category_emoji_shopping
            Category.Bills -> R.string.category_emoji_bills
            Category.Friends -> R.string.category_emoji_friends
            Category.Subscriptions -> R.string.category_emoji_subscriptions
            Category.Uncategorized -> R.string.category_emoji_uncategorized
        }
