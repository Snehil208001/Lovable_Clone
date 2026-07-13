"use client";

import { useCallback } from "react";

import { delay, extractFilePathsFromResponse } from "@/lib/workspace/chat-format";
import { streamAssistantResponse } from "@/lib/workspace/stream";
import { useAuthStore } from "@/stores/auth-store";
import { useWorkspaceStore } from "@/stores/workspace-store";

interface UseWorkspaceChatDeps {
  loadFileTree: (selectFirstFile?: boolean) => Promise<string[]>;
  refreshPreview: (pathsOverride?: string[]) => Promise<void>;
}

const MAX_FILE_SYNC_ATTEMPTS = 8;
const FILE_SYNC_DELAY_MS = 800;

/**
 * Sends a prompt to the AI stream, applies chunks to the optimistic assistant
 * message, then polls the file tree until the generated files land and
 * refreshes the preview.
 */
export function useWorkspaceChat(projectId: number, { loadFileTree, refreshPreview }: UseWorkspaceChatDeps) {
  const token = useAuthStore((state) => state.token);

  const sendPrompt = useCallback(
    async (trimmedPrompt: string) => {
      const {
        isSending,
        setIsSending,
        setChatError,
        appendMessages,
        appendToMessage,
        fillEmptyMessage,
      } = useWorkspaceStore.getState();

      if (!trimmedPrompt || isSending) return;

      setChatError(null);
      setIsSending(true);

      const userMessageId = `user-${Date.now()}`;
      const assistantMessageId = `assistant-${Date.now()}`;

      appendMessages([
        { id: userMessageId, role: "USER", content: trimmedPrompt },
        { id: assistantMessageId, role: "ASSISTANT", content: "" },
      ]);

      try {
        const assistantResponse = await streamAssistantResponse({
          projectId,
          prompt: trimmedPrompt,
          token,
          onChunk: (chunk) => appendToMessage(assistantMessageId, chunk),
        });
        const expectedFilePaths = extractFilePathsFromResponse(assistantResponse);

        let latestPaths = await loadFileTree(false);

        for (let attempt = 0; attempt < MAX_FILE_SYNC_ATTEMPTS; attempt += 1) {
          const hasExpectedFiles = expectedFilePaths.every((path) => latestPaths.includes(path));
          if (hasExpectedFiles) break;
          await delay(FILE_SYNC_DELAY_MS);
          latestPaths = await loadFileTree(false);
        }

        await refreshPreview(latestPaths);
      } catch (error) {
        const detail = error instanceof Error && error.message !== "Streaming request failed" ? ` (${error.message})` : "";
        setChatError(`Streaming failed. Please try again.${detail}`);
        fillEmptyMessage(assistantMessageId, "I hit an error while generating a response.");
      } finally {
        setIsSending(false);
      }
    },
    [loadFileTree, projectId, refreshPreview, token],
  );

  return { sendPrompt };
}
