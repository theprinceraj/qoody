package com.qoody.shared.capture.classifier

import com.qoody.shared.domain.model.Category
import com.qoody.shared.domain.model.Permille
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class QmcFeaturesTest {
    @Test
    fun normaliseKeepsAsciiLettersAndDigitsOnly() {
        assertEquals("upi swiggy0 1 ybl", QmcFeatures.normalise("  UPI/SWIGGY0-1@ybl "))
        assertEquals("caf", QmcFeatures.normalise("Café"))
        assertEquals("", QmcFeatures.normalise("मिठाई"))
    }

    @Test
    fun featuresAreNgramsWordsAndPairsWithDigitsFolded() {
        assertEquals(
            listOf("<o", "ol", "la", "a>", "<ol", "ola", "la>", "w ola", "<0", "0>", "<0>", "w 0", "b ola 0"),
            QmcFeatures.features("Ola 7", minN = 2, maxN = 3),
        )
        assertEquals(emptyList(), QmcFeatures.features(" @@ ", minN = 2, maxN = 3))
    }

    @Test
    fun fnv1aMatchesTheReferenceVectors() {
        assertEquals(0x811C9DC5u, QmcFeatures.fnv1a(""))
        assertEquals(0xE40C292Cu, QmcFeatures.fnv1a("a"))
        assertEquals(0xBF9CF968u, QmcFeatures.fnv1a("foobar"))
    }
}

class QmcModelTest {
    @Test
    fun parsesAFileAndComputesSoftmaxFromTheBias() {
        val model = QmcModel.parse(qmcFile(bias = floatArrayOf(2f, 0f)))

        val probabilities = assertNotNull(model.probabilities("Anything"))
        assertEquals("test model", model.label)
        assertEquals(listOf("FoodAndDrink", "Other"), model.classes)
        assertEquals(0.8808f, probabilities[0], absoluteTolerance = 1e-4f)
        assertEquals(1f, probabilities.sum(), absoluteTolerance = 1e-6f)
    }

    @Test
    fun textWithoutFeaturesHasNoProbabilities() {
        assertNull(QmcModel.parse(qmcFile()).probabilities("  —  "))
    }

    @Test
    fun embeddingsMoveTheGuess() {
        // Every bucket pushes towards class 1, outweighing the bias towards class 0.
        val model =
            QmcModel.parse(
                qmcFile(bias = floatArrayOf(1f, 0f), embedding = 100, weights = floatArrayOf(-1f, 1f, -1f, 1f)),
            )

        val probabilities = assertNotNull(model.probabilities("Ola"))
        assertTrue(probabilities[1] > probabilities[0])
    }

    @Test
    fun rejectsMalformedFiles() {
        val valid = qmcFile()
        assertFailsWith<IllegalArgumentException> {
            QmcModel.parse("QMC9".encodeToByteArray() + valid.copyOfRange(4, valid.size))
        }
        assertFailsWith<IllegalArgumentException> { QmcModel.parse(valid.copyOf(valid.size - 1)) }
        assertFailsWith<IllegalArgumentException> { QmcModel.parse(valid + 0.toByte()) }
        assertFailsWith<IllegalArgumentException> { QmcModel.parse(valid.copyOf(10)) }
        assertFailsWith<IllegalArgumentException> { QmcModel.parse(ByteArray(0)) }
        val wrongVersion = valid.copyOf().also { it[4] = 2 }
        assertFailsWith<IllegalArgumentException> { QmcModel.parse(wrongVersion) }
    }
}

class QmcMerchantClassifierTest {
    @Test
    fun guessesTheBestCategoryAboveTheThreshold() =
        runTest {
            val classifier =
                QmcMerchantClassifier(QmcModel.parse(qmcFile(bias = floatArrayOf(3f, 0f), threshold = 0.9f)))

            assertEquals(MerchantGuess(Category.FoodAndDrink, Permille(953), "test model"), classifier.classify("Cafe"))
        }

    @Test
    fun noGuessBelowTheThresholdForOtherOrWithoutFeatures() =
        runTest {
            val unsure = QmcMerchantClassifier(QmcModel.parse(qmcFile(bias = floatArrayOf(3f, 0f), threshold = 0.96f)))
            val other = QmcMerchantClassifier(QmcModel.parse(qmcFile(bias = floatArrayOf(0f, 3f))))

            assertNull(unsure.classify("Cafe"))
            assertNull(other.classify("Rahul Sharma"))
            assertNull(other.classify(""))
        }

    @Test
    fun lazyClassifierFallsBackToNoGuessWhenTheModelCannotLoad() =
        runTest {
            val broken = LazyMerchantClassifier({ ByteArray(3) })
            val missing = LazyMerchantClassifier({ error("no asset") })
            val working = LazyMerchantClassifier({ qmcFile(bias = floatArrayOf(3f, 0f)) })

            assertNull(broken.classify("Cafe"))
            assertNull(missing.classify("Cafe"))
            assertEquals(Category.FoodAndDrink, working.classify("Cafe")?.category)
        }
}

/** A QMC1 file with two classes, 8 buckets, dim 2, every embedding set to [embedding] (int8). */
internal fun qmcFile(
    bias: FloatArray = floatArrayOf(0f, 0f),
    threshold: Float = 0.5f,
    embedding: Int = 0,
    weights: FloatArray = FloatArray(4),
): ByteArray {
    val out = mutableListOf<Byte>()

    fun u8(value: Int) = out.add(value.toByte())

    fun u16(value: Int) = repeat(2) { u8(value shr (8 * it)) }

    fun i32(value: Int) = repeat(4) { u8(value shr (8 * it)) }

    fun ascii(text: String) {
        u8(text.length)
        text.forEach { u8(it.code) }
    }
    "QMC1".forEach { u8(it.code) }
    u16(1)
    ascii("test model")
    u8(2)
    u8(3)
    i32(8)
    u16(2)
    u8(2)
    ascii("FoodAndDrink")
    ascii("Other")
    i32(threshold.toBits())
    i32(0.01f.toBits())
    repeat(8 * 2) { u8(embedding) }
    weights.forEach { i32(it.toBits()) }
    bias.forEach { i32(it.toBits()) }
    return out.toByteArray()
}
