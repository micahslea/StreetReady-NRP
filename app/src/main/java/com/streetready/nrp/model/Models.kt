
package com.streetready.nrp.model

data class Chapter(
    val id: String, val number: Int, val title: String, val track: String,
    val domain: String, val visual: String, val minutes: Int, val hook: String,
    val concept: String, val cues: List<String>, val priorities: List<String>,
    val pitfalls: List<String>, val prompts: List<String>
)

data class LessonCard(
    val id: String, val chapter: Int, val order: Int, val kicker: String,
    val title: String, val body: String, val bullets: List<String>,
    val callout: String, val visual: String
) {
    fun narration(chapterTitle: String): String =
        "$chapterTitle. $title. $body. " + bullets.joinToString(". ") + ". " + callout
}

data class Question(
    val id: String, val type: String, val chapter: Int, val domain: String,
    val text: String, val choices: List<String>, val correct: List<Int>,
    val explanation: String
)

data class ClinicalCase(
    val id: String, val title: String, val domain: String, val chapter: Int,
    val phase: String, val scene: String, val vitals: String,
    val steps: List<Pair<String,String>>
)

data class EcgPattern(
    val name: String, val type: String, val rate: Int,
    val criteria: String, val pearl: String
)

data class Course(
    val chapters: List<Chapter>,
    val cards: List<LessonCard>,
    val questions: List<Question>,
    val cases: List<ClinicalCase>,
    val ecgs: List<EcgPattern>
)
