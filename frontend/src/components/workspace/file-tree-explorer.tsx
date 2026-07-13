"use client";

import { ChevronDown, ChevronRight, FileCode2, Folder } from "lucide-react";

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
          <li key={node.path}>
            <button
              type="button"
              onClick={() => onToggleFolder(node.path, isExpanded)}
              className="flex w-full items-center gap-1 rounded-md px-2 py-1.5 text-left text-xs text-zinc-300 transition hover:bg-zinc-900 hover:text-zinc-100"
              style={{ paddingLeft: `${8 + depth * 14}px` }}
            >
              {isExpanded ? <ChevronDown className="size-3 text-zinc-500" /> : <ChevronRight className="size-3 text-zinc-500" />}
              <Folder className="size-3.5 text-zinc-500" />
              <span className="truncate">{node.name}</span>
            </button>
            {isExpanded ? <ul>{renderNodes(node.children || [], depth + 1)}</ul> : null}
          </li>
        );
      }

      return (
        <li key={node.path}>
          <button
            type="button"
            onClick={() => onSelectFile(node.path)}
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

  return tree.length ? (
    <ul className="space-y-0.5">{renderNodes(tree)}</ul>
  ) : (
    <p className="text-xs text-zinc-500">No generated files yet.</p>
  );
}
