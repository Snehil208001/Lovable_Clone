import type { ReactNode } from "react";
import { motion } from "framer-motion";
import { Sparkles } from "lucide-react";

export function AuthShell({
  title,
  description,
  children,
}: {
  title: string;
  description: string;
  children: ReactNode;
}) {
  return (
    <div className="relative flex min-h-screen items-center justify-center overflow-hidden bg-background px-4 py-8">
      <div className="pointer-events-none absolute inset-0 z-0 bg-grid-pattern opacity-40" />
      <div className="pointer-events-none absolute inset-0 z-0 bg-[radial-gradient(circle_at_20%_20%,rgba(34,197,94,0.12),transparent_40%),radial-gradient(circle_at_80%_10%,rgba(56,189,248,0.1),transparent_35%)]" />

      <div className="absolute top-1/4 left-1/4 -z-10 size-72 rounded-full bg-primary/10 blur-[80px]" />
      <div className="absolute right-1/4 bottom-1/4 -z-10 size-72 rounded-full bg-sky-500/10 blur-[80px]" />

      <motion.div
        initial={{ opacity: 0, y: 24, scale: 0.98 }}
        animate={{ opacity: 1, y: 0, scale: 1 }}
        transition={{ duration: 0.55, ease: [0.16, 1, 0.3, 1] }}
        className="relative z-10 w-full max-w-md rounded-2xl border border-border/80 bg-card/70 p-8 shadow-glow-primary backdrop-blur-2xl"
      >
        <div className="mb-8 space-y-4">
          <div className="flex items-center gap-2.5">
            <div className="grid size-9 place-items-center rounded-lg border border-primary/30 bg-primary/10 text-primary shadow-glow-primary">
              <Sparkles className="size-4" />
            </div>
            <span className="font-heading text-lg font-bold tracking-tight text-foreground">AuraCode</span>
          </div>
          <div>
            <h1 className="font-heading text-2xl font-semibold tracking-tight text-foreground">{title}</h1>
            <p className="mt-1.5 text-sm text-muted-foreground">{description}</p>
          </div>
        </div>
        {children}
      </motion.div>
    </div>
  );
}
