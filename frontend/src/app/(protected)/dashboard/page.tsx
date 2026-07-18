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

    // Validate on frontend before sending to prevent 400 Bad Request
    const trimmedName = projectName.trim();
    if (!trimmedName) {
        setCreateError("Project name cannot be empty.");
        return;
    }

    setIsCreatingProject(true);

    try {
      const response = await ApiClient.createProject({
        name: trimmedName,
        description: projectDescription.trim(),
      });
      setIsCreateDialogOpen(false);
      setProjectName("");
      setProjectDescription("");
      router.push(`/workspace/${response.data.id}`);
    } catch (err) {
        const axiosError = err as AxiosError<any>;
        console.error("Project Creation Error:", axiosError.response?.data);

        let message = "Unable to create project right now. Please try again.";

        // Better error extraction for Spring Boot validation errors
        if (axiosError.response?.data?.message) {
             message = axiosError.response.data.message;
        } else if (axiosError.response?.data?.errors) {
            // Spring Boot often returns a list of validation errors
            message = Object.values(axiosError.response.data.errors).join(", ");
        }

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
    <div className="relative min-h-screen overflow-hidden bg-background text-foreground">
      {/* Premium background gradient and grids */}
      <div className="pointer-events-none absolute inset-0 z-0 bg-grid-pattern opacity-[0.25]" />
      <div className="pointer-events-none absolute inset-0 z-0 bg-[radial-gradient(ellipse_at_top_right,rgba(34,197,94,0.12),transparent_40%),radial-gradient(circle_at_bottom_left,rgba(14,165,233,0.08),transparent_45%)]" />
      
      <div className="relative z-10">
      <header className="sticky top-0 z-20 border-b border-white/5 bg-background/60 backdrop-blur-xl">
        <div className="mx-auto flex h-16 max-w-7xl items-center justify-between px-6">
          <div className="flex items-center gap-3">
            <div className="grid size-9 place-items-center rounded-xl border border-primary/30 bg-primary/10 text-primary shadow-glow-primary">
              <Sparkles className="size-4.5" />
            </div>
            <div>
              <p className="text-sm font-bold tracking-wide bg-gradient-to-r from-foreground to-muted-foreground bg-clip-text text-transparent">AuraCode</p>
              <p className="text-[10px] uppercase tracking-wider font-semibold text-muted-foreground">AI App Platform</p>
            </div>
          </div>

          <div className="flex items-center gap-4">
            <Link href="/billing">
              <Button
                variant="outline"
                size="sm"
                className="border-white/10 bg-zinc-900/60 text-foreground/80 hover:bg-zinc-800/80 hover:text-white rounded-xl h-9"
              >
                <CreditCard className="mr-2 size-4 text-muted-foreground" />
                Billing
              </Button>
            </Link>
            
            <div className="flex items-center gap-2 rounded-xl border border-white/5 bg-zinc-900/40 px-3 py-1.5 backdrop-blur-sm">
              <Avatar className="size-6 border border-white/10">
                <AvatarFallback className="bg-gradient-to-br from-primary to-sky-500 text-[10px] font-bold text-white">
                  {initials}
                </AvatarFallback>
              </Avatar>
              <span className="text-xs font-medium text-foreground/80">{user?.name ?? user?.username}</span>
            </div>

            <Button
              variant="outline"
              size="sm"
              onClick={onLogout}
              className="border-white/10 bg-zinc-900/60 text-foreground/80 hover:bg-zinc-800/80 hover:text-red-400 rounded-xl h-9 transition-colors"
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
            initial={{ opacity: 0, y: 15 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.4, ease: [0.16, 1, 0.3, 1] }}
            className="mb-8 grid gap-4 sm:grid-cols-2 lg:grid-cols-3"
          >
            <Card className="border-white/5 bg-zinc-900/40 backdrop-blur-md shadow-lg hover:border-white/10 transition-all duration-300">
              <CardContent className="p-5 space-y-3.5">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <div className="grid size-8 place-items-center rounded-lg bg-sky-500/10 text-sky-400">
                      <Zap className="size-4" />
                    </div>
                    <p className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">
                      AI Generation Limit
                    </p>
                  </div>
                  <p className="text-xs font-bold text-foreground/80">
                    {limits?.unlimitedAi ? "Unlimited" : `${usage.tokensUsed.toLocaleString()} / ${usage.tokensLimit.toLocaleString()}`}
                  </p>
                </div>
                {!limits?.unlimitedAi ? (
                  <div className="space-y-1.5">
                    <Progress value={tokenPercent} className="h-1.5 bg-zinc-800/80 rounded-full overflow-hidden [&>div]:bg-gradient-to-r [&>div]:from-primary [&>div]:to-sky-400" />
                    <p className="text-[10px] text-muted-foreground text-right">{tokenPercent}% Daily Tokens Used</p>
                  </div>
                ) : (
                  <div className="h-1.5 rounded-full bg-gradient-to-r from-primary to-sky-400" />
                )}
              </CardContent>
            </Card>

            <Card className="border-white/5 bg-zinc-900/40 backdrop-blur-md shadow-lg hover:border-white/10 transition-all duration-300">
              <CardContent className="p-5 space-y-3.5">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <div className="grid size-8 place-items-center rounded-lg bg-emerald-500/10 text-emerald-400">
                      <Sparkles className="size-4" />
                    </div>
                    <p className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">Active Previews</p>
                  </div>
                  <p className="text-xs font-bold text-foreground/80">{usage.previewsRunning} / {usage.previewsLimit}</p>
                </div>
                <div className="space-y-1.5">
                  <Progress
                    value={usage.previewsLimit > 0 ? Math.round((usage.previewsRunning / usage.previewsLimit) * 100) : 0}
                    className="h-1.5 bg-zinc-800/80 rounded-full overflow-hidden [&>div]:bg-gradient-to-r [&>div]:from-emerald-500 [&>div]:to-teal-400"
                  />
                  <p className="text-[10px] text-muted-foreground text-right">
                    {usage.previewsLimit > 0 ? Math.round((usage.previewsRunning / usage.previewsLimit) * 100) : 0}% Preview Capacity
                  </p>
                </div>
              </CardContent>
            </Card>

            {limits && (
              <Card className="border-white/5 bg-zinc-900/40 backdrop-blur-md shadow-lg hover:border-white/10 transition-all duration-300">
                <CardContent className="p-5 flex items-center justify-between h-full">
                  <div className="space-y-1">
                    <p className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">Account Plan</p>
                    <p className="text-sm font-bold text-primary">{limits.planeName}</p>
                  </div>
                  <Link href="/billing">
                    <Button size="sm" variant="outline" className="border-primary/20 bg-primary/10 text-primary hover:bg-primary hover:text-primary-foreground text-xs h-8.5 rounded-xl transition-all duration-200">
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
            <h1 className="text-xl font-bold tracking-tight text-foreground">Your Projects</h1>
            <p className="text-xs text-muted-foreground mt-1">
              Pick up where you left off or start building something new.
            </p>
          </div>

          <Dialog open={isCreateDialogOpen} onOpenChange={setIsCreateDialogOpen}>
            <DialogTrigger asChild>
              <Button className="bg-primary text-primary-foreground hover:bg-primary/90 font-medium shadow-[0_4px_15px_rgba(34,197,94,0.25)] hover:shadow-[0_4px_20px_rgba(34,197,94,0.35)] rounded-xl h-10 transition-all duration-200">
                <Plus className="mr-2 size-4" />
                New Project
              </Button>
            </DialogTrigger>
            <DialogContent className="border-white/10 bg-background/95 backdrop-blur-2xl text-foreground rounded-2xl max-w-md shadow-2xl">
              <DialogHeader className="space-y-2">
                <DialogTitle className="text-lg font-bold tracking-tight">Create a new project</DialogTitle>
                <DialogDescription className="text-xs text-muted-foreground">
                  Give your project a clear name to start generating code.
                </DialogDescription>
              </DialogHeader>
              <form className="space-y-5" onSubmit={onCreateProject}>
                <div className="space-y-2">
                  <Label htmlFor="project-name" className="text-xs text-foreground/80 font-medium">Project Name</Label>
                  <Input
                    id="project-name"
                    value={projectName}
                    onChange={(event) => setProjectName(event.target.value)}
                    placeholder="e.g., E-commerce Checkout Flow"
                    required
                    className="h-10 border-white/10 bg-zinc-900/60 focus:border-primary/50 focus:ring-primary-500/10 focus:ring-2 rounded-xl text-sm"
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="project-description" className="text-xs text-foreground/80 font-medium">Description (Optional)</Label>
                  <Textarea
                    id="project-description"
                    value={projectDescription}
                    onChange={(event) => setProjectDescription(event.target.value)}
                    placeholder="Describe your app idea, key features, styling preferences..."
                    className="min-h-24 border-white/10 bg-zinc-900/60 focus:border-primary/50 focus:ring-primary-500/10 focus:ring-2 rounded-xl text-sm resize-none"
                  />
                  <p className="text-[10px] text-muted-foreground">
                    Description helps the AI understand the core layout requirements better.
                  </p>
                </div>
                {createError ? <p className="text-xs text-red-400 font-medium">{createError}</p> : null}
                <DialogFooter className="gap-2">
                  <Button
                    type="button"
                    variant="outline"
                    onClick={() => setIsCreateDialogOpen(false)}
                    className="border-white/10 bg-zinc-900 text-foreground/80 hover:bg-zinc-800 rounded-xl h-10"
                  >
                    Cancel
                  </Button>
                  <Button
                    type="submit"
                    disabled={isCreatingProject}
                    className="bg-primary text-primary-foreground hover:bg-emerald-400 font-bold rounded-xl h-10 px-5 shadow-lg active:scale-95 transition-all"
                  >
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
        <section className="grid grid-cols-1 gap-5 md:grid-cols-2 xl:grid-cols-3">
          <button
            type="button"
            onClick={() => setIsCreateDialogOpen(true)}
            className="group flex min-h-44 flex-col items-center justify-center rounded-2xl border border-dashed border-white/10 bg-zinc-900/10 text-muted-foreground transition-all duration-300 hover:border-primary/50 hover:bg-primary/5 hover:text-primary-200 hover:shadow-[0_0_30px_rgba(34,197,94,0.08)] cursor-pointer"
          >
            <div className="grid size-10 place-items-center rounded-xl bg-zinc-900/60 border border-white/5 group-hover:border-primary/20 group-hover:bg-primary/10 text-muted-foreground group-hover:text-primary transition-all duration-300">
              <Plus className="size-5 transition-transform group-hover:scale-110" />
            </div>
            <p className="text-xs font-semibold mt-3">Start a new project</p>
            <p className="text-[10px] text-muted-foreground mt-1">Prompt your application in natural language</p>
          </button>

          {isLoadingProjects ? (
            <div className="col-span-full flex flex-col items-center justify-center py-20 text-muted-foreground">
              <Loader2 className="size-6 animate-spin text-primary" />
              <p className="text-xs mt-3 text-muted-foreground">Loading projects...</p>
            </div>
          ) : null}

          {!isLoadingProjects &&
            projects.map((project, index) => {
              const projectInitial = project.projectName ? project.projectName.charAt(0).toUpperCase() : "P";
              return (
                <motion.div
                  key={project.id}
                  initial={{ opacity: 0, y: 15 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ duration: 0.3, delay: Math.min(index * 0.04, 0.2) }}
                >
                  <Card
                    className="group relative cursor-pointer border-white/5 bg-zinc-900/30 hover:bg-zinc-900/50 transition-all duration-300 hover:-translate-y-1 hover:border-primary/30 hover:shadow-[0_4px_30px_rgba(34,197,94,0.08)] overflow-hidden rounded-2xl"
                    onClick={() => router.push(`/workspace/${project.id}`)}
                  >
                    <div className="pointer-events-none absolute inset-0 bg-[linear-gradient(135deg,rgba(34,197,94,0.03),transparent_40%)]" />
                    
                    <CardContent className="space-y-4 p-5.5 relative z-10">
                      <div className="flex items-start justify-between gap-3">
                        <div className="flex items-center gap-3">
                          <div className="grid size-9 place-items-center rounded-xl bg-gradient-to-br from-primary to-sky-500 font-bold text-sm text-white select-none shadow-sm">
                            {projectInitial}
                          </div>
                          <div>
                            <h3 className="line-clamp-1 text-sm font-semibold text-foreground group-hover:text-white transition-colors">
                              {project.projectName}
                            </h3>
                            <p className="text-[10px] text-muted-foreground">Project ID: #{project.id}</p>
                          </div>
                        </div>

                        <DropdownMenu>
                          <DropdownMenuTrigger asChild onClick={(e) => e.stopPropagation()}>
                            <Button variant="ghost" size="icon" className="size-7 shrink-0 text-muted-foreground hover:text-foreground/80 hover:bg-zinc-800 rounded-lg">
                              <MoreVertical className="size-4" />
                            </Button>
                          </DropdownMenuTrigger>
                          <DropdownMenuContent align="end" className="border-white/10 bg-background text-foreground rounded-xl shadow-2xl">
                            <DropdownMenuItem
                              className="text-red-400 focus:text-red-300 focus:bg-red-500/10 rounded-lg cursor-pointer"
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

                      {project.description ? (
                        <p className="text-xs text-muted-foreground line-clamp-2 min-h-[32px] leading-relaxed">{project.description}</p>
                      ) : (
                        <p className="text-xs text-muted-600 italic line-clamp-2 min-h-[32px] leading-relaxed">No description provided for this project.</p>
                      )}
                      
                      <div className="flex items-center justify-between pt-1 text-[10px] text-muted-foreground border-t border-white/5">
                        <span>Last active</span>
                        <span className="font-medium text-muted-foreground">{formatDate(project.updatedAt)}</span>
                      </div>
                    </CardContent>
                  </Card>
                </motion.div>
              );
            })}

          {!isLoadingProjects && projects.length === 0 ? (
            <div className="col-span-full rounded-2xl border border-white/5 bg-zinc-900/20 p-12 text-center text-sm text-muted-foreground backdrop-blur-sm flex flex-col items-center justify-center">
              <div className="grid size-12 place-items-center rounded-full bg-primary/10 text-primary mb-4">
                <FolderCode className="size-6 animate-pulse" />
              </div>
              <h3 className="font-bold text-foreground/90">Create your first project</h3>
              <p className="text-xs text-muted-foreground mt-1 max-w-[280px] leading-relaxed">
                You don&apos;t have any workspaces yet. Create one to begin coding apps with real-time preview canvas.
              </p>
            </div>
          ) : null}
        </section>
      </main>
      </div>
    </div>
  );
}