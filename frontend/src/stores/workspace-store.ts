"use client";

import { create } from "zustand";
import type { SandpackFiles } from "@codesandbox/sandpack-react";

import type { ChatMessage, WorkspaceProject } from "@/types/workspace";

interface WorkspaceState {
  activeProject: WorkspaceProject | null;
  fileTree: string[];
  selectedFilePath: string | null;
  // chat
  messages: ChatMessage[];
  isSending: boolean;
  chatError: string | null;
  // code viewer
  selectedFileContent: string;
  isLoadingFile: boolean;
  // preview
  previewFiles: SandpackFiles;
  isPreviewLoading: boolean;
  previewError: string | null;
  previewRefreshKey: number;
  setActiveProject: (project: WorkspaceProject | null) => void;
  setFileTree: (fileTree: string[]) => void;
  setSelectedFilePath: (path: string | null) => void;
  setMessages: (messages: ChatMessage[]) => void;
  appendMessages: (messages: ChatMessage[]) => void;
  appendToMessage: (id: string, chunk: string) => void;
  fillEmptyMessage: (id: string, fallback: string) => void;
  setIsSending: (isSending: boolean) => void;
  setChatError: (error: string | null) => void;
  setSelectedFileContent: (content: string) => void;
  setIsLoadingFile: (isLoading: boolean) => void;
  setPreviewFiles: (files: SandpackFiles) => void;
  setIsPreviewLoading: (isLoading: boolean) => void;
  setPreviewError: (error: string | null) => void;
  bumpPreviewRefreshKey: () => void;
  resetWorkspace: () => void;
}

const initialState = {
  activeProject: null,
  fileTree: [],
  selectedFilePath: null,
  messages: [],
  isSending: false,
  chatError: null,
  selectedFileContent: "",
  isLoadingFile: false,
  previewFiles: {},
  isPreviewLoading: true,
  previewError: null,
  previewRefreshKey: 0,
};

export const useWorkspaceStore = create<WorkspaceState>((set) => ({
  ...initialState,
  setActiveProject: (activeProject) => set({ activeProject }),
  setFileTree: (fileTree) => set({ fileTree }),
  setSelectedFilePath: (selectedFilePath) => set({ selectedFilePath }),
  setMessages: (messages) => set({ messages }),
  appendMessages: (newMessages) => set((state) => ({ messages: [...state.messages, ...newMessages] })),
  appendToMessage: (id, chunk) =>
    set((state) => ({
      messages: state.messages.map((message) =>
        message.id === id ? { ...message, content: `${message.content}${chunk}` } : message,
      ),
    })),
  fillEmptyMessage: (id, fallback) =>
    set((state) => ({
      messages: state.messages.map((message) =>
        message.id === id ? { ...message, content: message.content || fallback } : message,
      ),
    })),
  setIsSending: (isSending) => set({ isSending }),
  setChatError: (chatError) => set({ chatError }),
  setSelectedFileContent: (selectedFileContent) => set({ selectedFileContent }),
  setIsLoadingFile: (isLoadingFile) => set({ isLoadingFile }),
  setPreviewFiles: (previewFiles) => set({ previewFiles }),
  setIsPreviewLoading: (isPreviewLoading) => set({ isPreviewLoading }),
  setPreviewError: (previewError) => set({ previewError }),
  bumpPreviewRefreshKey: () => set((state) => ({ previewRefreshKey: state.previewRefreshKey + 1 })),
  resetWorkspace: () => set(initialState),
}));
