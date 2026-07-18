"use client";

import { useMemo, useState } from "react";
import { Code2, Loader2, ClipboardCopy, Check } from "lucide-react";
import { Prism as SyntaxHighlighter } from "react-syntax-highlighter";
import { oneDark } from "react-syntax-highlighter/dist/esm/styles/prism";

import { languageFromPath } from "@/lib/workspace/explorer-tree";
import { useWorkspaceStore } from "@/stores/workspace-store";
import { Button } from "@/components/ui/button";

export function CodeViewer() {
  const selectedFilePath = useWorkspaceStore((state) => state.selectedFilePath);
  const selectedFileContent = useWorkspaceStore((state) => state.selectedFileContent);
  const isLoadingFile = useWorkspaceStore((state) => state.isLoadingFile);

  const [isCopied, setIsCopied] = useState(false);

  const selectedLanguage = useMemo(() => languageFromPath(selectedFilePath || ""), [selectedFilePath]);

  async function onCopy() {
    if (!selectedFileContent) return;
    try {
      await navigator.clipboard.writeText(selectedFileContent);
      setIsCopied(true);
      setTimeout(() => setIsCopied(false), 2000);
    } catch (e) {
      console.error(e);
    }
  }

  if (isLoadingFile) {
    return (
      <div className="flex h-full items-center justify-center text-zinc-400"><Loader2 className="mr-2 size-4 animate-spin text-indigo-400" />Loading file...</div>
    );
  }

  if (!selectedFilePath) {
    return (
      <div className="flex h-full items-center justify-center px-6 text-center text-xs text-zinc-500 italic">Select a file from the tree to inspect generated code.</div>
    );
  }

  return (
    <div className="flex flex-col h-full min-h-0 overflow-hidden">
      <div className="sticky top-0 flex items-center justify-between border-b border-white/5 bg-zinc-950/95 px-4 py-2 backdrop-blur shrink-0 z-10">
        <p className="truncate text-xs text-zinc-400 font-mono">{selectedFilePath}</p>
        <div className="flex items-center gap-1.5">
          <Button
            variant="ghost"
            size="icon"
            onClick={onCopy}
            className="size-7 text-zinc-450 hover:text-zinc-250 hover:bg-zinc-900 rounded-md"
            title="Copy file contents"
          >
            {isCopied ? <Check className="size-3.5 text-emerald-400" /> : <ClipboardCopy className="size-3.5" />}
          </Button>
          <Code2 className="size-3.5 text-zinc-500" />
        </div>
      </div>
      <div className="flex-1 overflow-auto bg-zinc-950 font-mono text-xs scrollbar-thin">
        <SyntaxHighlighter language={selectedLanguage} style={oneDark} customStyle={{ margin: 0, padding: "16px", background: "transparent", fontSize: "12px", minHeight: "100%" }} wrapLongLines>
          {selectedFileContent || "// Empty file"}
        </SyntaxHighlighter>
      </div>
    </div>
  );
}
