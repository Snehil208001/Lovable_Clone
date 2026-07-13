"use client";

import { useMemo } from "react";
import { Code2, Loader2 } from "lucide-react";
import { Prism as SyntaxHighlighter } from "react-syntax-highlighter";
import { oneDark } from "react-syntax-highlighter/dist/esm/styles/prism";

import { languageFromPath } from "@/lib/workspace/explorer-tree";
import { useWorkspaceStore } from "@/stores/workspace-store";

export function CodeViewer() {
  const selectedFilePath = useWorkspaceStore((state) => state.selectedFilePath);
  const selectedFileContent = useWorkspaceStore((state) => state.selectedFileContent);
  const isLoadingFile = useWorkspaceStore((state) => state.isLoadingFile);

  const selectedLanguage = useMemo(() => languageFromPath(selectedFilePath || ""), [selectedFilePath]);

  if (isLoadingFile) {
    return (
      <div className="flex h-full items-center justify-center text-zinc-400"><Loader2 className="mr-2 size-4 animate-spin" />Loading file...</div>
    );
  }

  if (!selectedFilePath) {
    return (
      <div className="flex h-full items-center justify-center px-6 text-center text-xs text-zinc-500">Select a file from the tree to inspect generated code.</div>
    );
  }

  return (
    <>
      <div className="sticky top-0 flex items-center justify-between border-b border-white/10 bg-zinc-950/95 px-3 py-2 backdrop-blur">
        <p className="truncate text-xs text-zinc-300">{selectedFilePath}</p>
        <Code2 className="size-4 text-zinc-500" />
      </div>
      <SyntaxHighlighter language={selectedLanguage} style={oneDark} customStyle={{ margin: 0, padding: "14px", background: "transparent", fontSize: "12px", minHeight: "100%" }} wrapLongLines>
        {selectedFileContent || "// Empty file"}
      </SyntaxHighlighter>
    </>
  );
}
