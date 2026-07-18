"use client";

import { useEffect } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { motion } from "framer-motion";
import { ArrowRight, Sparkles } from "lucide-react";

import { Button } from "@/components/ui/button";
import { useAuthStore } from "@/stores/auth-store";

export default function HomePage() {
  const router = useRouter();
  const isHydrating = useAuthStore((state) => state.isHydrating);
  const token = useAuthStore((state) => state.token);

  useEffect(() => {
    if (isHydrating) return;
    if (token) router.replace("/dashboard");
  }, [isHydrating, router, token]);

  if (isHydrating || token) {
    return (
      <main className="flex min-h-screen items-center justify-center bg-background text-muted-foreground">
        <p className="text-sm">Preparing your workspace…</p>
      </main>
    );
  }

  return (
    <main className="relative flex min-h-screen flex-col overflow-hidden bg-background text-foreground">
      <div className="pointer-events-none absolute inset-0 bg-grid-pattern opacity-50" />
      <div className="pointer-events-none absolute inset-0 bg-[radial-gradient(ellipse_70%_50%_at_50%_-10%,rgba(34,197,94,0.16),transparent)]" />

      <header className="relative z-10 flex items-center justify-between px-6 py-5 md:px-10">
        <div className="flex items-center gap-2">
          <span className="grid size-8 place-items-center rounded-lg border border-primary/30 bg-primary/10 text-primary">
            <Sparkles className="size-4" />
          </span>
          <span className="font-heading text-lg font-bold tracking-tight">AuraCode</span>
        </div>
        <div className="flex items-center gap-2">
          <Button asChild variant="ghost" size="sm">
            <Link href="/login">Log in</Link>
          </Button>
          <Button asChild size="sm">
            <Link href="/signup">Sign up</Link>
          </Button>
        </div>
      </header>

      <section className="relative z-10 mx-auto flex w-full max-w-3xl flex-1 flex-col items-center justify-center px-6 pb-24 text-center">
        <motion.p
          initial={{ opacity: 0, y: 8 }}
          animate={{ opacity: 1, y: 0 }}
          className="mb-4 text-xs font-semibold uppercase tracking-[0.28em] text-primary"
        >
          AuraCode
        </motion.p>
        <motion.h1
          initial={{ opacity: 0, y: 16 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.05 }}
          className="font-heading text-4xl font-bold tracking-tight text-balance md:text-6xl"
        >
          Build apps by describing them
        </motion.h1>
        <motion.p
          initial={{ opacity: 0, y: 16 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.12 }}
          className="mt-5 max-w-xl text-base text-muted-foreground md:text-lg"
        >
          Chat with AI, watch a live preview, and ship multi-file React apps without leaving the workspace.
        </motion.p>
        <motion.div
          initial={{ opacity: 0, y: 16 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.2 }}
          className="mt-8 flex flex-wrap items-center justify-center gap-3"
        >
          <Button asChild size="lg" className="gap-2">
            <Link href="/signup">
              Start building <ArrowRight className="size-4" />
            </Link>
          </Button>
          <Button asChild variant="outline" size="lg">
            <Link href="/login">Open workspace</Link>
          </Button>
        </motion.div>
      </section>
    </main>
  );
}
