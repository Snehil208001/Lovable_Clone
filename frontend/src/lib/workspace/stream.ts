import { API_BASE_URL } from "@/lib/constants";

export interface StreamAssistantResponseOptions {
  projectId: number;
  prompt: string;
  token: string | null;
  onChunk: (chunk: string) => void;
}

/**
 * POSTs the prompt to the backend SSE endpoint and reads the response stream,
 * invoking onChunk for every decoded `data:` payload. Resolves with the full
 * concatenated assistant response once the stream ends.
 *
 * Each payload is a JSON-encoded string (see ChatController) so that token
 * whitespace and newlines survive SSE framing intact.
 */
export async function streamAssistantResponse({
  projectId,
  prompt,
  token,
  onChunk,
}: StreamAssistantResponseOptions): Promise<string> {
  const response = await fetch(`${API_BASE_URL}/api/chat/stream`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: JSON.stringify({ message: prompt, projectId }),
  });

  if (!response.ok || !response.body) {
    let message = "Streaming request failed";
    try {
      const body = (await response.json()) as { message?: string };
      if (body?.message) message = body.message;
    } catch {}
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
      const payload = event
        .split(/\r?\n/)
        .filter((line) => line.startsWith("data:"))
        // Per the SSE spec, exactly one leading space after "data:" is framing.
        .map((line) => line.slice(line.startsWith("data: ") ? 6 : 5))
        .join("\n");
      if (!payload) continue;

      let chunk: string;
      try {
        chunk = JSON.parse(payload) as string;
      } catch {
        chunk = payload;
      }
      if (!chunk) continue;

      fullAssistantResponse += chunk;
      onChunk(chunk);
    }
  }

  return fullAssistantResponse;
}
