package com.qoody.shared.capture

import com.qoody.shared.capture.classifier.MerchantClassifier
import com.qoody.shared.capture.classifier.MerchantGuess
import com.qoody.shared.data.InMemoryMerchantCategoryRepository
import com.qoody.shared.domain.model.Categorization
import com.qoody.shared.domain.model.Category
import com.qoody.shared.domain.model.Permille
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/** Always returns [guess] and records what it was asked. */
internal class FixedClassifier(
    private val guess: MerchantGuess?,
) : MerchantClassifier {
    val asked = mutableListOf<String>()

    override suspend fun classify(merchant: String): MerchantGuess? {
        asked += merchant
        return guess
    }
}

class MerchantCategoriserTest {
    private val merchantCategories = InMemoryMerchantCategoryRepository()
    private val guess = MerchantGuess(Category.Shopping, Permille(880), "test model")
    private val classifier = FixedClassifier(guess)
    private val categoriser = MerchantCategoriser(merchantCategories, classifier)

    @Test
    fun rememberedChoiceComesFirstAndSkipsTheModel() =
        runTest {
            merchantCategories.remember("Kanti Sweets", Category.Bills)

            assertEquals(Category.Bills to Categorization.Remembered, categoriser.categorise("Kanti Sweets"))
            assertEquals(emptyList(), classifier.asked)
        }

    @Test
    fun keywordRuleComesBeforeTheModel() =
        runTest {
            assertEquals(Category.FoodAndDrink to Categorization.Rule("swiggy"), categoriser.categorise("Swiggy"))
            assertEquals(emptyList(), classifier.asked)
        }

    @Test
    fun theModelFillsTheGap() =
        runTest {
            assertEquals(
                Category.Shopping to Categorization.Model("test model", Permille(880)),
                categoriser.categorise("Poorvika Mobiles"),
            )
            assertEquals(listOf("Poorvika Mobiles"), classifier.asked)
        }

    @Test
    fun noGuessOrNoMerchantStaysUncategorized() =
        runTest {
            val unsure = MerchantCategoriser(merchantCategories, FixedClassifier(null))

            assertEquals(Category.Uncategorized to Categorization.None, unsure.categorise("Rahul Verma"))
            assertEquals(Category.Uncategorized to Categorization.None, categoriser.categorise(null))
        }
}
