package dev.chungjungsoo.gptmobile.util

/**
 * A small provider-independent fallback for APIs that do not return usage metadata.
 * It intentionally reports an estimate rather than pretending to be model-exact.
 */
internal fun estimateTokenCount(text: String): Int {
    var index = 0
    var tokenCount = 0

    while (index < text.length) {
        val codePoint = text.codePointAt(index)
        val codePointLength = Character.charCount(codePoint)

        when {
            Character.isWhitespace(codePoint) -> index += codePointLength
            codePoint <= 0x7F && Character.isLetterOrDigit(codePoint) -> {
                var runLength = 0
                while (index < text.length) {
                    val runCodePoint = text.codePointAt(index)
                    if (runCodePoint > 0x7F || !Character.isLetterOrDigit(runCodePoint)) break
                    runLength += Character.charCount(runCodePoint)
                    index += Character.charCount(runCodePoint)
                }
                tokenCount += (runLength + 3) / 4
            }
            else -> {
                tokenCount++
                index += codePointLength
            }
        }
    }

    return tokenCount
}
