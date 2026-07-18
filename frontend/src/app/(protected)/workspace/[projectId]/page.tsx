"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import Link from "next/link";
import { useParams } from "next/navigation";
import { Loader2 } from "lucide-react";

import { Button } from "@/components/ui/button";
import { ResizableHandle, ResizablePanel, ResizablePanelGroup } from "@/components/ui/resizable";
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
  const { sendPrompt, stopGeneration } = useWorkspaceChat(projectId, { loadFileTree, refreshPreview });
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
        const parsed = JSON.parse(rawPanels) as { isChatCollapsed?: boolean; isCodeCollapsed?: boolean };
        if (typeof parsed.isChatCollapsed === "boolean") setIsChatCollapsed(parsed.isChatCollapsed);
        if (typeof parsed.isCodeCollapsed === "boolean") setIsCodeCollapsed(parsed.isCodeCollapsed);
      } catch {
        /* ignore */
      }
    }

    if (rawFolders) {
      try {
        const parsed = JSON.parse(rawFolders) as Record<string, boolean>;
        if (parsed && typeof parsed === "object") setExpandedFolders(parsed);
      } catch {
        /* ignore */
      }
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
    if (isPreviewMaximized) setIsPreviewMaximized(false);
    void sendPrompt(trimmedPrompt);
  }

  function onResetLayout() {
    if (typeof window !== "undefined") {
      window.localStorage.removeItem(panelsStorageKey);
      window.localStorage.removeItem(foldersStorageKey);
      window.localStorage.removeItem(selectedFileStorageKey);
      window.localStorage.removeItem(`workspace:${projectId}:layout`);
    }

    setIsChatCollapsed(false);
    setIsCodeCollapsed(false);
    setIsPreviewMaximized(false);
    const allFolders = collectFolderPaths(explorerTree);
    setExpandedFolders(Object.fromEntries(allFolders.map((path) => [path, true])));

    const defaultFile = fileTree.find((p) => !p.includes("/")) || fileTree[0] || null;
    setSelectedFilePath(defaultFile);
    if (defaultFile) {
      void loadFileContent(defaultFile, true);
    } else {
      setSelectedFileContent("");
    }
  }

  if (isLoadingWorkspace) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-background text-muted-foreground">
        <Loader2 className="mr-2 size-4 animate-spin text-primary" />
        Loading workspace...
      </div>
    );
  }

  if (workspaceError) {
    return (
      <div className="flex min-h-screen flex-col items-center justify-center gap-4 bg-background px-4 text-foreground">
        <p className="text-sm text-muted-foreground">{workspaceError}</p>
        <Button asChild>
          <Link href="/dashboard">Back to dashboard</Link>
        </Button>
      </div>
    );
  }

  if (isPreviewMaximized) {
    return (
      <main className="fixed inset-0 z-50 flex flex-col bg-background">
        <PreviewPane
          viewport={selectedViewport}
          onViewportChange={setSelectedViewport}
          isMaximized
          onToggleMaximize={() => setIsPreviewMaximized(false)}
          onResetLayout={onResetLayout}
          onRefresh={() => void refreshPreview(undefined, { remount: true })}
        />
      </main>
    );
  }

  return (
    <main className="relative flex h-screen w-full overflow-hidden bg-background text-foreground">
      <div className="pointer-events-none absolute inset-0 z-0 bg-grid-pattern opacity-40" />

      <ResizablePanelGroup
        orientation="horizontal"
        className="relative z-10 h-full"
        id={`workspace-${projectId}-layout`}
      >
        <ResizablePanel
          id="chat"
          defaultSize={isChatCollapsed ? "4%" : "24%"}
          minSize={isChatCollapsed ? "4%" : "18%"}
          maxSize={isChatCollapsed ? "6%" : "36%"}
          className="flex min-w-0 flex-col bg-background"
        >
          <ChatPanel
            isCollapsed={isChatCollapsed}
            onToggleCollapse={() => setIsChatCollapsed((value) => !value)}
            onOpenSettings={() => setIsSettingsOpen(true)}
            onSendPrompt={onSendPrompt}
            onStopGeneration={stopGeneration}
          />
          <ProjectSettingsDialog projectId={projectId} open={isSettingsOpen} onOpenChange={setIsSettingsOpen} />
        </ResizablePanel>

        {!isChatCollapsed ? <ResizableHandle withHandle className="bg-border/60" /> : null}

        <ResizablePanel id="preview" defaultSize="48%" minSize="30%" className="flex min-w-0 flex-col bg-secondary/20">
          <PreviewPane
            viewport={selectedViewport}
            onViewportChange={setSelectedViewport}
            isMaximized={false}
            onToggleMaximize={() => setIsPreviewMaximized(true)}
            onResetLayout={onResetLayout}
            onRefresh={() => void refreshPreview(undefined, { remount: true })}
          />
        </ResizablePanel>

        {!isCodeCollapsed ? <ResizableHandle withHandle className="bg-border/60" /> : null}

        <ResizablePanel
          id="code"
          defaultSize={isCodeCollapsed ? "4%" : "28%"}
          minSize={isCodeCollapsed ? "4%" : "18%"}
          maxSize={isCodeCollapsed ? "6%" : "40%"}
          className="flex min-w-0 flex-col bg-background"
        >
          <CodePanel
            isCollapsed={isCodeCollapsed}
            onToggleCollapse={() => setIsCodeCollapsed((value) => !value)}
            tree={explorerTree}
            expandedFolders={expandedFolders}
            onToggleFolder={(path, isExpanded) =>
              setExpandedFolders((prev) => ({ ...prev, [path]: !isExpanded }))
            }
            onSelectFile={(path) => {
              setSelectedFilePath(path);
              void loadFileContent(path);
            }}
          />
        </ResizablePanel>
      </ResizablePanelGroup>
    </main>
  );
}
