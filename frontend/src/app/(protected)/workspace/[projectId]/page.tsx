"use client";

import { FormEvent, useCallback, useEffect, useMemo, useRef, useState } from "react";
import Link from "next/link";
import { useParams } from "next/navigation";
import { AxiosError } from "axios";
import { AnimatePresence, motion } from "framer-motion";
import {
  ChevronDown,
  ChevronRight,
  Code2,
  FileCode2,
  Folder,
  FolderTree,
  Loader2,
  Monitor,
  PanelLeftClose,
  PanelLeftOpen,
  PanelRightClose,
  PanelRightOpen,
  RefreshCw,
  RotateCcw,
  SendHorizontal,
  Settings,
  Smartphone,
  Sparkles,
  Maximize,
  Minimize
} from "lucide-react";
import { Prism as SyntaxHighlighter } from "react-syntax-highlighter";
import { oneDark } from "react-syntax-highlighter/dist/esm/styles/prism";
import {
  SandpackLayout,
  SandpackPreview,
  SandpackProvider,
  type SandpackFiles,
} from "@codesandbox/sandpack-react";

import { Button } from "@/components/ui/button";
import { ApiClient } from "@/lib/api-client";
import { useAuthStore } from "@/stores/auth-store";
import { ProjectSettingsDialog } from "@/components/project-settings-dialog";
import { useWorkspaceStore } from "@/stores/workspace-store";
import type { ChatHistoryMessage, FileContentResponse, FileNode } from "@/types/workspace";

type ChatMessage = {
  id: string;
  role: "USER" | "ASSISTANT";
  content: string;
};

type ExplorerNode = {
  name: string;
  path: string;
  type: "folder" | "file";
  children?: ExplorerNode[];
};

function isRenderableChatRole(role: string): role is "USER" | "ASSISTANT" {
  return role === "USER" || role === "ASSISTANT";
}

function buildExplorerTree(paths: string[]): ExplorerNode[] {
  const root: ExplorerNode = { name: "", path: "", type: "folder", children: [] };

  for (const fullPath of paths) {
    const parts = fullPath.split("/").filter(Boolean);
    let current = root;

    parts.forEach((part, index) => {
      const isFile = index === parts.length - 1;
      const currentPath = parts.slice(0, index + 1).join("/");

      let next = current.children?.find((node) => node.name === part && node.type === (isFile ? "file" : "folder"));
      if (!next) {
        next = {
          name: part,
          path: currentPath,
          type: isFile ? "file" : "folder",
          ...(isFile ? {} : { children: [] }),
        };
        current.children?.push(next);
      }

      if (next.type === "folder") {
        current = next;
      }
    });
  }

  const sortNodes = (nodes: ExplorerNode[]): ExplorerNode[] =>
    nodes
      .map((node) =>
        node.type === "folder"
          ? { ...node, children: sortNodes(node.children || []) }
          : node,
      )
      .sort((a, b) => {
        if (a.type !== b.type) {
          return a.type === "folder" ? -1 : 1;
        }
        return a.name.localeCompare(b.name);
      });

  return sortNodes(root.children || []);
}

function collectFolderPaths(nodes: ExplorerNode[]): string[] {
  const result: string[] = [];
  for (const node of nodes) {
    if (node.type === "folder") {
      result.push(node.path);
      result.push(...collectFolderPaths(node.children || []));
    }
  }
  return result;
}

function languageFromPath(path: string): string {
  const ext = path.split(".").pop()?.toLowerCase();

  switch (ext) {
    case "ts":
      return "typescript";
    case "tsx":
      return "tsx";
    case "js":
      return "javascript";
    case "jsx":
      return "jsx";
    case "java":
      return "java";
    case "json":
      return "json";
    case "css":
      return "css";
    case "html":
      return "html";
    case "yml":
    case "yaml":
      return "yaml";
    case "xml":
      return "xml";
    case "md":
      return "markdown";
    default:
      return "text";
  }
}

