package com.qoody.shared.capture.classifier

/**
 * Text to hashed feature ids, exactly as `tools/merchant-classifier/qmc.py` does it. The model was
 * trained on these ids, so any change here must be made there too (and the model retrained).
 */
internal object QmcFeatures {
    private const val FNV_OFFSET = 0x811C9DC5u
    private const val FNV_PRIME = 0x01000193u
    private const val WORD_PREFIX = "w "
    private const val PAIR_PREFIX = "b "

    /** ASCII letters lower-cased, digits kept, everything else a separator; separators collapse. */
    fun normalise(text: String): String {
        val out = StringBuilder(text.length)
        for (ch in text) {
            when (ch) {
                in 'A'..'Z' -> out.append(ch.lowercaseChar())
                in 'a'..'z', in '0'..'9' -> out.append(ch)
                else -> out.append(' ')
            }
        }
        return out.split(' ').filter { it.isNotEmpty() }.joinToString(" ")
    }

    /** Character n-grams of each `<word>`, the word itself, and adjacent word pairs; digits fold to `0`. */
    fun features(
        text: String,
        minN: Int,
        maxN: Int,
    ): List<String> {
        val words = foldDigits(normalise(text)).split(' ').filter { it.isNotEmpty() }
        val out = ArrayList<String>()
        for (word in words) {
            val padded = "<$word>"
            for (n in minN..maxN) {
                for (start in 0..padded.length - n) out.add(padded.substring(start, start + n))
            }
            out.add(WORD_PREFIX + word)
        }
        for (index in 0 until words.size - 1) out.add(PAIR_PREFIX + words[index] + " " + words[index + 1])
        return out
    }

    /** 32-bit FNV-1a; features are ASCII after [normalise], so a char is one byte. */
    fun fnv1a(feature: String): UInt {
        var hash = FNV_OFFSET
        for (ch in feature) {
            hash = (hash xor ch.code.toUInt()) * FNV_PRIME
        }
        return hash
    }

    fun bucketIds(
        text: String,
        minN: Int,
        maxN: Int,
        buckets: Int,
    ): IntArray = features(text, minN, maxN).map { (fnv1a(it) % buckets.toUInt()).toInt() }.toIntArray()

    /** Every digit becomes `0`, so reference numbers and UPI id tails share features. */
    private fun foldDigits(text: String): String =
        buildString(text.length) {
            for (ch in text) append(if (ch.isAsciiDigit()) '0' else ch)
        }

    private fun Char.isAsciiDigit(): Boolean = this in '0'..'9'
}
