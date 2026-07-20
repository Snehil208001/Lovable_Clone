package com.snehil.auracode.mainui.workspace.chat

/** Structured timeline rows for the chat UI (reading / writing / done). */
sealed class ChatTimelineItem {
    data class Text(val text: String) : ChatTimelineItem()
    data class Action(val action: ChatAction, val detail: String) : ChatTimelineItem()
}

enum class ChatAction { READING, WRITING, WROTE }

private const val FILE_END = """(?:</file>|</arg_value>|(?=<(?:file|message|tool)[\s>]))"""
private const val TOOL_END = """(?:</tool>|</arg_value>|(?=<(?:file|message|tool)[\s>]))"""
private const val MESSAGE_END = """(?:</message>|</arg_value>|(?=<(?:file|message|tool)[\s>]))"""

private val fileBlockRegex = Regex("""<file[^>]*path=(["'])([^"']+)\1[^>]*>[\s\S]*?$FILE_END""", RegexOption.IGNORE_CASE)
private val toolArgsBlockRegex = Regex("""<tool[^>]*args=(["'])([^"']+)\1[^>]*>[\s\S]*?$TOOL_END""", RegexOption.IGNORE_CASE)
private val toolBlockRegex = Regex("""<tool[^>]*>[\s\S]*?$TOOL_END""", RegexOption.IGNORE_CASE)
private val messageBlockRegex = Regex("""<message[^>]*>([\s\S]*?)$MESSAGE_END""", RegexOption.IGNORE_CASE)
private val filePartialRegex = Regex("""<file[^>]*path=(["'])([^"']+)\1[^>]*>[\s\S]*$""", RegexOption.IGNORE_CASE)
private val toolArgsPartialRegex = Regex("""<tool[^>]*args=(["'])([^"']+)\1[^>]*>[\s\S]*$""", RegexOption.IGNORE_CASE)
private val toolPartialRegex = Regex("""<tool[^>]*>[\s\S]*$""", RegexOption.IGNORE_CASE)
private val messagePartialRegex = Regex("""<message[^>]*>([\s\S]*)$""", RegexOption.IGNORE_CASE)
private val wroteLineRegex = Regex("""^\[Wrote\]\s+(.+)$""", RegexOption.IGNORE_CASE)
private val writingLineRegex = Regex("""^\[Writing\]\s+(.+?)(?:…|\.\.\.)?$""", RegexOption.IGNORE_CASE)
private val readingLineRegex = Regex("""^\[Reading\]\s+(.+?)(?:…|\.\.\.)?$""", RegexOption.IGNORE_CASE)
private val readingFilesRegex = Regex("""^\[Reading files\]""", RegexOption.IGNORE_CASE)
private val fencedCodeRegex = Regex("""```[\w+\-]*\n?[\s\S]*?```""")
private val partialFenceRegex = Regex("""```[\w+\-]*\n?[\s\S]*$""")
private val rawTagRegex = Regex("""</?(?:file|tool|message|arg_value)(?:\s[^>]*)?/?>""", RegexOption.IGNORE_CASE)

/** Strip XML/tool/code payloads — chat shows vibe activity, not source. */
fun formatChatContent(content: String): String {
    if (content.isBlank()) return content
    return content
        .replace(fencedCodeRegex, "\n")
        .replace(partialFenceRegex, "\n")
        .replace(fileBlockRegex, "\n\n[Wrote] $2\n")
        .replace(toolArgsBlockRegex, "\n[Reading] $2\n")
        .replace(toolBlockRegex, "\n[Reading files]\n")
        .replace(messageBlockRegex, "\n$1\n")
        .replace(filePartialRegex, "\n\n[Writing] $2…\n")
        .replace(toolArgsPartialRegex, "\n[Reading] $2…\n")
        .replace(toolPartialRegex, "\n[Reading files]…\n")
        .replace(messagePartialRegex, "\n$1")
        .replace(rawTagRegex, "")
        .replace(Regex("""</?[a-z]{0,12}$""", RegexOption.IGNORE_CASE), "")
        .trim()
}

/**
 * @param complete when true (stream finished), unfinished [Writing] rows become Applied
 * and duplicate Exploring/Building rows for the same file collapse to Applied.
 */
fun parseChatTimeline(content: String, complete: Boolean = false): List<ChatTimelineItem> {
    val formatted = formatChatContent(content)
    if (formatted.isBlank()) return emptyList()

    val items = mutableListOf<ChatTimelineItem>()
    for (line in formatted.lines()) {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) continue
        when {
            wroteLineRegex.matches(trimmed) ->
                items += ChatTimelineItem.Action(ChatAction.WROTE, shortPath(wroteLineRegex.matchEntire(trimmed)!!.groupValues[1]))
            writingLineRegex.matches(trimmed) -> {
                val path = shortPath(writingLineRegex.matchEntire(trimmed)!!.groupValues[1])
                // Incomplete </file> tags stay as Writing while streaming; settle to Applied when done.
                items += ChatTimelineItem.Action(
                    if (complete) ChatAction.WROTE else ChatAction.WRITING,
                    path
                )
            }
            readingLineRegex.matches(trimmed) ->
                items += ChatTimelineItem.Action(ChatAction.READING, shortPath(readingLineRegex.matchEntire(trimmed)!!.groupValues[1]))
            readingFilesRegex.containsMatchIn(trimmed) ->
                items += ChatTimelineItem.Action(ChatAction.READING, "project files")
            looksLikeCode(trimmed) -> Unit // vibe coding: never dump source into chat
            else -> items += ChatTimelineItem.Text(trimmed)
        }
    }
    return if (complete) settleTimeline(items) else items
}

/** Keep prose + one strongest action per file (Applied > Building > Exploring). */
private fun settleTimeline(items: List<ChatTimelineItem>): List<ChatTimelineItem> {
    val texts = items.filterIsInstance<ChatTimelineItem.Text>()
    val bestByFile = linkedMapOf<String, ChatTimelineItem.Action>()
    for (action in items.filterIsInstance<ChatTimelineItem.Action>()) {
        val prev = bestByFile[action.detail]
        if (prev == null || actionRank(action.action) >= actionRank(prev.action)) {
            bestByFile[action.detail] = action
        }
    }
    return texts + bestByFile.values
}

private fun actionRank(action: ChatAction): Int = when (action) {
    ChatAction.WROTE -> 3
    ChatAction.WRITING -> 2
    ChatAction.READING -> 1
}

fun shortPath(path: String): String =
    path.trim().removePrefix("/").substringAfterLast('/').ifBlank { path }

private fun looksLikeCode(line: String): Boolean {
    val t = line.trim()
    if (t.startsWith("```") || t.endsWith("```")) return true
    if (t.startsWith("<") && t.contains(">")) return true
    if (Regex("""^(import|export|from|const|let|var|function|class|type|interface|return)\b""").containsMatchIn(t)) return true
    if (t.contains("{") && t.contains("}") && t.length > 48) return true
    val symbols = t.count { it in "{}();=<>[]" }
    return symbols >= 4 && symbols * 3 >= t.length
}
