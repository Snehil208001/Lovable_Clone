"use client";

import { FormEvent, useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { Loader2, LogIn } from "lucide-react";

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
          <Label htmlFor="email" className="text-zinc-200">
            Email
          </Label>
          <Input
            id="email"
            type="email"
            placeholder="you@example.com"
            autoComplete="email"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            className="border-white/10 bg-zinc-900/70 text-zinc-50 placeholder:text-zinc-500"
            required
          />
        </div>
        <div className="space-y-2">
          <Label htmlFor="password" className="text-zinc-200">
            Password
          </Label>
          <Input
            id="password"
            type="password"
            placeholder="••••••••"
            autoComplete="current-password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            className="border-white/10 bg-zinc-900/70 text-zinc-50 placeholder:text-zinc-500"
            required
            minLength={4}
          />
        </div>

        {error ? (
          <Alert variant="destructive" className="border-red-400/20 bg-red-500/10 text-red-200">
            <AlertDescription>{error}</AlertDescription>
          </Alert>
        ) : null}

        <Button
          type="submit"
          className="h-11 w-full bg-indigo-500 font-medium text-white hover:bg-indigo-400"
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

        <p className="text-center text-sm text-zinc-400">
          Don&apos;t have an account?{" "}
          <Link href="/signup" className="text-zinc-200 transition hover:text-white">
            Create one
          </Link>
        </p>
      </form>
    </AuthShell>
  );
}
