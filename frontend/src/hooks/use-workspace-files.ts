"use client";

import { useCallback, useRef } from "react";

import { ApiClient } from "@/lib/api-client";
import { buildSandpackFiles } from "@/lib/workspace/sandpack";
import { useWorkspaceStore } from "@/stores/workspace-store";

/**
 * File-tree, file-content, and preview loading for one workspace.
 * Owns the per-mount file content cache; all shared state lives in the store.
 */
export function useWorkspaceFiles(projectId: number) {
  const fileContentCacheRef = useRef<Record<string, string>>({});

  const loadFileContent = useCallback(
    async (path: string, force = false) => {
      const {
        setSelectedFileContent,
        setIsLoadingFile,
      } = useWorkspaceStore.getState();

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

      const {
        setFileTree,
        setSelectedFilePath,
        setSelectedFileContent,
        selectedFilePath: currentSelected,
      } = useWorkspaceStore.getState();
      setFileTree(paths);

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
    [loadFileContent, projectId],
  );

  const refreshPreview = useCallback(
    async (pathsOverride?: string[], options?: { remount?: boolean }) => {
      const {
        setPreviewFiles,
        setIsPreviewLoading,
        setPreviewError,
        bumpPreviewRefreshKey,
      } = useWorkspaceStore.getState();

      setIsPreviewLoading(true);
      setPreviewError(null);

      try {
        let paths = pathsOverride ?? useWorkspaceStore.getState().fileTree;
        if (!paths.length) {
          const response = await ApiClient.getFileTree(projectId);
          paths = response.data.map((node) => node.path.replace(/^\//, '')).sort((a, b) => a.localeCompare(b));
        }

        const { files, error } = await buildSandpackFiles(paths, async (path) => {
          const response = await ApiClient.getFile(projectId, path);
          const content = response.data.content ?? "";
          // Keep the code-viewer cache in sync with what the preview renders,
          // otherwise the viewer keeps serving pre-generation content forever.
          fileContentCacheRef.current[path] = content;
          return content;
        });

        const { selectedFilePath, setSelectedFileContent } = useWorkspaceStore.getState();
        if (selectedFilePath && fileContentCacheRef.current[selectedFilePath] !== undefined) {
          setSelectedFileContent(fileContentCacheRef.current[selectedFilePath]);
        }

        setPreviewFiles(files);
        if (error) {
          setPreviewError(error);
          return;
        }
        // Only remount the Sandpack bundler on an explicit refresh; normal updates
        // hot-swap files into the running instance so the canvas doesn't flash.
        if (options?.remount) {
          bumpPreviewRefreshKey();
        }
      } catch {
        setPreviewError("Preview failed to initialize from generated files.");
      } finally {
        setIsPreviewLoading(false);
      }
    },
    [projectId],
  );

  return { loadFileContent, loadFileTree, refreshPreview };
}
