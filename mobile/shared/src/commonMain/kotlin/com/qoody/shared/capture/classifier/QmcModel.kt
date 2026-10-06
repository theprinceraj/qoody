package com.qoody.shared.capture.classifier

import kotlin.math.exp

/**
 * The merchant classifier's weights, read from the QMC1 file written by
 * `tools/merchant-classifier/qmc.py` (layout documented in `qmc.write`).
 */
class QmcModel private constructor(
    /** Stored with each guess as `Categorization.Model.modelName`. */
    val label: String,
    /** Output classes in order: [com.qoody.shared.domain.model.Category] names, plus "Other". */
    val classes: List<String>,
    private val minN: Int,
    private val maxN: Int,
    private val buckets: Int,
    private val dim: Int,
    /** Guesses below this confidence are dropped. */
    val threshold: Float,
    private val scale: Float,
    private val embeddings: ByteArray,
    private val weights: FloatArray,
    private val bias: FloatArray,
) {
    /** Softmax over [classes], or `null` when [text] has no features (blank or no Latin letters or digits). */
    fun probabilities(text: String): FloatArray? {
        val ids = QmcFeatures.bucketIds(text, minN, maxN, buckets)
        if (ids.isEmpty()) return null
        val hidden = FloatArray(dim)
        for (id in ids) {
            val row = id * dim
            for (d in 0 until dim) hidden[d] += embeddings[row + d] * scale
        }
        for (d in 0 until dim) hidden[d] /= ids.size
        val logits = bias.copyOf()
        for (d in 0 until dim) {
            for (c in classes.indices) logits[c] += hidden[d] * weights[d * classes.size + c]
        }
        val peak = logits.max()
        val exps = FloatArray(logits.size) { exp(logits[it] - peak) }
        val total = exps.sum()
        return FloatArray(exps.size) { exps[it] / total }
    }

    companion object {
        private val MAGIC = "QMC1".encodeToByteArray()
        private const val FORMAT_VERSION = 1
        private const val FLOAT_BYTES = 4

        /** Parses a QMC1 file; throws [IllegalArgumentException] when it is malformed or of another version. */
        fun parse(bytes: ByteArray): QmcModel {
            val reader = LittleEndianReader(bytes)
            require(reader.bytes(MAGIC.size).contentEquals(MAGIC)) { "Not a QMC file" }
            require(reader.u16() == FORMAT_VERSION) { "Unsupported QMC version" }
            val label = reader.ascii(reader.u8())
            val minN = reader.u8()
            val maxN = reader.u8()
            val buckets = reader.u32()
            val dim = reader.u16()
            val classes = List(reader.u8()) { reader.ascii(reader.u8()) }
            require(minN in 1..maxN && buckets > 0 && dim > 0 && classes.isNotEmpty()) { "Invalid QMC header" }
            val threshold = reader.f32()
            val scale = reader.f32()
            val expectedRest = buckets.toLong() * dim + (dim.toLong() * classes.size + classes.size) * FLOAT_BYTES
            require(reader.remaining.toLong() == expectedRest) { "QMC size does not match its header" }
            val embeddings = reader.bytes(buckets * dim)
            val weights = FloatArray(dim * classes.size) { reader.f32() }
            val bias = FloatArray(classes.size) { reader.f32() }
            return QmcModel(label, classes, minN, maxN, buckets, dim, threshold, scale, embeddings, weights, bias)
        }
    }
}

private class LittleEndianReader(
    private val data: ByteArray,
) {
    private var position = 0

    val remaining: Int get() = data.size - position

    fun bytes(count: Int): ByteArray {
        require(count in 0..remaining) { "QMC file is truncated" }
        return data.copyOfRange(position, position + count).also { position += count }
    }

    fun u8(): Int = bytes(1)[0].toInt() and BYTE_MASK

    fun u16(): Int = u8() or (u8() shl BITS_PER_BYTE)

    /** Unsigned 32-bit; values above [Int.MAX_VALUE] are rejected, no real model is that large. */
    fun u32(): Int {
        val value = i32()
        require(value >= 0) { "QMC value out of range" }
        return value
    }

    fun f32(): Float = Float.fromBits(i32())

    fun ascii(length: Int): String = bytes(length).decodeToString()

    private fun i32(): Int = u16() or (u16() shl (2 * BITS_PER_BYTE))

    private companion object {
        const val BYTE_MASK = 0xFF
        const val BITS_PER_BYTE = 8
    }
}
