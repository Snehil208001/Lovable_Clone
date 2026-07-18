"use client";

import { FormEvent, useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { Loader2, UserPlus, User, Mail, Lock, Eye, EyeOff } from "lucide-react";

import { AuthShell } from "@/components/auth/auth-shell";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { ApiClient } from "@/lib/api-client";
import { getAxiosErrorMessage } from "@/lib/http-error";
import { useAuthStore } from "@/stores/auth-store";

export default function SignupPage() {
  const router = useRouter();
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
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

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);
    setIsSubmitting(true);

    try {
      const response = await ApiClient.signup({
        name,
        username: email,
        password,
      });

      setToken(response.data.token);
      setUser(response.data.userProfileResponse);
      router.replace("/dashboard");
    } catch (err) {
      const message = getAxiosErrorMessage(
        err,
        "Sign up failed. Please verify your inputs and try again.",
      );
      setError(message);
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <AuthShell title="Create your account" description="Start generating apps in minutes.">
      <form className="space-y-5" onSubmit={onSubmit}>
        <div className="space-y-2">
          <Label htmlFor="name" className="text-zinc-300 font-medium text-xs">
            Full Name
          </Label>
          <div className="relative">
            <User className="absolute left-3.5 top-1/2 size-4 -translate-y-1/2 text-zinc-500" />
            <Input
              id="name"
              type="text"
              placeholder="Jane Doe"
              autoComplete="name"
              value={name}
              onChange={(event) => setName(event.target.value)}
              className="h-11 border-white/10 bg-zinc-900/60 pl-11 pr-4 text-zinc-100 placeholder:text-zinc-500 focus:border-primary/50 focus:ring-primary/10 focus:ring-2 rounded-xl"
              required
              minLength={1}
              maxLength={30}
            />
          </div>
        </div>
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
          <Label htmlFor="password" className="text-zinc-300 font-medium text-xs">
            Password
          </Label>
          <div className="relative">
            <Lock className="absolute left-3.5 top-1/2 size-4 -translate-y-1/2 text-zinc-500" />
            <Input
              id="password"
              type={showPassword ? "text" : "password"}
              placeholder="At least 4 characters"
              autoComplete="new-password"
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
              Creating account...
            </>
          ) : (
            <>
              <UserPlus className="mr-2 size-4" />
              Create account
            </>
          )}
        </Button>

        <p className="text-center text-xs text-zinc-400">
          Already have an account?{" "}
          <Link href="/login" className="text-primary font-medium transition hover:text-primary/80 underline underline-offset-4">
            Sign in
          </Link>
        </p>
      </form>
    </AuthShell>
  );
}
