"use client";

import { useCallback, useRef } from "react";

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
 * message, refreshes on mid-stream file_ready events, then syncs tree/preview.
 */
export function useWorkspaceChat(projectId: number, { loadFileTree, refreshPreview }: UseWorkspaceChatDeps) {
  const token = useAuthStore((state) => state.token);
  const abortRef = useRef<AbortController | null>(null);

  const stopGeneration = useCallback(() => {
    abortRef.current?.abort();
    abortRef.current = null;
    useWorkspaceStore.getState().setIsSending(false);
  }, []);

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
      const controller = new AbortController();
      abortRef.current = controller;

      appendMessages([
        { id: userMessageId, role: "USER", content: trimmedPrompt },
        { id: assistantMessageId, role: "ASSISTANT", content: "" },
      ]);

      const readyPaths = new Set<string>();

      try {
        const assistantResponse = await streamAssistantResponse({
          projectId,
          prompt: trimmedPrompt,
          token,
          signal: controller.signal,
          onChunk: (chunk) => appendToMessage(assistantMessageId, chunk),
          onFileReady: (path) => {
            readyPaths.add(path);
            void (async () => {
              const latestPaths = await loadFileTree(false);
              await refreshPreview(latestPaths);
            })();
          },
          onStreamError: (message) => setChatError(message),
        });

        if (!assistantResponse.trim() && readyPaths.size === 0) {
          throw new Error("The AI returned an empty response");
        }

        const expectedFilePaths = extractFilePathsFromResponse(assistantResponse);
        let latestPaths = await loadFileTree(false);

        for (let attempt = 0; attempt < MAX_FILE_SYNC_ATTEMPTS; attempt += 1) {
          const hasExpectedFiles =
            expectedFilePaths.length === 0 ||
            expectedFilePaths.every((path) => latestPaths.includes(path));
          if (hasExpectedFiles || (readyPaths.size > 0 && attempt >= 2)) break;
          await delay(FILE_SYNC_DELAY_MS);
          latestPaths = await loadFileTree(false);
        }

        await refreshPreview(latestPaths);
      } catch (error) {
        if (error instanceof DOMException && error.name === "AbortError") {
          fillEmptyMessage(assistantMessageId, "Generation stopped.");
          const latestPaths = await loadFileTree(false);
          await refreshPreview(latestPaths);
          return;
        }
        const detail =
          error instanceof Error && error.message !== "Streaming request failed"
            ? ` (${error.message})`
            : "";
        setChatError(`Streaming failed. Please try again.${detail}`);
        fillEmptyMessage(assistantMessageId, "I hit an error while generating a response.");
      } finally {
        if (abortRef.current === controller) abortRef.current = null;
        setIsSending(false);
      }
    },
    [loadFileTree, projectId, refreshPreview, token],
  );

  return { sendPrompt, stopGeneration };
}
