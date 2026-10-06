package com.qoody.shared.capture.classifier

import com.qoody.shared.domain.model.Category
import com.qoody.shared.domain.model.Permille
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/** A category the on-device model is confident enough about. */
data class MerchantGuess(
    val category: Category,
    val confidence: Permille,
    /** Stored with the entry so a later model can be told apart. */
    val modelName: String,
)

/** Guesses a merchant's category on the device. Used only after remembered choices and keyword rules. */
interface MerchantClassifier {
    /** The guess for [merchant], or `null` when the model has no category for it or is not confident enough. */
    suspend fun classify(merchant: String): MerchantGuess?

    /** Never guesses: the model is missing or failed to load, so only rules categorise. */
    object None : MerchantClassifier {
        override suspend fun classify(merchant: String): MerchantGuess? = null
    }
}

/** Runs a [QmcModel]; its "Other" class (people, generic businesses) never produces a guess. */
class QmcMerchantClassifier(
    private val model: QmcModel,
) : MerchantClassifier {
    private val categories: List<Category?> =
        model.classes.map { name -> Category.builtIns.firstOrNull { it.key == name && it != Category.Uncategorized } }

    override suspend fun classify(merchant: String): MerchantGuess? = guess(merchant)

    fun guess(merchant: String): MerchantGuess? {
        val probabilities = model.probabilities(merchant) ?: return null
        val best = probabilities.indices.maxBy { probabilities[it] }
        val confidence = probabilities[best]
        return categories[best]
            ?.takeIf { confidence >= model.threshold }
            ?.let { MerchantGuess(it, Permille((confidence * PERMILLE_SCALE).roundToInt()), model.label) }
    }

    private companion object {
        const val PERMILLE_SCALE = 1_000f
    }
}

/**
 * Reads and parses the model on first use, off the caller's thread. A missing or malformed model
 * leaves the app categorising with rules only, as before the model existed.
 */
class LazyMerchantClassifier(
    private val load: () -> ByteArray,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : MerchantClassifier {
    private val delegate: Lazy<MerchantClassifier> =
        lazy {
            runCatching { QmcMerchantClassifier(QmcModel.parse(load())) }.getOrDefault(MerchantClassifier.None)
        }

    override suspend fun classify(merchant: String): MerchantGuess? =
        withContext(dispatcher) { delegate.value.classify(merchant) }
}
