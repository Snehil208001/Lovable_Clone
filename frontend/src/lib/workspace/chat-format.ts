import { AxiosError } from "axios";

export function isRenderableChatRole(role: string): role is "USER" | "ASSISTANT" {
  return role === "USER" || role === "ASSISTANT";
}

export function getErrorMessage(error: unknown, fallback: string): string {
  return (error as AxiosError<{ message?: string }>)?.response?.data?.message || fallback;
}

export function extractFilePathsFromResponse(responseText: string): string[] {
  const matches = responseText.matchAll(/<file[^>]*path=(["'])([^"']+)\1[^>]*>/gi);
  return Array.from(matches, (match) => (match[2] || "").replace(/^\//, "")).filter(Boolean);
}

// A block ends at its proper closing tag, a stray </arg_value> (some models
// close blocks with their native tool-call template tag), or right before the
// next opening tag when the closing tag was dropped entirely.
const FILE_END = /(?:<\/file>|<\/arg_value>|(?=<(?:file|message|tool)[\s>]))/.source;
const TOOL_END = /(?:<\/tool>|<\/arg_value>|(?=<(?:file|message|tool)[\s>]))/.source;
const MESSAGE_END = /(?:<\/message>|<\/arg_value>|(?=<(?:file|message|tool)[\s>]))/.source;

export function formatChatContent(content: string): string {
  if (!content) return content;
  return content
    .replace(
      new RegExp(`<file[^>]*path=(["'])([^"']+)\\1[^>]*>[\\s\\S]*?${FILE_END}`, "gi"),
      "\n\n[Wrote] $2\n",
    )
    .replace(
      new RegExp(`<tool[^>]*args=(["'])([^"']+)\\1[^>]*>[\\s\\S]*?${TOOL_END}`, "gi"),
      "\n[Reading] $2\n",
    )
    .replace(new RegExp(`<tool[^>]*>[\\s\\S]*?${TOOL_END}`, "gi"), "\n[Reading files]\n")
    .replace(new RegExp(`<message[^>]*>([\\s\\S]*?)${MESSAGE_END}`, "gi"), "\n$1\n")
    .replace(/<file[^>]*path=(["'])([^"']+)\1[^>]*>[\s\S]*$/i, "\n\n[Writing] $2…\n")
    .replace(/<tool[^>]*args=(["'])([^"']+)\1[^>]*>[\s\S]*$/i, "\n[Reading] $2…\n")
    .replace(/<tool[^>]*>[\s\S]*$/i, "\n[Reading files]…\n")
    .replace(/<message[^>]*>([\s\S]*)$/i, "\n$1")
    .replace(/<\/arg_value>/gi, "")
    .replace(/<\/?(?:file|tool|message)(?:\s[^>]*)?$/i, "")
    .replace(/<\/?[a-z]{0,7}$/i, "")
    .trim();
}

/** Structured timeline rows for the chat UI (reading / writing / done). */
export type ChatTimelineItem =
  | { kind: "text"; text: string }
  | { kind: "action"; action: "reading" | "writing" | "wrote"; detail: string };

export function parseChatTimeline(content: string): ChatTimelineItem[] {
  const formatted = formatChatContent(content);
  if (!formatted) return [];

  const items: ChatTimelineItem[] = [];
  for (const line of formatted.split("\n")) {
    const trimmed = line.trim();
    if (!trimmed) continue;
    const wrote = trimmed.match(/^\[Wrote\]\s+(.+)$/i);
    const writing = trimmed.match(/^\[Writing\]\s+(.+?)(?:…|\.\.\.)?$/i);
    const reading = trimmed.match(/^\[Reading\]\s+(.+?)(?:…|\.\.\.)?$/i);
    if (wrote) items.push({ kind: "action", action: "wrote", detail: wrote[1] });
    else if (writing) items.push({ kind: "action", action: "writing", detail: writing[1] });
    else if (reading) items.push({ kind: "action", action: "reading", detail: reading[1] });
    else if (/^\[Reading files\]/i.test(trimmed)) {
      items.push({ kind: "action", action: "reading", detail: "project files" });
    } else {
      items.push({ kind: "text", text: trimmed });
    }
  }
  return items;
}

export function delay(ms: number): Promise<void> {
  return new Promise((resolve) => {
    setTimeout(resolve, ms);
  });
}
