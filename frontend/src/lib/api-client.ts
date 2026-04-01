import { api } from "./api";

// --- Types ---

export interface UserProfileResponse {
  id: number;
  username: string;
  name: string;
}

export interface AuthResponse {
  token: string;
  userProfileResponse: UserProfileResponse;
}

export interface ProjectSummaryResponse {
  id: number;
  projectName: string;
  description: string;
  createdAt: string;
  updatedAt: string;
}

export interface ProjectResponse {
  id: number;
  name: string;
  description: string;
  createdAt: string;
  updatedAt: string;
  owner: UserProfileResponse;
}

export interface ProjectRequest {
  name: string;
  description?: string;
}

export interface InviteMemberRequest {
  username: string;
  role: "EDITOR" | "VIEWER" | "OWNER";
}

export interface MemberResponse {
  userId: number;
  username: string;
  name: string;
  projectRole: "EDITOR" | "VIEWER" | "OWNER";
  invitedAt: string;
}

export interface UpdateMemberRoleRequest {
  role: "EDITOR" | "VIEWER" | "OWNER";
}

export interface PostalResponse {
  portalUrl: string;
}

export interface CheckoutRequest {
  planId: number;
}

export interface CheckoutResponse {
  checkoutUrl: string;
}

export interface PlanResponse {
  id: number;
  name: string;
  maxProjects: number;
  maxTokensPerDay: number;
  unlimitedAi: boolean;
  price: string;
}

export interface SubscriptionResponse {
  plan: PlanResponse;
  status: string;
  currentPeriod: string;
  tokensUsedThisCycle: number;
}

export interface UsageTodayResponse {
  tokensUsed: number;
  tokensLimit: number;
  previewsRunning: number;
  previewsLimit: number;
}

export interface PlanLimitsResponse {
  planeName: string;
  maxTokensPerDay: number;
  maxProjects: number;
  unlimitedAi: boolean;
}

export interface ChatResponse {
  id: number;
  content: string;
  role: "USER" | "ASSISTANT" | "SYSTEM" | "TOOL";
  tokensUsed: number;
  createdAt: string;
}

export interface FileNode {
  path: string;
}

export interface FileContentResponse {
  path: string;
  content: string;
}

// --- API Client ---

export const ApiClient = {
  // Auth
  signup: async (data: any) => api.post<AuthResponse>("/api/auth/signup", data),
  login: async (data: any) => api.post<AuthResponse>("/api/auth/login", data),
  getProfile: async () => api.get<UserProfileResponse>("/api/auth/me"),

  // Projects
  getMyProjects: async () => api.get<ProjectSummaryResponse[]>("/api/projects"),
  createProject: async (data: ProjectRequest) => api.post<ProjectResponse>("/api/projects", data),
  getProjectById: async (id: number) => api.get<ProjectResponse>(`/api/projects/${id}`),
  updateProject: async (id: number, data: ProjectRequest) => api.patch<ProjectResponse>(`/api/projects/${id}`, data),
  deleteProject: async (id: number) => api.delete(`/api/projects/${id}`),

  // Project Members
  getProjectMembers: async (projectId: number) => api.get<MemberResponse[]>(`/api/projects/${projectId}/members`),
  inviteMember: async (projectId: number, data: InviteMemberRequest) => api.post<MemberResponse>(`/api/projects/${projectId}/members`, data),
  updateMemberRole: async (projectId: number, memberId: number, data: UpdateMemberRoleRequest) => api.patch<MemberResponse>(`/api/projects/${projectId}/members/${memberId}`, data),
  removeMember: async (projectId: number, memberId: number) => api.delete(`/api/projects/${projectId}/members/${memberId}`),

  // Workspace / Files
  getFileTree: async (projectId: number) => api.get<FileNode[]>(`/api/projects/${projectId}/files`),
  getFile: async (projectId: number, path: string) => api.get<FileContentResponse>(`/api/projects/${projectId}/files/${encodeURIComponent(path)}`),
  
  // Chat
  getChatHistory: async (projectId: number) => api.get<ChatResponse[]>(`/api/projects/${projectId}/messages`),
  // Note: streamChat is handled manually via fetch due to EventSource/stream nature

  // Billing / Plans
  getAllPlans: async () => api.get<PlanResponse[]>("/api/plans"),
  getMySubscription: async () => api.get<SubscriptionResponse>("/api/me/subscription"),
  createCheckoutSession: async (data: CheckoutRequest) => api.post<CheckoutResponse>("/api/payments/checkout", data),
  openCustomerPortal: async () => api.post<PostalResponse>("/api/payments/portal"),

  // Usage
  getTodayUsage: async () => api.get<UsageTodayResponse>("/api/usage/today"),
  getPlanLimits: async () => api.get<PlanLimitsResponse>("/api/usage/limits"),
};
