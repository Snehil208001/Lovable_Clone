"use client";

import { ChevronDown, ChevronRight, FileCode2, Folder, FolderOpen } from "lucide-react";

import type { ExplorerNode } from "@/lib/workspace/explorer-tree";
import { useWorkspaceStore } from "@/stores/workspace-store";

interface FileTreeExplorerProps {
  tree: ExplorerNode[];
  expandedFolders: Record<string, boolean>;
  onToggleFolder: (path: string, isExpanded: boolean) => void;
  onSelectFile: (path: string) => void;
}

export function FileTreeExplorer({ tree, expandedFolders, onToggleFolder, onSelectFile }: FileTreeExplorerProps) {
  const selectedFilePath = useWorkspaceStore((state) => state.selectedFilePath);

  function renderNodes(nodes: ExplorerNode[], depth = 0): React.ReactNode {
    return nodes.map((node) => {
      if (node.type === "folder") {
        const isExpanded = expandedFolders[node.path] ?? true;
        return (
          <li key={node.path} className="list-none">
            <button
              type="button"
              onClick={() => onToggleFolder(node.path, isExpanded)}
              className="flex w-full items-center gap-2 rounded-lg px-2 py-1.5 text-left text-xs text-zinc-400 hover:bg-zinc-900/60 hover:text-zinc-200 transition-all duration-150 cursor-pointer"
              style={{ paddingLeft: `${8 + depth * 12}px` }}
            >
              {isExpanded ? <ChevronDown className="size-3 text-zinc-500" /> : <ChevronRight className="size-3 text-zinc-500" />}
              {isExpanded ? <FolderOpen className="size-3.5 text-amber-500 fill-amber-500/10 shrink-0" /> : <Folder className="size-3.5 text-amber-500 fill-amber-500/5 shrink-0" />}
              <span className="truncate font-medium">{node.name}</span>
            </button>
            {isExpanded ? <ul className="pl-0 m-0">{renderNodes(node.children || [], depth + 1)}</ul> : null}
          </li>
        );
      }

      const isSelected = selectedFilePath === node.path;
      return (
        <li key={node.path} className="list-none">
          <button
            type="button"
            onClick={() => onSelectFile(node.path)}
            className={`relative flex w-full items-center gap-2 rounded-lg px-2 py-1.5 text-left text-xs transition-all duration-200 cursor-pointer ${
              isSelected 
                ? "bg-indigo-500/10 text-indigo-200 font-semibold border-l-2 border-indigo-500 rounded-l-none" 
                : "text-zinc-400 hover:bg-zinc-900/60 hover:text-zinc-200"
            }`}
            style={{ paddingLeft: `${isSelected ? (24 + depth * 12) : (26 + depth * 12)}px` }}
          >
            <FileCode2 className={`size-3.5 shrink-0 ${isSelected ? "text-indigo-400" : "text-sky-500"}`} />
            <span className="truncate">{node.name}</span>
          </button>
        </li>
      );
    });
  }

  return tree.length ? (
    <ul className="space-y-0.5 pl-0 m-0">{renderNodes(tree)}</ul>
  ) : (
    <p className="text-xs text-zinc-500 italic py-2 text-center">No workspace files found.</p>
  );
}
