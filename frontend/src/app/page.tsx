"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";

import { useAuthStore } from "@/stores/auth-store";

export default function HomePage() {
  const router = useRouter();
  const isHydrating = useAuthStore((state) => state.isHydrating);
  const token = useAuthStore((state) => state.token);

  useEffect(() => {
    if (isHydrating) {
      return;
    }

    router.replace(token ? "/dashboard" : "/login");
  }, [isHydrating, router, token]);

  return (
    <main className="flex min-h-screen items-center justify-center bg-zinc-950 text-zinc-300">
      <p className="text-sm">Preparing your workspace...</p>
    </main>
  );
}
