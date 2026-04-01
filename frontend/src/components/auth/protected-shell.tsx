"use client";

import { useEffect } from "react";
import { usePathname, useRouter } from "next/navigation";

import { useAuthStore } from "@/stores/auth-store";

export function ProtectedShell({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const pathname = usePathname();
  const token = useAuthStore((state) => state.token);
  const user = useAuthStore((state) => state.user);
  const isHydrating = useAuthStore((state) => state.isHydrating);

  useEffect(() => {
    if (isHydrating) {
      return;
    }

    if (!token || !user) {
      router.replace(`/login?next=${encodeURIComponent(pathname)}`);
    }
  }, [isHydrating, pathname, router, token, user]);

  if (isHydrating || !token || !user) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-zinc-950 text-zinc-300">
        <p className="text-sm">Checking your session...</p>
      </div>
    );
  }

  return <>{children}</>;
}
