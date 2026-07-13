"use client";

import { FormEvent, useEffect, useRef, useState } from "react";
import Link from "next/link";
import { AnimatePresence, motion } from "framer-motion";
import {
  Loader2,
  PanelLeftClose,
  PanelLeftOpen,
  SendHorizontal,
  Settings,
  Sparkles,
} from "lucide-react";

import { Button } from "@/components/ui/button";
import { formatChatContent } from "@/lib/workspace/chat-format";
import { useWorkspaceStore } from "@/stores/workspace-store";

interface ChatPanelProps {
  isCollapsed: boolean;
  onToggleCollapse: () => void;
  onOpenSettings: () => void;
  onSendPrompt: (prompt: string) => void;
}

export function ChatPanel({ isCollapsed, onToggleCollapse, onOpenSettings, onSendPrompt }: ChatPanelProps) {
  const [prompt, setPrompt] = useState("");
  const messagesEndRef = useRef<HTMLDivElement>(null);

  const activeProject = useWorkspaceStore((state) => state.activeProject);
  const messages = useWorkspaceStore((state) => state.messages);
  const isSending = useWorkspaceStore((state) => state.isSending);
  const chatError = useWorkspaceStore((state) => state.chatError);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages]);

  function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    const trimmedPrompt = prompt.trim();
    if (!trimmedPrompt || isSending) return;

    setPrompt("");
    onSendPrompt(trimmedPrompt);
  }

  return (
    <>
      <header className="border-b border-white/10 px-4 py-3">
        <div className="flex items-center justify-between">
          <Link href="/dashboard" className={`group ${isCollapsed ? "hidden" : "block"}`}>
            <p className="text-xs uppercase tracking-[0.2em] text-zinc-500 transition-colors group-hover:text-zinc-400">← Dashboard</p>
            <h2 className="truncate text-sm font-medium text-zinc-200">{activeProject?.name}</h2>
          </Link>
          <div className="flex items-center gap-1">
            <Button variant="ghost" size="icon" className={`size-8 text-zinc-400 hover:bg-zinc-900 hover:text-zinc-100 ${isCollapsed ? "hidden" : "block"}`} onClick={onOpenSettings}>
              <Settings className="size-4" />
            </Button>
            <Button variant="ghost" size="icon" className="size-8 text-zinc-400 hover:bg-zinc-900 hover:text-zinc-100" onClick={onToggleCollapse}>
              {isCollapsed ? <PanelLeftOpen className="size-4" /> : <PanelLeftClose className="size-4" />}
            </Button>
          </div>
        </div>
      </header>

      {!isCollapsed ? (
      <>
      <div className="flex-1 space-y-3 overflow-y-auto px-3 py-4">
        <AnimatePresence initial={false}>
          {messages.map((message) => {
            const isUser = message.role === "USER";
            return (
              <motion.div
                key={message.id}
                initial={{ opacity: 0, y: 6 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: -4 }}
                transition={{ duration: 0.16 }}
                className={`rounded-xl border px-3 py-2 text-sm leading-6 ${isUser ? "ml-6 border-indigo-300/30 bg-indigo-500/15 text-indigo-50" : "mr-6 border-white/10 bg-zinc-900/80 text-zinc-200"}`}
              >
                <p className="mb-1 text-[10px] uppercase tracking-[0.2em] text-zinc-400">{isUser ? "You" : "Assistant"}</p>
                <p className="whitespace-pre-wrap">{formatChatContent(message.content) || (isSending && !isUser ? "Thinking..." : "")}</p>
              </motion.div>
            );
          })}
        </AnimatePresence>
        <div ref={messagesEndRef} />
      </div>

      <div className="border-t border-white/10 p-3">
        <form onSubmit={onSubmit} className="space-y-2">
          <textarea
            value={prompt}
            onChange={(event) => setPrompt(event.target.value)}
            placeholder="Describe what you want to build..."
            className="h-24 w-full resize-none rounded-lg border border-white/10 bg-zinc-900/80 p-3 text-sm text-zinc-100 outline-none transition focus:border-indigo-300/50"
          />
          {chatError ? <p className="text-xs text-red-300">{chatError}</p> : null}
          <Button type="submit" className="w-full bg-indigo-500 text-white hover:bg-indigo-400" disabled={isSending}>
            {isSending ? <><Loader2 className="mr-2 size-4 animate-spin" />Streaming...</> : <><Sparkles className="mr-2 size-4" />Send<SendHorizontal className="ml-2 size-4" /></>}
          </Button>
        </form>
      </div>
      </>
      ) : (
        <div className="flex flex-1 items-start justify-center pt-4">
          <Link href="/dashboard" className="text-zinc-500 transition-colors hover:text-zinc-300"><Sparkles className="size-4" /></Link>
        </div>
      )}
    </>
  );
}
