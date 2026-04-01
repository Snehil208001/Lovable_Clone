"use client";

import { FormEvent, useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { Loader2, UserPlus } from "lucide-react";

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
          <Label htmlFor="name" className="text-zinc-200">
            Name
          </Label>
          <Input
            id="name"
            type="text"
            placeholder="Jane Doe"
            autoComplete="name"
            value={name}
            onChange={(event) => setName(event.target.value)}
            className="border-white/10 bg-zinc-900/70 text-zinc-50 placeholder:text-zinc-500"
            required
            minLength={1}
            maxLength={30}
          />
        </div>
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
            placeholder="At least 4 characters"
            autoComplete="new-password"
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
              Creating account...
            </>
          ) : (
            <>
              <UserPlus className="mr-2 size-4" />
              Create account
            </>
          )}
        </Button>

        <p className="text-center text-sm text-zinc-400">
          Already have an account?{" "}
          <Link href="/login" className="text-zinc-200 transition hover:text-white">
            Sign in
          </Link>
        </p>
      </form>
    </AuthShell>
  );
}
