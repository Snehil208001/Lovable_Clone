import { API_BASE_URL } from "@/lib/constants";

export interface StreamAssistantResponseOptions {
  projectId: number;
  prompt: string;
  token: string | null;
  signal?: AbortSignal;
  onChunk: (chunk: string) => void;
  onFileReady?: (path: string) => void;
  onStreamError?: (message: string) => void;
}

/**
 * POSTs the prompt to the backend SSE endpoint and reads the response stream,
 * invoking onChunk for every decoded `data:` payload. Resolves with the full
 * concatenated assistant response once the stream ends.
 *
 * Each payload is a JSON-encoded string (see ChatController) so that token
 * whitespace and newlines survive SSE framing intact.
 * Events: `chunk` (default), `file_ready` ({ path }), `error` (message).
 */
export async function streamAssistantResponse({
  projectId,
  prompt,
  token,
  signal,
  onChunk,
  onFileReady,
  onStreamError,
}: StreamAssistantResponseOptions): Promise<string> {
  const response = await fetch(`${API_BASE_URL}/api/chat/stream`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: JSON.stringify({ message: prompt, projectId }),
    signal,
  });

  if (!response.ok || !response.body) {
    let message = "Streaming request failed";
    try {
      const body = (await response.json()) as { message?: string };
      if (body?.message) message = body.message;
    } catch {
      /* ignore */
    }
    throw new Error(message);
  }

  const reader = response.body.getReader();
  const decoder = new TextDecoder("utf-8");
  let buffer = "";
  let fullAssistantResponse = "";

  while (true) {
    const { done, value } = await reader.read();
    if (done) break;

    buffer += decoder.decode(value, { stream: true });
    const events = buffer.split(/\r?\n\r?\n/);
    buffer = events.pop() || "";

    for (const event of events) {
      const lines = event.split(/\r?\n/);
      let eventName = "chunk";
      const dataLines: string[] = [];

      for (const line of lines) {
        if (line.startsWith("event:")) {
          eventName = line.slice(6).trim() || "chunk";
        } else if (line.startsWith("data:")) {
          dataLines.push(line.slice(line.startsWith("data: ") ? 6 : 5));
        }
      }

      const payload = dataLines.join("\n");
      if (!payload) continue;

      let parsed: unknown;
      try {
        parsed = JSON.parse(payload);
      } catch {
        parsed = payload;
      }

      if (eventName === "error") {
        const message = typeof parsed === "string" ? parsed : "Stream failed";
        onStreamError?.(message);
        throw new Error(message);
      }

      if (eventName === "file_ready") {
        const path =
          typeof parsed === "object" &&
          parsed !== null &&
          "path" in parsed &&
          typeof (parsed as { path: unknown }).path === "string"
            ? (parsed as { path: string }).path
            : typeof parsed === "string"
              ? parsed
              : "";
        if (path) onFileReady?.(path);
        continue;
      }

      const chunk = typeof parsed === "string" ? parsed : "";
      if (!chunk) continue;

      fullAssistantResponse += chunk;
      onChunk(chunk);
    }
  }

  return fullAssistantResponse;
}
