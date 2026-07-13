"use client";

import { Code2, FolderTree, PanelRightClose, PanelRightOpen } from "lucide-react";

import { Button } from "@/components/ui/button";
import { CodeViewer } from "@/components/workspace/code-viewer";
import { FileTreeExplorer } from "@/components/workspace/file-tree-explorer";
import type { ExplorerNode } from "@/lib/workspace/explorer-tree";

interface CodePanelProps {
  isCollapsed: boolean;
  onToggleCollapse: () => void;
  tree: ExplorerNode[];
  expandedFolders: Record<string, boolean>;
  onToggleFolder: (path: string, isExpanded: boolean) => void;
  onSelectFile: (path: string) => void;
}

export function CodePanel({
  isCollapsed,
  onToggleCollapse,
  tree,
  expandedFolders,
  onToggleFolder,
  onSelectFile,
}: CodePanelProps) {
  return (
    <>
      <header className="flex items-center justify-between border-b border-white/10 px-4 py-3">
        <div className={isCollapsed ? "hidden" : "block"}>
          <p className="text-xs uppercase tracking-[0.2em] text-zinc-500">Code</p>
          <h2 className="text-sm font-medium text-zinc-200">File Explorer</h2>
        </div>
        <div className="flex items-center gap-1">
          {!isCollapsed ? <FolderTree className="size-4 text-zinc-500" /> : null}
          <Button variant="ghost" size="icon" className="size-8 text-zinc-400 hover:bg-zinc-900 hover:text-zinc-100" onClick={onToggleCollapse}>
            {isCollapsed ? <PanelRightOpen className="size-4" /> : <PanelRightClose className="size-4" />}
          </Button>
        </div>
      </header>

      {!isCollapsed ? (
      <>
      <div className="h-1/2 overflow-y-auto border-b border-white/10 px-3 py-3">
        <FileTreeExplorer tree={tree} expandedFolders={expandedFolders} onToggleFolder={onToggleFolder} onSelectFile={onSelectFile} />
      </div>

      <div className="relative h-1/2 overflow-auto bg-zinc-950">
        <CodeViewer />
      </div>
      </>
      ) : (
        <div className="flex flex-1 items-start justify-center pt-4"><Code2 className="size-4 text-zinc-500" /></div>
      )}
    </>
  );
}
