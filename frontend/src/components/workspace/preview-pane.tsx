"use client";

import { motion } from "framer-motion";
import {
  Loader2,
  Maximize,
  Minimize,
  Monitor,
  RefreshCw,
  RotateCcw,
  Smartphone,
  ChevronLeft,
  ChevronRight,
  Globe,
} from "lucide-react";
import {
  SandpackLayout,
  SandpackPreview,
  SandpackProvider,
} from "@codesandbox/sandpack-react";

import { Button } from "@/components/ui/button";
import { getActiveFileForSandpack, SANDPACK_DEPENDENCIES, SANDPACK_ENTRY, SANDPACK_EXTERNAL_RESOURCES } from "@/lib/workspace/sandpack";
import { useWorkspaceStore } from "@/stores/workspace-store";

interface PreviewPaneProps {
  viewport: "desktop" | "mobile";
  onViewportChange: (viewport: "desktop" | "mobile") => void;
  isMaximized: boolean;
  onToggleMaximize: () => void;
  onResetLayout: () => void;
  onRefresh: () => void;
}

export function PreviewPane({
  viewport,
  onViewportChange,
  isMaximized,
  onToggleMaximize,
  onResetLayout,
  onRefresh,
}: PreviewPaneProps) {
  const previewFiles = useWorkspaceStore((state) => state.previewFiles);
  const isPreviewLoading = useWorkspaceStore((state) => state.isPreviewLoading);
  const previewError = useWorkspaceStore((state) => state.previewError);
  const previewRefreshKey = useWorkspaceStore((state) => state.previewRefreshKey);
  const selectedFilePath = useWorkspaceStore((state) => state.selectedFilePath);

  const previewWidthClass = viewport === "desktop" ? "w-full" : "mx-auto w-[375px]";
  const hasPreviewFiles = Object.keys(previewFiles).length > 0;

  return (
    <div className="flex flex-col h-full min-h-0 bg-zinc-950/20">
      {/* High-Fidelity Browser Shell Header */}
      <header className="flex flex-col gap-2 border-b border-white/5 bg-zinc-950/80 p-3 backdrop-blur-md shrink-0">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-1.5">
            {/* Window control dots */}
            <div className="flex items-center gap-1.5 px-1">
              <span className="size-2.5 rounded-full bg-red-500/80" />
              <span className="size-2.5 rounded-full bg-yellow-500/80" />
              <span className="size-2.5 rounded-full bg-green-500/80" />
            </div>
            <span className="h-4 w-[1px] bg-white/5 mx-2" />
            <span className="text-[10px] font-bold uppercase tracking-wider text-muted-foreground">Preview</span>
          </div>

          <div className="flex items-center gap-2">
            <div className="flex items-center gap-1 rounded-xl bg-zinc-900/60 p-0.5 border border-white/5">
              <Button variant={viewport === "desktop" ? "secondary" : "ghost"} size="icon" className="size-7 rounded-lg text-zinc-300 hover:text-white" onClick={() => onViewportChange("desktop")} title="Desktop mode"><Monitor className="size-3.5" /></Button>
              <Button variant={viewport === "mobile" ? "secondary" : "ghost"} size="icon" className="size-7 rounded-lg text-zinc-300 hover:text-white" onClick={() => onViewportChange("mobile")} title="Mobile mode"><Smartphone className="size-3.5" /></Button>
            </div>
            <Button variant="outline" size="sm" onClick={onResetLayout} className="border-white/10 bg-zinc-900/60 hover:bg-zinc-800 text-xs h-8.5 rounded-xl"><RotateCcw className="mr-1.5 size-3.5" />Reset</Button>
            <Button variant="outline" size="icon" className="size-8.5 border-white/10 bg-zinc-900/60 hover:bg-zinc-800 rounded-xl" onClick={onToggleMaximize}>
              {isMaximized ? <Minimize className="size-3.5 text-primary" /> : <Maximize className="size-3.5 text-muted-foreground" />}
            </Button>
          </div>
        </div>

        {/* Address bar / URL row */}
        <div className="flex items-center gap-3">
          <div className="flex items-center gap-1 text-zinc-500 shrink-0">
            <Button variant="ghost" size="icon" className="size-7 rounded-lg text-zinc-500 hover:text-zinc-300 hover:bg-zinc-900" disabled><ChevronLeft className="size-4" /></Button>
            <Button variant="ghost" size="icon" className="size-7 rounded-lg text-zinc-500 hover:text-zinc-300 hover:bg-zinc-900" disabled><ChevronRight className="size-4" /></Button>
            <Button variant="ghost" size="icon" className="size-7 rounded-lg text-zinc-400 hover:text-zinc-200 hover:bg-zinc-900" onClick={onRefresh} disabled={isPreviewLoading}>
              <RefreshCw className={`size-3.5 ${isPreviewLoading ? "animate-spin text-primary" : ""}`} />
            </Button>
          </div>
          
          <div className="flex-1 flex items-center gap-2 rounded-xl bg-zinc-900/40 border border-white/5 px-3 py-1.5 text-xs text-zinc-400 font-mono select-all truncate h-8">
            <Globe className="size-3.5 text-zinc-500 shrink-0" />
            <span className="text-zinc-500 shrink-0">localhost:3000/</span>
            <span className="text-primary/90 truncate">app/preview</span>
          </div>
        </div>
      </header>

      {/* Preview area */}
      <div className="flex-1 overflow-auto p-4 relative bg-zinc-950/20">
        {isPreviewLoading && !hasPreviewFiles ? (
          <div className="flex h-full items-center justify-center rounded-2xl border border-white/5 bg-zinc-900/20 backdrop-blur-md">
            <div className="flex flex-col items-center gap-2 text-xs text-muted-foreground">
              <Loader2 className="size-5 animate-spin text-primary" />
              <p className="font-medium text-foreground/80">Compiling preview…</p>
              <p className="text-[11px] text-muted-foreground">Syncing project files into Sandpack</p>
            </div>
          </div>
        ) : previewError && !hasPreviewFiles ? (
          <div className="flex h-full flex-col items-center justify-center rounded-2xl border border-red-500/10 bg-red-500/5 px-6 text-center">
            <p className="text-xs text-red-300 leading-relaxed max-w-sm">{previewError}</p>
            <Button variant="outline" size="sm" className="mt-4 border-white/10 bg-zinc-900 hover:bg-zinc-800 rounded-xl" onClick={onRefresh}>Recompile Canvas</Button>
          </div>
        ) : (
          <motion.div
            initial={{ opacity: 0.85, scale: 0.995 }}
            animate={{ opacity: 1, scale: 1 }}
            transition={{ duration: 0.35, ease: [0.16, 1, 0.3, 1] }}
            className={`${previewWidthClass} relative h-full min-h-[680px] rounded-2xl border border-white/5 bg-zinc-900/10 p-2 shadow-2xl transition-all duration-300 overflow-hidden`}
          >
            <SandpackProvider
              key={`sandpack-${previewRefreshKey}`}
              template="react-ts"
              theme="dark"
              files={previewFiles}
              customSetup={{
                dependencies: SANDPACK_DEPENDENCIES,
                entry: SANDPACK_ENTRY,
              }}
              options={{
                activeFile: getActiveFileForSandpack(previewFiles, selectedFilePath),
                externalResources: SANDPACK_EXTERNAL_RESOURCES,
              }}
            >
              <SandpackLayout style={{ height: "100%", minHeight: "680px", border: "none", background: "transparent" }}>
                <SandpackPreview showOpenInCodeSandbox={false} showRefreshButton={false} showNavigator={false} style={{ height: "100%", minHeight: "680px" }} />
              </SandpackLayout>
            </SandpackProvider>

            {isPreviewLoading ? (
              <div className="pointer-events-none absolute right-4 top-4 z-10 flex items-center gap-2 rounded-full border border-white/5 bg-zinc-950/85 px-3 py-1.5 text-[10px] text-zinc-300 shadow-xl backdrop-blur-md">
                <Loader2 className="size-3 animate-spin text-primary" />
                Syncing changes...
              </div>
            ) : previewError ? (
              <div className="absolute right-4 top-4 z-10 flex items-center gap-2 rounded-full border border-red-500/20 bg-zinc-950/90 px-3 py-1.5 text-[10px] text-red-300 shadow-xl backdrop-blur-md">
                {previewError}
                <button type="button" onClick={onRefresh} className="font-semibold text-red-400 hover:text-red-300 underline underline-offset-2 ml-1 cursor-pointer">Retry</button>
              </div>
            ) : null}
          </motion.div>
        )}
      </div>
    </div>
  );
}
