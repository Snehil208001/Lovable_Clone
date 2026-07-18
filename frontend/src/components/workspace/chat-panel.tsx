"use client";

import { FormEvent, useEffect, useRef, useState } from "react";
import Link from "next/link";
import { AnimatePresence, motion } from "framer-motion";
import {
  FileCode2,
  FilePenLine,
  PanelLeftClose,
  PanelLeftOpen,
  SendHorizontal,
  Settings,
  Sparkles,
  Square,
} from "lucide-react";

import { Button } from "@/components/ui/button";
import { parseChatTimeline } from "@/lib/workspace/chat-format";
import { useWorkspaceStore } from "@/stores/workspace-store";

interface ChatPanelProps {
  isCollapsed: boolean;
  onToggleCollapse: () => void;
  onOpenSettings: () => void;
  onSendPrompt: (prompt: string) => void;
  onStopGeneration?: () => void;
}

const CHAT_SUGGESTIONS = [
  "Create a modern landing page",
  "Add a responsive navbar",
  "Build a styled habit tracker",
];

export function ChatPanel({
  isCollapsed,
  onToggleCollapse,
  onOpenSettings,
  onSendPrompt,
  onStopGeneration,
}: ChatPanelProps) {
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
      <header className="sticky top-0 z-10 flex h-14 items-center justify-between border-b border-border/70 bg-background/80 px-4 py-3 backdrop-blur-md">
        <div className="flex items-center gap-2 overflow-hidden">
          <Link href="/dashboard" className={`group overflow-hidden ${isCollapsed ? "hidden" : "block"}`}>
            <p className="text-[9px] uppercase tracking-[0.2em] text-muted-foreground transition-colors group-hover:text-primary">
              Dashboard
            </p>
            <h2 className="mt-0.5 truncate font-heading text-xs font-semibold text-foreground">
              {activeProject?.name}
            </h2>
          </Link>
        </div>
        <div className="flex shrink-0 items-center gap-1.5">
          <Button
            variant="ghost"
            size="icon"
            className={`size-8 rounded-lg text-muted-foreground hover:text-foreground ${isCollapsed ? "hidden" : "flex"}`}
            onClick={onOpenSettings}
          >
            <Settings className="size-4" />
          </Button>
          <Button
            variant="ghost"
            size="icon"
            className="size-8 rounded-lg text-muted-foreground hover:text-foreground"
            onClick={onToggleCollapse}
          >
            {isCollapsed ? <PanelLeftOpen className="size-4 text-primary" /> : <PanelLeftClose className="size-4" />}
          </Button>
        </div>
      </header>

      {!isCollapsed ? (
        <>
          <div className="flex-1 space-y-4 overflow-y-auto px-4 py-4">
            <AnimatePresence initial={false}>
              {messages.length === 0 ? (
                <motion.div
                  initial={{ opacity: 0, y: 10 }}
                  animate={{ opacity: 1, y: 0 }}
                  className="flex flex-col items-center justify-center px-2 py-12 text-center"
                >
                  <div className="mb-4 grid size-10 place-items-center rounded-xl border border-primary/25 bg-primary/10 text-primary shadow-glow-primary">
                    <Sparkles className="size-5" />
                  </div>
                  <h3 className="font-heading text-sm font-semibold text-foreground">Build with AuraCode</h3>
                  <p className="mt-1 max-w-[220px] text-[11px] leading-relaxed text-muted-foreground">
                    Describe an app or UI change. Files apply as they finish streaming.
                  </p>
                </motion.div>
              ) : null}

              {messages.map((message) => {
                const isUser = message.role === "USER";
                const timeline = isUser ? null : parseChatTimeline(message.content);
                const emptyThinking = isSending && !isUser && !message.content;

                return (
                  <motion.div
                    key={message.id}
                    initial={{ opacity: 0, y: 10 }}
                    animate={{ opacity: 1, y: 0 }}
                    exit={{ opacity: 0, y: -10 }}
                    transition={{ duration: 0.25, ease: [0.16, 1, 0.3, 1] }}
                    className={`flex flex-col gap-1 ${isUser ? "items-end" : "items-start"}`}
                  >
                    <span className="px-1 text-[9px] font-semibold uppercase tracking-wider text-muted-foreground">
                      {isUser ? "You" : "Assistant"}
                    </span>
                    <div
                      className={`max-w-[92%] rounded-2xl border px-4 py-2.5 text-xs leading-relaxed shadow-sm ${
                        isUser
                          ? "rounded-tr-none border-primary/30 bg-primary/15 text-foreground"
                          : "rounded-tl-none border-border/80 bg-card/80 text-foreground/90 backdrop-blur-sm"
                      }`}
                    >
                      {isUser ? (
                        <p className="whitespace-pre-wrap">{message.content}</p>
                      ) : emptyThinking ? (
                        <p className="text-muted-foreground">Thinking…</p>
                      ) : (
                        <div className="space-y-2">
                          {timeline?.map((item, index) =>
                            item.kind === "text" ? (
                              <p key={`${message.id}-t-${index}`} className="whitespace-pre-wrap">
                                {item.text}
                              </p>
                            ) : (
                              <div
                                key={`${message.id}-a-${index}`}
                                className="flex items-center gap-2 rounded-lg border border-border/60 bg-secondary/50 px-2.5 py-1.5 text-[11px] text-muted-foreground"
                              >
                                {item.action === "reading" ? (
                                  <FileCode2 className="size-3.5 shrink-0 text-sky-400" />
                                ) : (
                                  <FilePenLine className="size-3.5 shrink-0 text-primary" />
                                )}
                                <span className="truncate">
                                  <span className="font-medium text-foreground/80">
                                    {item.action === "reading"
                                      ? "Reading"
                                      : item.action === "writing"
                                        ? "Writing"
                                        : "Wrote"}
                                  </span>{" "}
                                  {item.detail}
                                </span>
                              </div>
                            ),
                          )}
                        </div>
                      )}
                    </div>
                  </motion.div>
                );
              })}
            </AnimatePresence>
            <div ref={messagesEndRef} />
          </div>

          <div className="space-y-3 border-t border-border/70 bg-background/50 p-4">
            {messages.length === 0 && (
              <div className="flex flex-col gap-1.5">
                <span className="text-[9px] font-bold uppercase tracking-wider text-muted-foreground">
                  Suggestions
                </span>
                <div className="flex flex-col gap-1.5">
                  {CHAT_SUGGESTIONS.map((sug) => (
                    <button
                      key={sug}
                      type="button"
                      onClick={() => setPrompt(sug)}
                      className="w-full cursor-pointer truncate rounded-xl border border-border/70 bg-secondary/40 px-3 py-2 text-left text-[11px] text-muted-foreground transition-all duration-200 hover:bg-secondary hover:text-foreground active:scale-[0.99]"
                    >
                      {sug}
                    </button>
                  ))}
                </div>
              </div>
            )}

            <form onSubmit={onSubmit} className="space-y-3">
              <div className="relative rounded-2xl border border-border bg-secondary/50 transition-all duration-200 focus-within:border-primary/40 focus-within:ring-2 focus-within:ring-primary/15">
                <textarea
                  value={prompt}
                  onChange={(event) => setPrompt(event.target.value)}
                  placeholder="Describe UI or request code changes..."
                  className="min-h-20 w-full resize-none bg-transparent p-3.5 pr-11 text-xs text-foreground outline-none placeholder:text-muted-foreground"
                  onKeyDown={(e) => {
                    if (e.key === "Enter" && !e.shiftKey) {
                      e.preventDefault();
                      e.currentTarget.form?.requestSubmit();
                    }
                  }}
                />
                {isSending ? (
                  <Button
                    type="button"
                    size="icon"
                    className="absolute right-2.5 bottom-2.5 size-7 rounded-lg bg-destructive text-white transition-all hover:bg-destructive/90 active:scale-90"
                    onClick={() => onStopGeneration?.()}
                    aria-label="Stop generation"
                  >
                    <Square className="size-3 fill-current" />
                  </Button>
                ) : (
                  <Button
                    type="submit"
                    size="icon"
                    className="absolute right-2.5 bottom-2.5 size-7 rounded-lg active:scale-90"
                    disabled={!prompt.trim()}
                  >
                    <SendHorizontal className="size-3.5" />
                  </Button>
                )}
              </div>
              {chatError ? <p className="text-[10px] font-medium text-destructive">{chatError}</p> : null}
            </form>
          </div>
        </>
      ) : (
        <div className="flex flex-1 flex-col items-center justify-between py-6">
          <Link
            href="/dashboard"
            className="grid size-8 place-items-center rounded-lg border border-primary/25 bg-primary/10 text-primary transition-all duration-200 hover:bg-primary hover:text-primary-foreground"
          >
            <Sparkles className="size-4" />
          </Link>
          <Button
            variant="ghost"
            size="icon"
            className="size-8 text-muted-foreground hover:text-foreground"
            onClick={onToggleCollapse}
          >
            <PanelLeftOpen className="size-4" />
          </Button>
        </div>
      )}
    </>
  );
}
