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

/** Strip XML tool tags and normalize assistant output for display. */
fun formatChatContent(content: String): String {
    if (content.isBlank()) return content
    return content
        .replace(fileBlockRegex, "\n\n[Wrote] $2\n")
        .replace(toolArgsBlockRegex, "\n[Reading] $2\n")
        .replace(toolBlockRegex, "\n[Reading files]\n")
        .replace(messageBlockRegex, "\n$1\n")
        .replace(filePartialRegex, "\n\n[Writing] $2…\n")
        .replace(toolArgsPartialRegex, "\n[Reading] $2…\n")
        .replace(toolPartialRegex, "\n[Reading files]…\n")
        .replace(messagePartialRegex, "\n$1")
        .replace(Regex("""</arg_value>""", RegexOption.IGNORE_CASE), "")
        .replace(Regex("""</?(?:file|tool|message)(?:\s[^>]*)?$""", RegexOption.IGNORE_CASE), "")
        .replace(Regex("""</?[a-z]{0,7}$""", RegexOption.IGNORE_CASE), "")
        .trim()
}

fun parseChatTimeline(content: String): List<ChatTimelineItem> {
    val formatted = formatChatContent(content)
    if (formatted.isBlank()) return emptyList()

    val items = mutableListOf<ChatTimelineItem>()
    for (line in formatted.lines()) {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) continue
        when {
            wroteLineRegex.matches(trimmed) ->
                items += ChatTimelineItem.Action(ChatAction.WROTE, wroteLineRegex.matchEntire(trimmed)!!.groupValues[1])
            writingLineRegex.matches(trimmed) ->
                items += ChatTimelineItem.Action(ChatAction.WRITING, writingLineRegex.matchEntire(trimmed)!!.groupValues[1])
            readingLineRegex.matches(trimmed) ->
                items += ChatTimelineItem.Action(ChatAction.READING, readingLineRegex.matchEntire(trimmed)!!.groupValues[1])
            readingFilesRegex.containsMatchIn(trimmed) ->
                items += ChatTimelineItem.Action(ChatAction.READING, "project files")
            else -> items += ChatTimelineItem.Text(trimmed)
        }
    }
    return items
}
