"use client";

import { FormEvent, useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { Loader2, LogIn, Mail, Lock, Eye, EyeOff } from "lucide-react";

import { AuthShell } from "@/components/auth/auth-shell";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { ApiClient } from "@/lib/api-client";
import { getAxiosErrorMessage } from "@/lib/http-error";
import { useAuthStore } from "@/stores/auth-store";

export default function LoginPage() {
  const router = useRouter();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [nextPath, setNextPath] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const token = useAuthStore((state) => state.token);
  const isHydrating = useAuthStore((state) => state.isHydrating);
  const setToken = useAuthStore((state) => state.setToken);
  const setUser = useAuthStore((state) => state.setUser);

  useEffect(() => {
    if (!isHydrating && token) {
      router.replace("/dashboard");
    }
  }, [isHydrating, router, token]);

  useEffect(() => {
    if (typeof window === "undefined") {
      return;
    }

    const value = new URLSearchParams(window.location.search).get("next");
    setNextPath(value);
  }, []);

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);
    setIsSubmitting(true);

    try {
      const response = await ApiClient.login({
        username: email,
        password,
      });

      setToken(response.data.token);
      setUser(response.data.userProfileResponse);

      router.replace(nextPath || "/dashboard");
    } catch (err) {
      const message = getAxiosErrorMessage(
        err,
        "Login failed. Please check your credentials and try again.",
      );
      setError(message);
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <AuthShell title="Welcome back" description="Sign in to continue building with AI.">
      <form className="space-y-5" onSubmit={onSubmit}>
        <div className="space-y-2">
          <Label htmlFor="email" className="text-zinc-300 font-medium text-xs">
            Email Address
          </Label>
          <div className="relative">
            <Mail className="absolute left-3.5 top-1/2 size-4 -translate-y-1/2 text-zinc-500" />
            <Input
              id="email"
              type="email"
              placeholder="you@example.com"
              autoComplete="email"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              className="h-11 border-white/10 bg-zinc-900/60 pl-11 pr-4 text-zinc-100 placeholder:text-zinc-500 focus:border-primary/50 focus:ring-primary/10 focus:ring-2 rounded-xl"
              required
            />
          </div>
        </div>
        <div className="space-y-2">
          <div className="flex items-center justify-between">
            <Label htmlFor="password" className="text-zinc-300 font-medium text-xs">
              Password
            </Label>
          </div>
          <div className="relative">
            <Lock className="absolute left-3.5 top-1/2 size-4 -translate-y-1/2 text-zinc-500" />
            <Input
              id="password"
              type={showPassword ? "text" : "password"}
              placeholder="••••••••"
              autoComplete="current-password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              className="h-11 border-white/10 bg-zinc-900/60 pl-11 pr-11 text-zinc-100 placeholder:text-zinc-500 focus:border-primary/50 focus:ring-primary/10 focus:ring-2 rounded-xl"
              required
              minLength={4}
            />
            <button
              type="button"
              onClick={() => setShowPassword(!showPassword)}
              className="absolute right-3 top-1/2 -translate-y-1/2 p-1 text-zinc-500 hover:text-zinc-300 rounded-md transition-colors"
            >
              {showPassword ? <EyeOff className="size-4" /> : <Eye className="size-4" />}
            </button>
          </div>
        </div>

        {error ? (
          <Alert variant="destructive" className="border-red-500/20 bg-red-500/10 text-red-200 rounded-xl">
            <AlertDescription className="text-xs">{error}</AlertDescription>
          </Alert>
        ) : null}

        <Button
          type="submit"
          className="h-11 w-full bg-gradient-to-r from-primary to-emerald-600 hover:from-primary/90 hover:to-emerald-600 text-white font-medium shadow-[0_4px_20px_rgba(34,197,94,0.3)] hover:shadow-[0_4px_25px_rgba(34,197,94,0.4)] active:scale-[0.98] rounded-xl transition-all duration-200"
          disabled={isSubmitting}
        >
          {isSubmitting ? (
            <>
              <Loader2 className="mr-2 size-4 animate-spin" />
              Signing in...
            </>
          ) : (
            <>
              <LogIn className="mr-2 size-4" />
              Sign in
            </>
          )}
        </Button>

        <p className="text-center text-xs text-zinc-400">
          Don&apos;t have an account?{" "}
          <Link href="/signup" className="text-primary font-medium transition hover:text-primary/80 underline underline-offset-4">
            Create one
          </Link>
        </p>
      </form>
    </AuthShell>
  );
}
