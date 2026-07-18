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
      <header className="flex items-center justify-between border-b border-white/5 px-4 py-3 bg-zinc-950/60 backdrop-blur-md h-14 shrink-0">
        <div className={isCollapsed ? "hidden" : "block"}>
          <p className="text-[9px] uppercase tracking-[0.2em] text-zinc-500">Code</p>
          <h2 className="text-xs font-bold text-zinc-200 mt-0.5">File Explorer</h2>
        </div>
        <div className="flex items-center gap-1.5">
          {!isCollapsed ? <FolderTree className="size-3.5 text-zinc-400" /> : null}
          <Button variant="ghost" size="icon" className="size-8 text-zinc-400 hover:bg-zinc-900 hover:text-zinc-100 rounded-lg" onClick={onToggleCollapse}>
            {isCollapsed ? <PanelRightOpen className="size-4 text-indigo-400" /> : <PanelRightClose className="size-4" />}
          </Button>
        </div>
      </header>

      {!isCollapsed ? (
      <div className="flex flex-col flex-1 min-h-0 overflow-hidden bg-zinc-950/40">
        {/* File tree section */}
        <div className="h-1/2 overflow-y-auto px-4 py-3 border-b border-white/5 scrollbar-thin">
          <FileTreeExplorer tree={tree} expandedFolders={expandedFolders} onToggleFolder={onToggleFolder} onSelectFile={onSelectFile} />
        </div>

        {/* Code viewer section */}
        <div className="relative h-1/2 overflow-hidden bg-zinc-950 flex flex-col">
          <CodeViewer />
        </div>
      </div>
      ) : (
        <div className="flex flex-1 flex-col items-center justify-between py-6">
          <div className="grid size-8 place-items-center rounded-lg border border-indigo-400/20 bg-indigo-500/5 text-indigo-400"><Code2 className="size-4" /></div>
          <Button variant="ghost" size="icon" className="size-8 text-zinc-500 hover:text-zinc-300" onClick={onToggleCollapse}>
            <PanelRightOpen className="size-4" />
          </Button>
        </div>
      )}
    </>
  );
}
