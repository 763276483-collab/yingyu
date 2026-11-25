package com.rolechat.app.voice

/** Strips parenthetical / bracketed stage directions so TTS reads only spoken dialogue. */
object SpeechTextFilter {
    private val narrationRegex = Regex("""（[^）]*）|\([^)]*\)|【[^】]*】|\*[^*\n]+\*""")
    private val multiSpaceRegex = Regex("""[ \t\u3000]+""")
    private val multiNewlineRegex = Regex("""\n{2,}""")

    fun spokenOnly(raw: String): String {
        val stripped = narrationRegex.replace(raw, " ")
            .replace(multiSpaceRegex, " ")
            .replace(multiNewlineRegex, "\n")
            .lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString("\n")
            .trim()
        return if (stripped.any { it.isLetterOrDigit() }) stripped else ""
    }
}
