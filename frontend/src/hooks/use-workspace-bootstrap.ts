"use client";

import { useEffect, useState, type RefObject } from "react";

import { ApiClient } from "@/lib/api-client";
import { getErrorMessage, isRenderableChatRole } from "@/lib/workspace/chat-format";
import { useWorkspaceStore } from "@/stores/workspace-store";
import type { ChatHistoryMessage, ChatMessage } from "@/types/workspace";

interface UseWorkspaceBootstrapDeps {
  loadFileTree: (selectFirstFile?: boolean) => Promise<string[]>;
  refreshPreview: (pathsOverride?: string[]) => Promise<void>;
}

/**
 * Loads the project, chat history (unless messages were restored from
 * localStorage), file tree, and initial preview on mount.
 */
export function useWorkspaceBootstrap(
  projectId: number,
  persistedMessagesRef: RefObject<ChatMessage[] | null>,
  { loadFileTree, refreshPreview }: UseWorkspaceBootstrapDeps,
) {
  const [isLoadingWorkspace, setIsLoadingWorkspace] = useState(true);
  const [workspaceError, setWorkspaceError] = useState<string | null>(null);

  useEffect(() => {
    let mounted = true;

    async function bootstrapWorkspace() {
      if (Number.isNaN(projectId)) {
        setWorkspaceError("Invalid project id.");
        setIsLoadingWorkspace(false);
        return;
      }

      setIsLoadingWorkspace(true);
      setWorkspaceError(null);

      try {
        const projectResponse = await ApiClient.getProjectById(projectId);

        let chatData: ChatHistoryMessage[] = [];
        try {
          const chatResponse = await ApiClient.getChatHistory(projectId);
          chatData = chatResponse.data;
        } catch (e) {
          console.warn("Chat history not available yet", e);
        }

        if (!mounted) return;

        const { setActiveProject, setMessages } = useWorkspaceStore.getState();
        setActiveProject(projectResponse.data);
        if (!persistedMessagesRef.current?.length) {
          setMessages(
            chatData.flatMap((message) =>
              isRenderableChatRole(message.role)
                ? [{ id: String(message.id), role: message.role, content: message.content }]
                : [],
            ),
          );
        }
        const initialPaths = await loadFileTree(true);
        await refreshPreview(initialPaths);
      } catch (error) {
        if (mounted) setWorkspaceError(getErrorMessage(error, "Failed to load workspace."));
      } finally {
        if (mounted) setIsLoadingWorkspace(false);
      }
    }

    void bootstrapWorkspace();
    return () => { mounted = false; };
  }, [loadFileTree, persistedMessagesRef, projectId, refreshPreview]);

  return { isLoadingWorkspace, workspaceError };
}
