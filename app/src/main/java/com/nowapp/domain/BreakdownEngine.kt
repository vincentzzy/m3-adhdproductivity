package com.nowapp.domain

/** One step of a breakdown, with the time it is expected to take. */
data class BreakdownStep(val text: String, val minutes: Int)

/**
 * A breakdown is always 3-5 steps and its first step always takes 5 minutes or
 * less. That constraint is enforced in the constructor so it cannot silently
 * regress: the first step's whole job is to defeat task-initiation paralysis.
 */
data class Breakdown(val steps: List<BreakdownStep>) {
    init {
        require(steps.size in 3..5) { "A breakdown must have 3 to 5 steps, had ${steps.size}" }
        require(steps.first().minutes <= 5) { "The first step must take 5 minutes or less" }
    }
}

/**
 * Offline, heuristic task breakdown. No LLM, no network: keyword families map to
 * hand-written step templates that are useful for a student's actual tasks.
 */
object BreakdownEngine {

    private data class Rule(val keywords: List<String>, val steps: List<BreakdownStep>)

    private fun step(text: String, minutes: Int) = BreakdownStep(text, minutes)

    private val rules = listOf(
        Rule(
            listOf("essay", "report", "write", "paper", "draft", "thesis", "document"),
            listOf(
                step("Open the document and write one ugly sentence", 3),
                step("List the three points you want to make", 5),
                step("Write a rough paragraph for each point", 20),
                step("Reread once and fix only the worst sentence", 10),
            ),
        ),
        Rule(
            listOf("read", "reading", "chapter", "article", "notes", "review", "revise", "study", "exam"),
            listOf(
                step("Open it and read the first paragraph out loud", 4),
                step("Highlight or copy the three key lines", 10),
                step("Close it and say the gist in one sentence", 5),
                step("Decide the next chunk and stop here", 2),
            ),
        ),
        Rule(
            listOf("code", "coding", "program", "assignment", "project", "debug", "bug", "implement", "lab", "build"),
            listOf(
                step("Open the editor and run the project once", 5),
                step("Write down the exact next thing to change", 5),
                step("Make that one change", 20),
                step("Run it and note what happened", 10),
            ),
        ),
        Rule(
            listOf("email", "reply", "message", "text", "dm", "contact", "professor", "teacher"),
            listOf(
                step("Open the thread and read it once", 2),
                step("Type the first line only", 3),
                step("Finish it in one pass, typos allowed", 10),
                step("Send it and close the app", 1),
            ),
        ),
        Rule(
            listOf("clean", "tidy", "wash", "dishes", "laundry", "trash", "room", "desk"),
            listOf(
                step("Set a 5 minute timer and clear one surface", 5),
                step("Put the obvious rubbish in a bag", 10),
                step("Return the five biggest things to where they live", 10),
            ),
        ),
        Rule(
            listOf("submit", "upload", "apply", "form", "portal", "enrol", "register", "renew"),
            listOf(
                step("Open the portal and log in", 4),
                step("Screenshot the requirements list", 3),
                step("Do the first field or upload", 10),
                step("Submit and save the receipt", 5),
            ),
        ),
        Rule(
            listOf("buy", "order", "shop", "groceries", "purchase"),
            listOf(
                step("Write the three things you actually need", 3),
                step("Open the app and put them in the basket", 8),
                step("Check out", 5),
            ),
        ),
        Rule(
            listOf("call", "phone", "ring", "book", "appointment", "doctor"),
            listOf(
                step("Write the one sentence you will say", 3),
                step("Find the number", 2),
                step("Call and read the sentence", 10),
            ),
        ),
        Rule(
            listOf("present", "slide", "deck", "pitch", "demo"),
            listOf(
                step("Open the deck and title the first slide", 5),
                step("Add one bullet per slide idea", 15),
                step("Say the talk out loud once, standing", 15),
            ),
        ),
        Rule(
            listOf("exercise", "walk", "gym", "run", "stretch", "shower"),
            listOf(
                step("Put shoes on", 2),
                step("Go outside for ten minutes", 10),
                step("Decide whether to continue", 1),
            ),
        ),
    )

    private val fallback = listOf(
        step("Spend 3 minutes writing what 'done' looks like", 3),
        step("Collect everything you need in one place", 5),
        step("Do the smallest visible piece", 20),
        step("Stop and decide the next tiny piece", 2),
    )

    fun forTask(title: String): Breakdown {
        val lower = title.lowercase()
        val rule = rules.firstOrNull { candidate -> candidate.keywords.any { lower.contains(it) } }
        return Breakdown(rule?.steps ?: fallback)
    }
}
