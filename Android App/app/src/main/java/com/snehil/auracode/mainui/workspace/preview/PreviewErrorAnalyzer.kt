package com.snehil.auracode.mainui.workspace.preview

/** Parse React/Sandpack crash text into a targeted repair prompt. */
internal object PreviewErrorAnalyzer {

    private val renderMethodRegex =
        Regex("""render method of [`'"]?([A-Za-z_][\w]*)[`'"]?""", RegexOption.IGNORE_CASE)
    private val undefinedRegex =
        Regex("""[`'"]?([A-Za-z_][\w]*)[`'"]? is not (?:defined|a function|exported)""", RegexOption.IGNORE_CASE)
    private val moduleNotFoundRegex =
        Regex("""(?:Cannot find module|Module not found)[`'":\s]+([^\s'"`]+)""", RegexOption.IGNORE_CASE)
    private val failedImportRegex =
        Regex("""(?:from|import)\s+[`'"]([^`'"]+)[`'"]""", RegexOption.IGNORE_CASE)

    fun extractHints(error: String): ErrorHints {
        val components = linkedSetOf<String>()
        renderMethodRegex.findAll(error).forEach { components += it.groupValues[1] }
        undefinedRegex.findAll(error).forEach { components += it.groupValues[1] }

        val modules = linkedSetOf<String>()
        moduleNotFoundRegex.findAll(error).forEach { modules += it.groupValues[1].trim() }
        failedImportRegex.findAll(error).forEach { modules += it.groupValues[1].trim() }

        return ErrorHints(
            components = components.filterNot { it in IGNORED_NAMES }.take(6),
            modules = modules.take(6)
        )
    }

    fun suspectFiles(hints: ErrorHints, allPaths: List<String>): List<String> {
        if (allPaths.isEmpty()) return emptyList()
        val needles = (hints.components + hints.modules.map { it.substringAfterLast('/') })
            .map { it.removeSuffix(".tsx").removeSuffix(".ts").removeSuffix(".jsx").removeSuffix(".js") }
            .filter { it.length >= 2 }
        if (needles.isEmpty()) {
            return allPaths.filter {
                it.contains("App.", ignoreCase = true) ||
                    it.contains("main.", ignoreCase = true) ||
                    it.contains("index.", ignoreCase = true)
            }.take(6)
        }
        return allPaths.filter { path ->
            needles.any { n -> path.contains(n, ignoreCase = true) }
        }.distinct().take(10)
    }

    fun buildFixPrompt(error: String, allPaths: List<String>): String {
        val hints = extractHints(error)
        val suspects = suspectFiles(hints, allPaths)
        return buildString {
            appendLine("PREVIEW CRASH — fix so the app renders. Output corrected <file> blocks only (close every </file>).")
            appendLine("Likely cause: named vs default export mismatch, wrong import path, or undefined component.")
            if (hints.components.isNotEmpty()) {
                appendLine("Broken component(s): ${hints.components.joinToString(", ")}")
            }
            if (hints.modules.isNotEmpty()) {
                appendLine("Module/import hint(s): ${hints.modules.joinToString(", ")}")
            }
            if (suspects.isNotEmpty()) {
                appendLine("Check/fix these files first:")
                suspects.forEach { appendLine("- $it") }
            }
            appendLine("Also ensure src/App.tsx imports them correctly.")
            appendLine("No long explanation — write the fixed files.")
            appendLine()
            appendLine("Error:")
            appendLine(error.take(1_200))
        }
    }

    private val IGNORED_NAMES = setOf(
        "undefined", "null", "Object", "Array", "String", "Number", "Boolean",
        "React", "Component", "Fragment", "Error", "TypeError", "ReferenceError"
    )
}

internal data class ErrorHints(
    val components: List<String>,
    val modules: List<String>
)
