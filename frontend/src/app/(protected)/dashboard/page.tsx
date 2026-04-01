"use client";

import { FormEvent, useEffect, useMemo, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { AxiosError } from "axios";
import { motion } from "framer-motion";
import {
  CreditCard,
  FolderCode,
  Loader2,
  LogOut,
  MoreVertical,
  Plus,
  Sparkles,
  Trash2,
  Zap,
} from "lucide-react";

import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Progress } from "@/components/ui/progress";
import { Textarea } from "@/components/ui/textarea";
import { ApiClient } from "@/lib/api-client";
import { useAuthStore } from "@/stores/auth-store";
import type { ProjectSummaryResponse, UsageTodayResponse, PlanLimitsResponse } from "@/lib/api-client";

function formatDate(value: string): string {
  return new Intl.DateTimeFormat("en-US", {
    day: "2-digit",
    month: "short",
    year: "numeric",
  }).format(new Date(value));
}

export default function DashboardPage() {
  const router = useRouter();
  const user = useAuthStore((state) => state.user);
  const clearAuth = useAuthStore((state) => state.clearAuth);
  const [projects, setProjects] = useState<ProjectSummaryResponse[]>([]);
  const [isLoadingProjects, setIsLoadingProjects] = useState(true);
  const [isCreateDialogOpen, setIsCreateDialogOpen] = useState(false);
  const [isCreatingProject, setIsCreatingProject] = useState(false);
  const [createError, setCreateError] = useState<string | null>(null);
  const [projectName, setProjectName] = useState("");
  const [projectDescription, setProjectDescription] = useState("");
  const [deletingId, setDeletingId] = useState<number | null>(null);

  // Usage state
  const [usage, setUsage] = useState<UsageTodayResponse | null>(null);
  const [limits, setLimits] = useState<PlanLimitsResponse | null>(null);

  const initials = useMemo(() => {
    const source = user?.name || user?.username || "User";
    return source
      .split(" ")
      .map((part) => part[0])
      .join("")
      .slice(0, 2)
      .toUpperCase();
  }, [user?.name, user?.username]);

  useEffect(() => {
    let mounted = true;

    async function loadDashboard() {
      setIsLoadingProjects(true);
      try {
        const [projectsR, usageR, limitsR] = await Promise.allSettled([
          ApiClient.getMyProjects(),
          ApiClient.getTodayUsage(),
          ApiClient.getPlanLimits(),
        ]);
        if (!mounted) return;
        if (projectsR.status === "fulfilled") setProjects(projectsR.value.data);
        if (usageR.status === "fulfilled") setUsage(usageR.value.data);
        if (limitsR.status === "fulfilled") setLimits(limitsR.value.data);
      } catch {
        if (mounted) setProjects([]);
      } finally {
        if (mounted) setIsLoadingProjects(false);
      }
    }

    void loadDashboard();
    return () => { mounted = false; };
  }, []);

  async function onCreateProject(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setCreateError(null);
    setIsCreatingProject(true);

    try {
      const response = await ApiClient.createProject({
        name: projectName,
        description: projectDescription,
      });
      setIsCreateDialogOpen(false);
      setProjectName("");
      setProjectDescription("");
      router.push(`/workspace/${response.data.id}`);
    } catch (err) {
      const message =
        (err as AxiosError<{ message?: string }>)?.response?.data?.message ||
        "Unable to create project right now. Please try again.";
      setCreateError(message);
    } finally {
      setIsCreatingProject(false);
    }
  }

  async function onDeleteProject(id: number) {
    if (!confirm("Are you sure you want to delete this project?")) return;
    setDeletingId(id);
    try {
      await ApiClient.deleteProject(id);
      setProjects((prev) => prev.filter((p) => p.id !== id));
    } catch {
      // silent fail
    } finally {
      setDeletingId(null);
    }
  }

  function onLogout() {
    clearAuth();
    router.push("/login");
  }

  const tokenPercent = usage && usage.tokensLimit > 0
    ? Math.min(Math.round((usage.tokensUsed / usage.tokensLimit) * 100), 100) : 0;

  return (
    <div className="relative min-h-screen overflow-hidden bg-zinc-950 text-zinc-100">
      <div className="pointer-events-none absolute inset-0 z-0 bg-[radial-gradient(ellipse_at_top_right,_var(--tw-gradient-stops))] from-indigo-900/20 via-zinc-950 to-zinc-950" />
      <div className="relative z-10">
      <header className="sticky top-0 z-20 border-b border-white/10 bg-zinc-950/70 backdrop-blur-xl">
        <div className="mx-auto flex h-16 max-w-7xl items-center justify-between px-6">
          <div className="flex items-center gap-3">
            <div className="grid size-9 place-items-center rounded-lg border border-indigo-400/30 bg-indigo-500/10 text-indigo-300">
              <Sparkles className="size-4" />
            </div>
            <div>
              <p className="text-sm font-semibold tracking-wide">Lovable Clone</p>
              <p className="text-xs text-zinc-400">AI App Generator</p>
            </div>
          </div>

          <div className="flex items-center gap-3">
            <Link href="/billing">
              <Button
                variant="outline"
                size="sm"
                className="border-white/10 bg-zinc-900 text-zinc-300 hover:bg-zinc-800 hover:text-white"
              >
                <CreditCard className="mr-2 size-4" />
                Billing
              </Button>
            </Link>
            <div className="flex items-center gap-2 rounded-xl border border-white/10 bg-zinc-900/70 px-3 py-1.5">
              <Avatar className="size-7 border border-white/10">
                <AvatarFallback className="bg-zinc-800 text-xs text-zinc-200">
                  {initials}
                </AvatarFallback>
              </Avatar>
              <span className="text-sm text-zinc-300">{user?.name ?? user?.username}</span>
            </div>
            <Button
              variant="outline"
              onClick={onLogout}
              className="border-white/10 bg-zinc-900 text-zinc-200 hover:bg-zinc-800 hover:text-white"
            >
              <LogOut className="mr-2 size-4" />
              Logout
            </Button>
          </div>
        </div>
      </header>

      <main className="mx-auto max-w-7xl px-6 py-8">
        {/* ─── Usage Quick Stats ─── */}
        {usage && (
          <motion.div
            initial={{ opacity: 0, y: 8 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.25 }}
            className="mb-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-3"
          >
            <Card className="border-white/10 bg-zinc-900/60">
              <CardContent className="p-4 space-y-2">
                <div className="flex items-center justify-between">
                  <p className="text-xs font-medium text-zinc-400 uppercase tracking-wider flex items-center gap-1.5">
                    <Zap className="size-3.5 text-sky-400" /> AI Tokens Today
                  </p>
                  <p className="text-xs text-zinc-500">
                    {limits?.unlimitedAi ? "∞" : `${usage.tokensUsed.toLocaleString()} / ${usage.tokensLimit.toLocaleString()}`}
                  </p>
                </div>
                {!limits?.unlimitedAi ? (
                  <Progress value={tokenPercent} className="h-1.5 bg-zinc-800 [&>div]:bg-gradient-to-r [&>div]:from-indigo-500 [&>div]:to-sky-400" />
                ) : (
                  <div className="h-1.5 rounded-full bg-gradient-to-r from-indigo-500 to-sky-400" />
                )}
              </CardContent>
            </Card>

            <Card className="border-white/10 bg-zinc-900/60">
              <CardContent className="p-4 space-y-2">
                <div className="flex items-center justify-between">
                  <p className="text-xs font-medium text-zinc-400 uppercase tracking-wider">Previews Running</p>
                  <p className="text-xs text-zinc-500">{usage.previewsRunning} / {usage.previewsLimit}</p>
                </div>
                <Progress
                  value={usage.previewsLimit > 0 ? Math.round((usage.previewsRunning / usage.previewsLimit) * 100) : 0}
                  className="h-1.5 bg-zinc-800 [&>div]:bg-gradient-to-r [&>div]:from-emerald-500 [&>div]:to-teal-400"
                />
              </CardContent>
            </Card>

            {limits && (
              <Card className="border-white/10 bg-zinc-900/60">
                <CardContent className="p-4 flex items-center justify-between">
                  <div>
                    <p className="text-xs font-medium text-zinc-400 uppercase tracking-wider">Current Plan</p>
                    <p className="text-sm font-semibold text-zinc-200 mt-0.5">{limits.planeName}</p>
                  </div>
                  <Link href="/billing">
                    <Button size="sm" variant="outline" className="border-white/10 bg-zinc-900 text-zinc-300 hover:bg-zinc-800 hover:text-white text-xs h-8">
                      Upgrade
                    </Button>
                  </Link>
                </CardContent>
              </Card>
            )}
          </motion.div>
        )}

        {/* ─── Projects Header ─── */}
        <div className="mb-6 flex items-end justify-between">
          <div>
            <h1 className="text-2xl font-semibold">Your Projects</h1>
            <p className="text-sm text-zinc-400">
              Pick up where you left off or start building something new.
            </p>
          </div>

          <Dialog open={isCreateDialogOpen} onOpenChange={setIsCreateDialogOpen}>
            <DialogTrigger asChild>
              <Button className="bg-indigo-500 text-white hover:bg-indigo-400">
                <Plus className="mr-2 size-4" />
                New Project
              </Button>
            </DialogTrigger>
            <DialogContent className="border-white/10 bg-zinc-950 text-zinc-50">
              <DialogHeader>
                <DialogTitle>Create a new project</DialogTitle>
                <DialogDescription className="text-zinc-400">
                  Give your project a clear name to start generating code.
                </DialogDescription>
              </DialogHeader>
              <form className="space-y-5" onSubmit={onCreateProject}>
                <div className="space-y-2">
                  <Label htmlFor="project-name">Project Name</Label>
                  <Input
                    id="project-name"
                    value={projectName}
                    onChange={(event) => setProjectName(event.target.value)}
                    placeholder="SaaS Landing Page"
                    required
                    className="border-white/10 bg-zinc-900/80"
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="project-description">Description</Label>
                  <Textarea
                    id="project-description"
                    value={projectDescription}
                    onChange={(event) => setProjectDescription(event.target.value)}
                    placeholder="Describe your app idea..."
                    className="min-h-24 border-white/10 bg-zinc-900/80"
                  />
                  <p className="text-xs text-zinc-500">
                    Description helps AI understand the context better.
                  </p>
                </div>
                {createError ? <p className="text-sm text-red-300">{createError}</p> : null}
                <DialogFooter>
                  <Button
                    type="button"
                    variant="outline"
                    onClick={() => setIsCreateDialogOpen(false)}
                    className="border-white/10 bg-zinc-900 hover:bg-zinc-800"
                  >
                    Cancel
                  </Button>
                  <Button type="submit" disabled={isCreatingProject} className="bg-indigo-500 hover:bg-indigo-400">
                    {isCreatingProject ? (
                      <>
                        <Loader2 className="mr-2 size-4 animate-spin" />
                        Creating...
                      </>
                    ) : (
                      <>
                        <FolderCode className="mr-2 size-4" />
                        Create Project
                      </>
                    )}
                  </Button>
                </DialogFooter>
              </form>
            </DialogContent>
          </Dialog>
        </div>

        {/* ─── Project Grid ─── */}
        <section className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-3">
          <button
            type="button"
            onClick={() => setIsCreateDialogOpen(true)}
            className="group flex min-h-40 flex-col items-center justify-center rounded-xl border border-dashed border-white/20 bg-zinc-900/40 text-zinc-400 transition-all duration-300 hover:border-indigo-400/50 hover:bg-indigo-500/10 hover:text-indigo-200 hover:shadow-[0_0_30px_rgba(99,102,241,0.15)]"
          >
            <Plus className="mb-2 size-5 transition-transform group-hover:scale-110" />
            <p className="text-sm font-medium">New Project</p>
          </button>

          {isLoadingProjects ? (
            <div className="col-span-full flex items-center justify-center py-20 text-zinc-400">
              <Loader2 className="mr-2 size-4 animate-spin" />
              Loading projects...
            </div>
          ) : null}

          {!isLoadingProjects &&
            projects.map((project, index) => (
              <motion.div
                key={project.id}
                initial={{ opacity: 0, y: 12 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.2, delay: Math.min(index * 0.04, 0.2) }}
              >
                <Card
                  className="cursor-pointer border-white/10 bg-zinc-900/60 transition-all hover:-translate-y-0.5 hover:border-indigo-300/30 hover:shadow-[0_0_24px_rgba(99,102,241,0.12)]"
                  onClick={() => router.push(`/workspace/${project.id}`)}
                >
                  <CardContent className="space-y-3 p-5">
                    <div className="flex items-start justify-between gap-3">
                      <h3 className="line-clamp-2 text-base font-medium text-zinc-100">
                        {project.projectName}
                      </h3>
                      <DropdownMenu>
                        <DropdownMenuTrigger asChild onClick={(e) => e.stopPropagation()}>
                          <Button variant="ghost" size="icon" className="size-7 shrink-0 text-zinc-500 hover:text-zinc-300 hover:bg-zinc-800">
                            <MoreVertical className="size-4" />
                          </Button>
                        </DropdownMenuTrigger>
                        <DropdownMenuContent align="end" className="border-white/10 bg-zinc-900 text-zinc-100">
                          <DropdownMenuItem
                            className="text-red-400 focus:text-red-300 focus:bg-red-500/10"
                            onClick={(e) => {
                              e.stopPropagation();
                              void onDeleteProject(project.id);
                            }}
                            disabled={deletingId === project.id}
                          >
                            <Trash2 className="mr-2 size-4" />
                            {deletingId === project.id ? "Deleting..." : "Delete Project"}
                          </DropdownMenuItem>
                        </DropdownMenuContent>
                      </DropdownMenu>
                    </div>
                    {project.description && (
                      <p className="text-xs text-zinc-500 line-clamp-2">{project.description}</p>
                    )}
                    <p className="text-xs text-zinc-400">
                      Last modified {formatDate(project.updatedAt)}
                    </p>
                  </CardContent>
                </Card>
              </motion.div>
            ))}

          {!isLoadingProjects && projects.length === 0 ? (
            <div className="col-span-full rounded-xl border border-white/10 bg-zinc-900/50 p-8 text-center text-sm text-zinc-400">
              No projects yet. Create your first project to open the AI workspace.
            </div>
          ) : null}
        </section>
      </main>
      </div>
    </div>
  );
}
