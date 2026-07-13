"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import Link from "next/link";
import { useParams } from "next/navigation";
import { Loader2 } from "lucide-react";

import { Button } from "@/components/ui/button";
import { ProjectSettingsDialog } from "@/components/project-settings-dialog";
import { ChatPanel } from "@/components/workspace/chat-panel";
import { CodePanel } from "@/components/workspace/code-panel";
import { PreviewPane } from "@/components/workspace/preview-pane";
import { useWorkspaceBootstrap } from "@/hooks/use-workspace-bootstrap";
import { useWorkspaceChat } from "@/hooks/use-workspace-chat";
import { useWorkspaceFiles } from "@/hooks/use-workspace-files";
import { buildExplorerTree, collectFolderPaths } from "@/lib/workspace/explorer-tree";
import { useWorkspaceStore } from "@/stores/workspace-store";
import type { ChatMessage } from "@/types/workspace";

export default function WorkspacePage() {
  const params = useParams<{ projectId: string }>();
  const projectId = Number(params.projectId);
  const chatStorageKey = `workspace:${projectId}:messages`;
  const selectedFileStorageKey = `workspace:${projectId}:selected-file`;
  const panelsStorageKey = `workspace:${projectId}:panels`;
  const foldersStorageKey = `workspace:${projectId}:folders`;

  const persistedMessagesRef = useRef<ChatMessage[] | null>(null);
  const [selectedViewport, setSelectedViewport] = useState<"desktop" | "mobile">("desktop");
  const [isPreviewMaximized, setIsPreviewMaximized] = useState(false);
  const [isChatCollapsed, setIsChatCollapsed] = useState(false);
  const [isCodeCollapsed, setIsCodeCollapsed] = useState(false);
  const [isSettingsOpen, setIsSettingsOpen] = useState(false);
  const [expandedFolders, setExpandedFolders] = useState<Record<string, boolean>>({});

  const fileTree = useWorkspaceStore((state) => state.fileTree);
  const messages = useWorkspaceStore((state) => state.messages);
  const selectedFilePath = useWorkspaceStore((state) => state.selectedFilePath);
  const setMessages = useWorkspaceStore((state) => state.setMessages);
  const setSelectedFilePath = useWorkspaceStore((state) => state.setSelectedFilePath);
  const setSelectedFileContent = useWorkspaceStore((state) => state.setSelectedFileContent);
  const resetWorkspace = useWorkspaceStore((state) => state.resetWorkspace);

  const { loadFileContent, loadFileTree, refreshPreview } = useWorkspaceFiles(projectId);
  const { sendPrompt } = useWorkspaceChat(projectId, { loadFileTree, refreshPreview });
  const { isLoadingWorkspace, workspaceError } = useWorkspaceBootstrap(projectId, persistedMessagesRef, {
    loadFileTree,
    refreshPreview,
  });

  const explorerTree = useMemo(() => buildExplorerTree(fileTree), [fileTree]);

  useEffect(() => {
    return () => {
      resetWorkspace();
    };
  }, [resetWorkspace]);

  // One-time hydration-safe restore: reading localStorage in a lazy useState
  // initializer would run during SSR/hydration and mismatch the server markup.
  /* eslint-disable react-hooks/set-state-in-effect */
  useEffect(() => {
    if (typeof window === "undefined") return;

    const rawMessages = window.localStorage.getItem(chatStorageKey);
    const rawSelectedFile = window.localStorage.getItem(selectedFileStorageKey);
    const rawPanels = window.localStorage.getItem(panelsStorageKey);
    const rawFolders = window.localStorage.getItem(foldersStorageKey);

    if (rawMessages) {
      try {
        const parsed = JSON.parse(rawMessages) as ChatMessage[];
        if (Array.isArray(parsed)) {
          persistedMessagesRef.current = parsed;
          setMessages(parsed);
        }
      } catch {
        persistedMessagesRef.current = null;
      }
    }

    if (rawSelectedFile) setSelectedFilePath(rawSelectedFile);

    if (rawPanels) {
      try {
        const parsed = JSON.parse(rawPanels) as { isChatCollapsed?: boolean; isCodeCollapsed?: boolean; };
        if (typeof parsed.isChatCollapsed === "boolean") setIsChatCollapsed(parsed.isChatCollapsed);
        if (typeof parsed.isCodeCollapsed === "boolean") setIsCodeCollapsed(parsed.isCodeCollapsed);
      } catch {}
    }

    if (rawFolders) {
      try {
        const parsed = JSON.parse(rawFolders) as Record<string, boolean>;
        if (parsed && typeof parsed === "object") setExpandedFolders(parsed);
      } catch {}
    }
  }, [chatStorageKey, foldersStorageKey, panelsStorageKey, selectedFileStorageKey, setMessages, setSelectedFilePath]);
  /* eslint-enable react-hooks/set-state-in-effect */

  useEffect(() => {
    if (typeof window !== "undefined") {
      window.localStorage.setItem(chatStorageKey, JSON.stringify(messages));
    }
  }, [chatStorageKey, messages]);

  useEffect(() => {
    if (typeof window !== "undefined" && selectedFilePath) {
      window.localStorage.setItem(selectedFileStorageKey, selectedFilePath);
    }
  }, [selectedFilePath, selectedFileStorageKey]);

  useEffect(() => {
    if (typeof window !== "undefined") {
      window.localStorage.setItem(panelsStorageKey, JSON.stringify({ isChatCollapsed, isCodeCollapsed }));
    }
  }, [isChatCollapsed, isCodeCollapsed, panelsStorageKey]);

  useEffect(() => {
    if (typeof window !== "undefined") {
      window.localStorage.setItem(foldersStorageKey, JSON.stringify(expandedFolders));
    }
  }, [expandedFolders, foldersStorageKey]);

  function onSendPrompt(trimmedPrompt: string) {
    // Automatically minimize preview if you send a new prompt
    if (isPreviewMaximized) {
      setIsPreviewMaximized(false);
    }
    void sendPrompt(trimmedPrompt);
  }

  function onResetLayout() {
    if (typeof window !== "undefined") {
      window.localStorage.removeItem(panelsStorageKey);
      window.localStorage.removeItem(foldersStorageKey);
      window.localStorage.removeItem(selectedFileStorageKey);
    }

    setIsChatCollapsed(false);
    setIsCodeCollapsed(false);
    setIsPreviewMaximized(false);
    const allFolders = collectFolderPaths(explorerTree);
    setExpandedFolders(Object.fromEntries(allFolders.map((path) => [path, true])));

    const defaultFile = fileTree.find(p => !p.includes('/')) || fileTree[0] || null;
    setSelectedFilePath(defaultFile);
    if (defaultFile) {
      void loadFileContent(defaultFile, true);
    } else {
      setSelectedFileContent("");
    }
  }

  const chatPanelWidth = isChatCollapsed ? "w-14 min-w-14" : "w-[25%] min-w-[300px] max-w-[420px]";
  const codePanelWidth = isCodeCollapsed ? "w-14 min-w-14" : "w-[25%] min-w-[320px] max-w-[460px]";

  if (isLoadingWorkspace) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-zinc-950 text-zinc-300">
        <Loader2 className="mr-2 size-4 animate-spin" />
        Loading workspace...
      </div>
    );
  }

  if (workspaceError) {
    return (
      <div className="flex min-h-screen flex-col items-center justify-center gap-4 bg-zinc-950 px-4 text-zinc-200">
        <p>{workspaceError}</p>
        <Button asChild className="bg-indigo-500 hover:bg-indigo-400">
          <Link href="/dashboard">Back to dashboard</Link>
        </Button>
      </div>
    );
  }

  return (
    <main className="flex h-screen w-full overflow-hidden bg-zinc-950 text-zinc-100 relative">
      <section className={`flex h-full flex-col border-r border-white/10 bg-zinc-950 transition-all duration-200 ${chatPanelWidth}`}>
        <ChatPanel
          isCollapsed={isChatCollapsed}
          onToggleCollapse={() => setIsChatCollapsed((value) => !value)}
          onOpenSettings={() => setIsSettingsOpen(true)}
          onSendPrompt={onSendPrompt}
        />
        <ProjectSettingsDialog projectId={projectId} open={isSettingsOpen} onOpenChange={setIsSettingsOpen} />
      </section>

      {/* When maximized, the preview section absolutely covers the entire screen */}
      <section className={
        isPreviewMaximized
          ? "fixed inset-0 z-50 flex flex-col bg-zinc-950"
          : "flex h-full min-w-[420px] flex-1 flex-col border-r border-white/10 bg-zinc-900/40"
      }>
        <PreviewPane
          viewport={selectedViewport}
          onViewportChange={setSelectedViewport}
          isMaximized={isPreviewMaximized}
          onToggleMaximize={() => setIsPreviewMaximized((value) => !value)}
          onResetLayout={onResetLayout}
          onRefresh={() => void refreshPreview(undefined, { remount: true })}
        />
      </section>

      <section className={`flex h-full flex-col bg-zinc-950 transition-all duration-200 ${codePanelWidth}`}>
        <CodePanel
          isCollapsed={isCodeCollapsed}
          onToggleCollapse={() => setIsCodeCollapsed((value) => !value)}
          tree={explorerTree}
          expandedFolders={expandedFolders}
          onToggleFolder={(path, isExpanded) => setExpandedFolders((prev) => ({ ...prev, [path]: !isExpanded }))}
          onSelectFile={(path) => {
            setSelectedFilePath(path);
            void loadFileContent(path);
          }}
        />
      </section>
    </main>
  );
}
