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
    .replace(new RegExp(`<file[^>]*path=(["'])([^"']+)\\1[^>]*>[\\s\\S]*?${FILE_END}`, "gi"), '\n\n📝 Updated $2\n')
    .replace(new RegExp(`<tool[^>]*>[\\s\\S]*?${TOOL_END}`, "gi"), '')
    .replace(new RegExp(`<message[^>]*>([\\s\\S]*?)${MESSAGE_END}`, "gi"), '\n$1\n')
    // Streaming-safe fallbacks: tags whose closing tag has not arrived yet.
    .replace(/<file[^>]*path=(["'])([^"']+)\1[^>]*>[\s\S]*$/i, '\n\n📝 Writing $2…\n')
    .replace(/<tool[^>]*>[\s\S]*$/i, '')
    .replace(/<message[^>]*>([\s\S]*)$/i, '\n$1')
    // Scrub any dangling stray closers the rules above did not consume.
    .replace(/<\/arg_value>/gi, '')
    // A partially received tag at the very end of the buffer, e.g.
    // `<file path="src/...` (attributes still streaming) or `<mess`.
    .replace(/<\/?(?:file|tool|message)(?:\s[^>]*)?$/i, '')
    .replace(/<\/?[a-z]{0,7}$/i, '')
    .trim();
}

export function delay(ms: number): Promise<void> {
  return new Promise((resolve) => {
    setTimeout(resolve, ms);
  });
}
