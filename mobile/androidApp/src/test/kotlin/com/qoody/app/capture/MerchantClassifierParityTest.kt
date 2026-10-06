package com.qoody.app.capture

import com.qoody.shared.capture.classifier.QmcMerchantClassifier
import com.qoody.shared.capture.classifier.QmcModel
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.float
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.time.measureTime

/**
 * The bundled model, run by the Kotlin port, must give the probabilities the Python trainer
 * recorded (tools/merchant-classifier writes the reference file next to the model).
 */
class MerchantClassifierParityTest {
    private val model = QmcModel.parse(File("src/main/assets/merchant_classifier.bin").readBytes())
    private val reference =
        Json
            .parseToJsonElement(javaClass.getResource("/merchant_classifier_reference.json")!!.readText())
            .jsonObject

    @Test
    fun headerMatchesTheReference() {
        assertEquals(reference.getValue("label").jsonPrimitive.content, model.label)
        assertEquals(reference.getValue("classes").jsonArray.map { it.jsonPrimitive.content }, model.classes)
        assertEquals(reference.getValue("threshold").jsonPrimitive.float, model.threshold, 0f)
    }

    @Test
    fun probabilitiesMatchPython() {
        val cases = reference.getValue("cases").jsonArray
        assertTrue(cases.size > 100)
        for (case in cases) {
            val text =
                case.jsonObject
                    .getValue("text")
                    .jsonPrimitive.content
            val expected = case.jsonObject.getValue("probabilities")
            val actual = model.probabilities(text)
            if (expected is JsonNull) {
                assertNull(text, actual)
            } else {
                val probabilities = (expected as JsonArray).map { it.jsonPrimitive.float }
                for ((index, value) in probabilities.withIndex()) {
                    assertEquals(text, value, actual!![index], TOLERANCE)
                }
            }
        }
    }

    @Test
    fun classifiesQuickly() {
        val classifier = QmcMerchantClassifier(model)
        val texts =
            reference.getValue("cases").jsonArray.map {
                it.jsonObject
                    .getValue("text")
                    .jsonPrimitive.content
            }
        classifier.guess("warm up")
        val elapsed = measureTime { repeat(ROUNDS) { texts.forEach(classifier::guess) } }
        val perGuessMicros = elapsed.inWholeMicroseconds / (ROUNDS * texts.size)
        assertTrue("$perGuessMicros µs per guess", perGuessMicros < MAX_MICROS_PER_GUESS)
    }

    private companion object {
        const val TOLERANCE = 1e-4f
        const val ROUNDS = 10
        const val MAX_MICROS_PER_GUESS = 2_000
    }
}
