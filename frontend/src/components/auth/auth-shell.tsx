import type { ReactNode } from "react";
import { motion } from "framer-motion";

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
    <div className="relative flex min-h-screen items-center justify-center overflow-hidden bg-zinc-950 px-4 py-8">
      <div className="pointer-events-none absolute inset-0 bg-[radial-gradient(circle_at_20%_20%,rgba(255,255,255,0.08),transparent_25%),radial-gradient(circle_at_80%_0%,rgba(79,70,229,0.16),transparent_25%),radial-gradient(circle_at_80%_80%,rgba(14,165,233,0.12),transparent_25%)]" />
      <motion.div 
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5, ease: "easeOut" }}
        className="relative z-10 w-full max-w-md rounded-2xl border border-white/10 bg-white/5 p-8 shadow-[0_0_30px_rgba(99,102,241,0.12)] backdrop-blur-xl"
      >
        <div className="mb-8 space-y-2">
          <p className="text-xs uppercase tracking-[0.24em] text-zinc-400">Lovable Clone</p>
          <h1 className="text-2xl font-semibold text-zinc-50">{title}</h1>
          <p className="text-sm text-zinc-400">{description}</p>
        </div>
        {children}
      </motion.div>
    </div>
  );
}