function getErrorMessage(error: unknown, fallback: string): string {
  return (error as AxiosError<{ message?: string }>)?.response?.data?.message || fallback;
}

function extractFilePathsFromResponse(responseText: string): string[] {
  const matches = responseText.matchAll(/<file[^>]*path=(["'])([^"']+)\1[^>]*>/gi);
  return Array.from(matches, (match) => (match[2] || "").replace(/^\//, "")).filter(Boolean);
}

function formatChatContent(content: string): string {
  if (!content) return content;
  return content
    .replace(/<file[^>]*path=(["'])([^"']+)\1[^>]*>[\s\S]*?<\/file>/gi, '\n\n📝 Updated $2\n')
    .replace(/<tool[^>]*>[\s\S]*?<\/tool>/gi, '')
    .replace(/<message[^>]*>([\s\S]*?)<\/message>/gi, '\n$1\n')
    .trim();
}

function delay(ms: number): Promise<void> {
  return new Promise((resolve) => {
    setTimeout(resolve, ms);
  });
}

export default function WorkspacePage() {
  const params = useParams<{ projectId: string }>();
  const projectId = Number(params.projectId);
  const chatStorageKey = `workspace:${projectId}:messages`;
  const selectedFileStorageKey = `workspace:${projectId}:selected-file`;
  const panelsStorageKey = `workspace:${projectId}:panels`;
  const foldersStorageKey = `workspace:${projectId}:folders`;
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const persistedMessagesRef = useRef<ChatMessage[] | null>(null);
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [prompt, setPrompt] = useState("");
  const [chatError, setChatError] = useState<string | null>(null);
  const [workspaceError, setWorkspaceError] = useState<string | null>(null);
  const [isLoadingWorkspace, setIsLoadingWorkspace] = useState(true);
  const [isSending, setIsSending] = useState(false);
  const [isLoadingFile, setIsLoadingFile] = useState(false);
  const [selectedViewport, setSelectedViewport] = useState<"desktop" | "mobile">("desktop");
  const [isPreviewMaximized, setIsPreviewMaximized] = useState(false);
  const [previewRefreshKey, setPreviewRefreshKey] = useState(0);
  const [previewFiles, setPreviewFiles] = useState<SandpackFiles>({});
  const [isPreviewLoading, setIsPreviewLoading] = useState(true);
  const [previewError, setPreviewError] = useState<string | null>(null);
  const [selectedFileContent, setSelectedFileContent] = useState("");
  const [isChatCollapsed, setIsChatCollapsed] = useState(false);
  const [isCodeCollapsed, setIsCodeCollapsed] = useState(false);
  const [isSettingsOpen, setIsSettingsOpen] = useState(false);
  const [expandedFolders, setExpandedFolders] = useState<Record<string, boolean>>({});
  const fileContentCacheRef = useRef<Record<string, string>>({});
  const token = useAuthStore((state) => state.token);
  const activeProject = useWorkspaceStore((state) => state.activeProject);
  const fileTree = useWorkspaceStore((state) => state.fileTree);
  const selectedFilePath = useWorkspaceStore((state) => state.selectedFilePath);
  const setActiveProject = useWorkspaceStore((state) => state.setActiveProject);
  const setFileTree = useWorkspaceStore((state) => state.setFileTree);
  const setSelectedFilePath = useWorkspaceStore((state) => state.setSelectedFilePath);
  const resetWorkspace = useWorkspaceStore((state) => state.resetWorkspace);

  const explorerTree = useMemo(() => buildExplorerTree(fileTree), [fileTree]);

  useEffect(() => {
    return () => {
      resetWorkspace();
    };
  }, [resetWorkspace]);

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
  }, [chatStorageKey, foldersStorageKey, panelsStorageKey, selectedFileStorageKey, setSelectedFilePath]);

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

  useEffect(() => {
    const folderPaths = collectFolderPaths(explorerTree);
    setExpandedFolders((previous) => {
      const next = { ...previous };
      folderPaths.forEach((folderPath) => {
        if (typeof next[folderPath] === "undefined") next[folderPath] = true;
      });
      return next;
    });
  }, [explorerTree]);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages]);

  const loadFileContent = useCallback(
    async (path: string, force = false) => {
      if (!force && fileContentCacheRef.current[path]) {
        setSelectedFileContent(fileContentCacheRef.current[path]);
        return;
      }

      setIsLoadingFile(true);
      try {
        const response = await ApiClient.getFile(projectId, path);
        const content = response.data.content ?? "";
        fileContentCacheRef.current[path] = content;
        setSelectedFileContent(content);
      } catch {
        setSelectedFileContent("");
      } finally {
        setIsLoadingFile(false);
      }
    },
    [projectId],
  );

  const loadFileTree = useCallback(
    async (selectFirstFile = false): Promise<string[]> => {
      const response = await ApiClient.getFileTree(projectId);
      const paths = response.data.map((node) => node.path.replace(/^\//, '')).sort((a, b) => a.localeCompare(b));
      setFileTree(paths);

      const currentSelected = useWorkspaceStore.getState().selectedFilePath;
      const defaultSelected = paths.find(p => !p.includes('/')) || paths[0] || null;
      const preferredSelected = currentSelected && paths.includes(currentSelected) ? currentSelected : defaultSelected;

      if (preferredSelected) {
        setSelectedFilePath(preferredSelected);
        await loadFileContent(preferredSelected, selectFirstFile);
      } else {
        setSelectedFilePath(null);
        setSelectedFileContent("");
      }

      return paths;
    },
    [loadFileContent, projectId, setFileTree, setSelectedFilePath],
  );

  const refreshPreview = useCallback(
    async (pathsOverride?: string[]) => {
      setIsPreviewLoading(true);
      setPreviewError(null);

      try {
        let paths = pathsOverride ?? useWorkspaceStore.getState().fileTree;
        if (!paths.length) {
          const response = await ApiClient.getFileTree(projectId);
          paths = response.data.map((node) => node.path.replace(/^\//, '')).sort((a, b) => a.localeCompare(b));
        }

        const filePathsOnly = paths.filter(p => !paths.some(other => other !== p && other.startsWith(p + '/')));

        if (!filePathsOnly.length) {
          setPreviewFiles({});
          setPreviewError("No files available yet to render a preview.");
          return;
        }

        const fileEntries = await Promise.allSettled(
          filePathsOnly.map(async (path) => {
            const response = await ApiClient.getFile(projectId, path);
            return [path, response.data.content ?? ""] as const;
          }),
        );

        const nextPreviewFiles: SandpackFiles = {};
        fileEntries.forEach((entry) => {
          if (entry.status === "fulfilled") {
            let [path, content] = entry.value;
            const normalizedPath = path.startsWith("/") ? path : `/${path}`;

            // Strip out configs that crash the browser Webpack bundler
            if (
              normalizedPath.includes("postcss.config") ||
              normalizedPath.includes("tailwind.config") ||
              normalizedPath.includes("vite.config")
            ) {
              return;
            }

            // Prevent CSS loaders from trying to resolve Node.js tailwind modules
            if (normalizedPath.endsWith(".css")) {
              content = content
                .replace(/@import\s+['"]tailwindcss.*?['"];?/g, "/* tailwind import removed for CDN */")
                .replace(/@tailwind\s+base;?/g, "")
                .replace(/@tailwind\s+components;?/g, "")
                .replace(/@tailwind\s+utilities;?/g, "");
            }

            nextPreviewFiles[normalizedPath] = { code: content };
          }
        });

        if (!Object.keys(nextPreviewFiles).length) {
          setPreviewFiles({});
          setPreviewError("Preview could not load file contents.");
          return;
        }

        // Remove generated index.html because react-ts template strictly uses /public/index.html
        if (nextPreviewFiles["/index.html"]) {
          delete nextPreviewFiles["/index.html"];
        }

        // Inject the Tailwind Play CDN into the specific public/index.html required by react-ts
        nextPreviewFiles["/public/index.html"] = {
          code: `<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="utf-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1" />
    <title>Preview</title>
    <script src="https://cdn.tailwindcss.com"></script>
    <link href="https://cdn.jsdelivr.net/npm/daisyui@4.7.2/dist/full.min.css" rel="stylesheet" type="text/css" />
    <script>
      tailwind.config = {
        theme: { extend: {} },
        // daisyui is handled by the linked CSS file above
      }
    </script>
  </head>
  <body>
    <noscript>You need to enable JavaScript to run this app.</noscript>
    <div id="root"></div>
  </body>
</html>`,
        };

        if (!nextPreviewFiles["/src/main.tsx"] && !nextPreviewFiles["/src/index.tsx"]) {
          const possibleAppPaths = ["/src/App.tsx", "/src/App.jsx", "/src/Index.tsx", "/src/Index.jsx"];
          const existingAppPath = possibleAppPaths.find(p => nextPreviewFiles[p]);
          let importPath = "./App";
          let appComponent = "<App />";

          if (existingAppPath) {
             importPath = existingAppPath.replace("/src/", "./").replace(".tsx", "").replace(".jsx", "");
          } else {
             appComponent = `<div><h2>No App Component Found</h2></div>`;
          }

          nextPreviewFiles["/src/main.tsx"] = {
            code: `import React from "react";
import ReactDOM from "react-dom/client";
${existingAppPath ? `import App from "${importPath}";` : ""}

ReactDOM.createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    ${appComponent}
  </React.StrictMode>
);`,
          };
        }

        // Package.json explicitly strips Node.js tailwind/postcss tools
        if (!nextPreviewFiles["/package.json"]) {
          nextPreviewFiles["/package.json"] = {
            code: JSON.stringify(
              {
                name: "generated-preview",
                main: "/src/main.tsx",
                private: true,
                version: "0.0.0",
                dependencies: {
                  "react": "^18.2.0",
                  "react-dom": "^18.2.0",
                  "react-scripts": "^5.0.1",
                  "react-router-dom": "^6.22.0",
                  "@tanstack/react-query": "^5.0.0",
                  "lucide-react": "^0.300.0",
                  "clsx": "^2.1.0",
                  "tailwind-merge": "^2.2.1"
                },
                scripts: {
                  "start": "react-scripts start",
                  "build": "react-scripts build",
                  "test": "react-scripts test",
                  "eject": "react-scripts eject"
                }
              },
              null,
              2,
            ),
          };
        }

        setPreviewFiles(nextPreviewFiles);
        setPreviewRefreshKey((value) => value + 1);
      } catch {
        setPreviewError("Preview failed to initialize from generated files.");
      } finally {
        setIsPreviewLoading(false);
      }
    },
    [projectId],
  );

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
  }, [loadFileTree, projectId, refreshPreview, setActiveProject]);

  async function streamAssistantResponse(userPrompt: string, assistantMessageId: string): Promise<string> {
    const response = await fetch("http://localhost:8080/api/chat/stream", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      body: JSON.stringify({ message: userPrompt, projectId }),
    });

    if (!response.ok || !response.body) throw new Error("Streaming request failed");

    const reader = response.body.getReader();
    const decoder = new TextDecoder("utf-8");
    let buffer = "";
    let fullAssistantResponse = "";

    while (true) {
      const { done, value } = await reader.read();
      if (done) break;

      buffer += decoder.decode(value, { stream: true });
      const events = buffer.split("\n\n");
      buffer = events.pop() || "";

      for (const event of events) {
        const lines = event.split("\n").map((line) => line.trim()).filter((line) => line.startsWith("data:"));
        if (!lines.length) continue;

        const chunk = lines.map((line) => line.startsWith("data: ") ? line.slice(6) : line.slice(5)).join("\n");
        fullAssistantResponse += chunk;
        setMessages((previous) =>
          previous.map((message) =>
            message.id === assistantMessageId
              ? { ...message, content: `${message.content}${chunk}` }
              : message,
          ),
        );
      }
    }

    return fullAssistantResponse;
  }

  async function onSendPrompt(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    const trimmedPrompt = prompt.trim();
    if (!trimmedPrompt || isSending) return;

    setChatError(null);
    setIsSending(true);
    setPrompt("");

    const userMessageId = `user-${Date.now()}`;
    const assistantMessageId = `assistant-${Date.now()}`;

    setMessages((previous) => [
      ...previous,
      { id: userMessageId, role: "USER", content: trimmedPrompt },
      { id: assistantMessageId, role: "ASSISTANT", content: "" },
    ]);

    // Automatically minimize preview if you send a new prompt
    if (isPreviewMaximized) {
      setIsPreviewMaximized(false);
    }

    try {
      const filesBefore = new Set(fileTree);
      const assistantResponse = await streamAssistantResponse(trimmedPrompt, assistantMessageId);
      const expectedFilePaths = extractFilePathsFromResponse(assistantResponse);

      let latestPaths = await loadFileTree(false);
      const maxSyncAttempts = 8;

      for (let attempt = 0; attempt < maxSyncAttempts; attempt += 1) {
        const hasExpectedFiles = expectedFilePaths.length > 0 && expectedFilePaths.every((path) => latestPaths.includes(path));
        if (hasExpectedFiles) break;
        await delay(800);
        latestPaths = await loadFileTree(false);
      }

      await refreshPreview(latestPaths);
    } catch {
      setChatError("Streaming failed. Please try again.");
      setMessages((previous) =>
        previous.map((message) =>
          message.id === assistantMessageId
            ? { ...message, content: message.content || "I hit an error while generating a response." }
            : message,
        ),
      );
    } finally {
      setIsSending(false);
    }
  }

  const previewWidthClass = selectedViewport === "desktop" ? "w-full" : "mx-auto w-[390px]";
  const selectedLanguage = useMemo(() => languageFromPath(selectedFilePath || ""), [selectedFilePath]);
  const chatPanelWidth = isChatCollapsed ? "w-14 min-w-14" : "w-[25%] min-w-[300px] max-w-[420px]";
  const codePanelWidth = isCodeCollapsed ? "w-14 min-w-14" : "w-[25%] min-w-[320px] max-w-[460px]";

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

  const getActiveFileForSandpack = () => {
    if (selectedFilePath) {
      const normalized = selectedFilePath.startsWith("/") ? selectedFilePath : `/${selectedFilePath}`;
      if (previewFiles[normalized]) return normalized;
    }
    if (previewFiles["/src/App.tsx"]) return "/src/App.tsx";
    if (previewFiles["/src/main.tsx"]) return "/src/main.tsx";
    return "/public/index.html";
  };

  function renderExplorerNodes(nodes: ExplorerNode[], depth = 0): React.ReactNode {
    return nodes.map((node) => {
      if (node.type === "folder") {
        const isExpanded = expandedFolders[node.path] ?? true;
        return (
          <li key={node.path}>
            <button
              type="button"
              onClick={() => setExpandedFolders((prev) => ({ ...prev, [node.path]: !isExpanded }))}
              className="flex w-full items-center gap-1 rounded-md px-2 py-1.5 text-left text-xs text-zinc-300 transition hover:bg-zinc-900 hover:text-zinc-100"
              style={{ paddingLeft: `${8 + depth * 14}px` }}
            >
              {isExpanded ? <ChevronDown className="size-3 text-zinc-500" /> : <ChevronRight className="size-3 text-zinc-500" />}
              <Folder className="size-3.5 text-zinc-500" />
              <span className="truncate">{node.name}</span>
            </button>
            {isExpanded ? <ul>{renderExplorerNodes(node.children || [], depth + 1)}</ul> : null}
          </li>
        );
      }

      return (
        <li key={node.path}>
          <button
            type="button"
            onClick={() => {
              setSelectedFilePath(node.path);
              void loadFileContent(node.path);
            }}
            className={`flex w-full items-center gap-1 rounded-md px-2 py-1.5 text-left text-xs transition ${
              selectedFilePath === node.path ? "bg-indigo-500/20 text-indigo-100" : "text-zinc-300 hover:bg-zinc-900 hover:text-zinc-100"
            }`}
            style={{ paddingLeft: `${26 + depth * 14}px` }}
          >
            <FileCode2 className="size-3.5 shrink-0 text-zinc-500" />
            <span className="truncate">{node.name}</span>
          </button>
        </li>
      );
    });
  }

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
        <header className="border-b border-white/10 px-4 py-3">
          <div className="flex items-center justify-between">
            <Link href="/dashboard" className={`group ${isChatCollapsed ? "hidden" : "block"}`}>
              <p className="text-xs uppercase tracking-[0.2em] text-zinc-500 transition-colors group-hover:text-zinc-400">← Dashboard</p>
              <h2 className="truncate text-sm font-medium text-zinc-200">{activeProject?.name}</h2>
            </Link>
            <div className="flex items-center gap-1">
              <Button variant="ghost" size="icon" className={`size-8 text-zinc-400 hover:bg-zinc-900 hover:text-zinc-100 ${isChatCollapsed ? "hidden" : "block"}`} onClick={() => setIsSettingsOpen(true)}>
                <Settings className="size-4" />
              </Button>
              <Button variant="ghost" size="icon" className="size-8 text-zinc-400 hover:bg-zinc-900 hover:text-zinc-100" onClick={() => setIsChatCollapsed((value) => !value)}>
                {isChatCollapsed ? <PanelLeftOpen className="size-4" /> : <PanelLeftClose className="size-4" />}
              </Button>
            </div>
          </div>
        </header>

        <ProjectSettingsDialog projectId={projectId} open={isSettingsOpen} onOpenChange={setIsSettingsOpen} />

        {!isChatCollapsed ? (
        <>
        <div className="flex-1 space-y-3 overflow-y-auto px-3 py-4">
          <AnimatePresence initial={false}>
            {messages.map((message) => {
              const isUser = message.role === "USER";
              return (
                <motion.div
                  key={message.id}
                  initial={{ opacity: 0, y: 6 }}
                  animate={{ opacity: 1, y: 0 }}
                  exit={{ opacity: 0, y: -4 }}
                  transition={{ duration: 0.16 }}
                  className={`rounded-xl border px-3 py-2 text-sm leading-6 ${isUser ? "ml-6 border-indigo-300/30 bg-indigo-500/15 text-indigo-50" : "mr-6 border-white/10 bg-zinc-900/80 text-zinc-200"}`}
                >
                  <p className="mb-1 text-[10px] uppercase tracking-[0.2em] text-zinc-400">{isUser ? "You" : "Assistant"}</p>
                  <p className="whitespace-pre-wrap">{formatChatContent(message.content) || (isSending && !isUser ? "Thinking..." : "")}</p>
                </motion.div>
              );
            })}
          </AnimatePresence>
          <div ref={messagesEndRef} />
        </div>

        <div className="border-t border-white/10 p-3">
          <form onSubmit={onSendPrompt} className="space-y-2">
            <textarea
              value={prompt}
              onChange={(event) => setPrompt(event.target.value)}
              placeholder="Describe what you want to build..."
              className="h-24 w-full resize-none rounded-lg border border-white/10 bg-zinc-900/80 p-3 text-sm text-zinc-100 outline-none transition focus:border-indigo-300/50"
            />
            {chatError ? <p className="text-xs text-red-300">{chatError}</p> : null}
            <Button type="submit" className="w-full bg-indigo-500 text-white hover:bg-indigo-400" disabled={isSending}>
              {isSending ? <><Loader2 className="mr-2 size-4 animate-spin" />Streaming...</> : <><Sparkles className="mr-2 size-4" />Send<SendHorizontal className="ml-2 size-4" /></>}
            </Button>
          </form>
        </div>
        </>
        ) : (
          <div className="flex flex-1 items-start justify-center pt-4">
            <Link href="/dashboard" className="text-zinc-500 transition-colors hover:text-zinc-300"><Sparkles className="size-4" /></Link>
          </div>
        )}
      </section>

      {/* FULLSCREEN FIX APPLIED HERE: When isPreviewMaximized is true, this section absolutely covers the entire screen */}
      <section className={
        isPreviewMaximized
          ? "fixed inset-0 z-50 flex flex-col bg-zinc-950"
          : "flex h-full min-w-[420px] flex-1 flex-col border-r border-white/10 bg-zinc-900/40"
      }>
        <header className="flex items-center justify-between border-b border-white/10 px-4 py-3 bg-zinc-950/80 backdrop-blur-md">
          <div>
            <p className="text-xs uppercase tracking-[0.2em] text-zinc-500">Preview</p>
            <h2 className="text-sm font-medium text-zinc-200">UI Canvas</h2>
          </div>
          <div className="flex items-center gap-2">
            <div className="rounded-lg border border-white/10 bg-zinc-900 p-1">
              <Button variant={selectedViewport === "desktop" ? "secondary" : "ghost"} size="icon" className="size-8" onClick={() => setSelectedViewport("desktop")}><Monitor className="size-4" /></Button>
              <Button variant={selectedViewport === "mobile" ? "secondary" : "ghost"} size="icon" className="size-8" onClick={() => setSelectedViewport("mobile")}><Smartphone className="size-4" /></Button>
            </div>
            <Button variant="outline" size="sm" onClick={onResetLayout} className="border-white/10 bg-zinc-900 hover:bg-zinc-800"><RotateCcw className="mr-2 size-4" />Reset Layout</Button>
            <Button variant="outline" size="sm" onClick={() => void refreshPreview()} className="border-white/10 bg-zinc-900 hover:bg-zinc-800" disabled={isPreviewLoading}><RefreshCw className="mr-2 size-4" />{isPreviewLoading ? "Refreshing..." : "Refresh"}</Button>

            {/* FULLSCREEN TOGGLE BUTTON */}
            <Button variant="outline" size="icon" className="size-8 border-white/10 bg-zinc-900 hover:bg-zinc-800" onClick={() => setIsPreviewMaximized(!isPreviewMaximized)}>
              {isPreviewMaximized ? <Minimize className="size-4 text-indigo-400" /> : <Maximize className="size-4 text-zinc-400" />}
            </Button>
          </div>
        </header>

        <div className="flex-1 overflow-auto p-2 relative">
          {isPreviewLoading ? (
            <div className="flex h-full items-center justify-center rounded-xl border border-white/10 bg-zinc-900/70">
              <div className="flex items-center text-sm text-zinc-300"><Loader2 className="mr-2 size-4 animate-spin" />Building preview...</div>
            </div>
          ) : previewError ? (
            <div className="flex h-full flex-col items-center justify-center rounded-xl border border-red-400/20 bg-red-500/5 px-6 text-center">
              <p className="text-sm text-red-200">{previewError}</p>
              <Button variant="outline" size="sm" className="mt-3 border-white/10 bg-zinc-900 hover:bg-zinc-800" onClick={() => void refreshPreview()}>Retry Preview</Button>
            </div>
          ) : (
            <motion.div
              key={previewRefreshKey}
              initial={{ opacity: 0.75, scale: 0.99 }}
              animate={{ opacity: 1, scale: 1 }}
              transition={{ duration: 0.2 }}
              className={`${previewWidthClass} h-full min-h-[800px] rounded-xl border border-white/10 bg-zinc-900/70 p-2 transition-all`}
            >
              <SandpackProvider
                key={`sandpack-${previewRefreshKey}`}
                template="react-ts"
                theme="dark"
                files={previewFiles}
                customSetup={{
                  dependencies: {
                    "react": "^18.2.0",
                    "react-dom": "^18.2.0",
                    "react-scripts": "^5.0.1",
                    "lucide-react": "^0.300.0",
                    "react-router-dom": "^6.22.0",
                    "@tanstack/react-query": "^5.0.0",
                    "clsx": "^2.1.0",
                    "tailwind-merge": "^2.2.1"
                  },
                  entry: "/src/main.tsx"
                }}
                options={{
                  activeFile: getActiveFileForSandpack()
                }}
              >
                <SandpackLayout style={{ height: "100%", minHeight: "800px", border: "none", background: "transparent" }}>
                  <SandpackPreview showOpenInCodeSandbox={false} showRefreshButton showNavigator={true} style={{ height: "100%", minHeight: "800px" }} />
                </SandpackLayout>
              </SandpackProvider>
            </motion.div>
          )}
        </div>
      </section>

      <section className={`flex h-full flex-col bg-zinc-950 transition-all duration-200 ${codePanelWidth}`}>
        <header className="flex items-center justify-between border-b border-white/10 px-4 py-3">
          <div className={isCodeCollapsed ? "hidden" : "block"}>
            <p className="text-xs uppercase tracking-[0.2em] text-zinc-500">Code</p>
            <h2 className="text-sm font-medium text-zinc-200">File Explorer</h2>
          </div>
          <div className="flex items-center gap-1">
            {!isCodeCollapsed ? <FolderTree className="size-4 text-zinc-500" /> : null}
            <Button variant="ghost" size="icon" className="size-8 text-zinc-400 hover:bg-zinc-900 hover:text-zinc-100" onClick={() => setIsCodeCollapsed((value) => !value)}>
              {isCodeCollapsed ? <PanelRightOpen className="size-4" /> : <PanelRightClose className="size-4" />}
            </Button>
          </div>
        </header>

        {!isCodeCollapsed ? (
        <>
        <div className="h-1/2 overflow-y-auto border-b border-white/10 px-3 py-3">
          {fileTree.length ? <ul className="space-y-0.5">{renderExplorerNodes(explorerTree)}</ul> : <p className="text-xs text-zinc-500">No generated files yet.</p>}
        </div>

        <div className="relative h-1/2 overflow-auto bg-zinc-950">
          {isLoadingFile ? (
            <div className="flex h-full items-center justify-center text-zinc-400"><Loader2 className="mr-2 size-4 animate-spin" />Loading file...</div>
          ) : selectedFilePath ? (
            <>
              <div className="sticky top-0 flex items-center justify-between border-b border-white/10 bg-zinc-950/95 px-3 py-2 backdrop-blur">
                <p className="truncate text-xs text-zinc-300">{selectedFilePath}</p>
                <Code2 className="size-4 text-zinc-500" />
              </div>
              <SyntaxHighlighter language={selectedLanguage} style={oneDark} customStyle={{ margin: 0, padding: "14px", background: "transparent", fontSize: "12px", minHeight: "100%" }} wrapLongLines>
                {selectedFileContent || "// Empty file"}
              </SyntaxHighlighter>
            </>
          ) : (
            <div className="flex h-full items-center justify-center px-6 text-center text-xs text-zinc-500">Select a file from the tree to inspect generated code.</div>
          )}
        </div>
        </>
        ) : (
          <div className="flex flex-1 items-start justify-center pt-4"><Code2 className="size-4 text-zinc-500" /></div>
        )}
      </section>
    </main>
  );
}