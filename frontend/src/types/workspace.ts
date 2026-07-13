import type { ProjectResponse } from "@/lib/api-client";

export type WorkspaceProject = ProjectResponse;

export interface FileNode {
  path: string;
}

export interface FileContentResponse {
  path: string;
  content: string;
}

export type ChatRole = "USER" | "ASSISTANT" | "SYSTEM" | "TOOL";

export interface ChatHistoryMessage {
  id: number;
  content: string;
  role: ChatRole;
  tokensUsed: number | null;
  createdAt: string;
}

export interface ChatMessage {
  id: string;
  role: "USER" | "ASSISTANT";
  content: string;
}

export type { ExplorerNode } from "@/lib/workspace/explorer-tree";
