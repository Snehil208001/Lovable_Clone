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

  const previewWidthClass = viewport === "desktop" ? "w-full" : "mx-auto w-[390px]";
  const hasPreviewFiles = Object.keys(previewFiles).length > 0;

  return (
    <>
      <header className="flex items-center justify-between border-b border-white/10 px-4 py-3 bg-zinc-950/80 backdrop-blur-md">
        <div>
          <p className="text-xs uppercase tracking-[0.2em] text-zinc-500">Preview</p>
          <h2 className="text-sm font-medium text-zinc-200">UI Canvas</h2>
        </div>
        <div className="flex items-center gap-2">
          <div className="rounded-lg border border-white/10 bg-zinc-900 p-1">
            <Button variant={viewport === "desktop" ? "secondary" : "ghost"} size="icon" className="size-8" onClick={() => onViewportChange("desktop")}><Monitor className="size-4" /></Button>
            <Button variant={viewport === "mobile" ? "secondary" : "ghost"} size="icon" className="size-8" onClick={() => onViewportChange("mobile")}><Smartphone className="size-4" /></Button>
          </div>
          <Button variant="outline" size="sm" onClick={onResetLayout} className="border-white/10 bg-zinc-900 hover:bg-zinc-800"><RotateCcw className="mr-2 size-4" />Reset Layout</Button>
          <Button variant="outline" size="sm" onClick={onRefresh} className="border-white/10 bg-zinc-900 hover:bg-zinc-800" disabled={isPreviewLoading}><RefreshCw className="mr-2 size-4" />{isPreviewLoading ? "Refreshing..." : "Refresh"}</Button>

          <Button variant="outline" size="icon" className="size-8 border-white/10 bg-zinc-900 hover:bg-zinc-800" onClick={onToggleMaximize}>
            {isMaximized ? <Minimize className="size-4 text-indigo-400" /> : <Maximize className="size-4 text-zinc-400" />}
          </Button>
        </div>
      </header>

      <div className="flex-1 overflow-auto p-2 relative">
        {isPreviewLoading && !hasPreviewFiles ? (
          <div className="flex h-full items-center justify-center rounded-xl border border-white/10 bg-zinc-900/70">
            <div className="flex items-center text-sm text-zinc-300"><Loader2 className="mr-2 size-4 animate-spin" />Building preview...</div>
          </div>
        ) : previewError && !hasPreviewFiles ? (
          <div className="flex h-full flex-col items-center justify-center rounded-xl border border-red-400/20 bg-red-500/5 px-6 text-center">
            <p className="text-sm text-red-200">{previewError}</p>
            <Button variant="outline" size="sm" className="mt-3 border-white/10 bg-zinc-900 hover:bg-zinc-800" onClick={onRefresh}>Retry Preview</Button>
          </div>
        ) : (
          <motion.div
            initial={{ opacity: 0.75, scale: 0.99 }}
            animate={{ opacity: 1, scale: 1 }}
            transition={{ duration: 0.2 }}
            className={`${previewWidthClass} relative h-full min-h-[800px] rounded-xl border border-white/10 bg-zinc-900/70 p-2 transition-all`}
          >
            {/* The Sandpack instance stays mounted across file updates (files are
                hot-swapped via props); the key changes only on an explicit Refresh. */}
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
              <SandpackLayout style={{ height: "100%", minHeight: "800px", border: "none", background: "transparent" }}>
                <SandpackPreview showOpenInCodeSandbox={false} showRefreshButton showNavigator={true} style={{ height: "100%", minHeight: "800px" }} />
              </SandpackLayout>
            </SandpackProvider>

            {isPreviewLoading ? (
              <div className="pointer-events-none absolute right-4 top-4 z-10 flex items-center gap-2 rounded-full border border-white/10 bg-zinc-950/85 px-3 py-1.5 text-xs text-zinc-300 shadow-lg backdrop-blur">
                <Loader2 className="size-3.5 animate-spin text-indigo-400" />
                Updating preview...
              </div>
            ) : previewError ? (
              <div className="absolute right-4 top-4 z-10 flex items-center gap-2 rounded-full border border-red-400/30 bg-zinc-950/85 px-3 py-1.5 text-xs text-red-200 shadow-lg backdrop-blur">
                {previewError}
                <button type="button" onClick={onRefresh} className="font-medium text-red-100 underline underline-offset-2">Retry</button>
              </div>
            ) : null}
          </motion.div>
        )}
      </div>
    </>
  );
}
