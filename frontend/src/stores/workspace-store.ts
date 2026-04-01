"use client";

import { create } from "zustand";

import type { WorkspaceProject } from "@/types/workspace";

interface WorkspaceState {
  activeProject: WorkspaceProject | null;
  fileTree: string[];
  selectedFilePath: string | null;
  setActiveProject: (project: WorkspaceProject | null) => void;
  setFileTree: (fileTree: string[]) => void;
  setSelectedFilePath: (path: string | null) => void;
  resetWorkspace: () => void;
}

const initialState = {
  activeProject: null,
  fileTree: [],
  selectedFilePath: null,
};

export const useWorkspaceStore = create<WorkspaceState>((set) => ({
  ...initialState,
  setActiveProject: (activeProject) => set({ activeProject }),
  setFileTree: (fileTree) => set({ fileTree }),
  setSelectedFilePath: (selectedFilePath) => set({ selectedFilePath }),
  resetWorkspace: () => set(initialState),
}));
