"use client";

import { create } from "zustand";

import { clearStoredToken, getStoredToken, setStoredToken } from "@/lib/auth-token";
import { ApiClient } from "@/lib/api-client";
import type { UserProfileResponse } from "@/lib/api-client";

interface AuthState {
  user: UserProfileResponse | null;
  token: string | null;
  isHydrating: boolean;
  setToken: (token: string) => void;
  setUser: (user: UserProfileResponse | null) => void;
  clearAuth: () => void;
  hydrateAuth: () => Promise<void>;
}

export const useAuthStore = create<AuthState>((set) => ({
  user: null,
  token: null,
  isHydrating: true,
  setToken: (token) => {
    setStoredToken(token);
    set({ token });
  },
  setUser: (user) => set({ user }),
  clearAuth: () => {
    clearStoredToken();
    set({ token: null, user: null, isHydrating: false });
  },
  hydrateAuth: async () => {
    const token = getStoredToken();

    if (!token) {
      set({ token: null, user: null, isHydrating: false });
      return;
    }

    set({ token, isHydrating: true });

    try {
      const response = await ApiClient.getProfile();
      set({ user: response.data, isHydrating: false });
    } catch {
      clearStoredToken();
      set({ token: null, user: null, isHydrating: false });
    }
  },
}));
