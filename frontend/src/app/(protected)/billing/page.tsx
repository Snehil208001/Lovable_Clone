"use client";

import { useEffect, useMemo, useState } from "react";
import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { motion } from "framer-motion";
import {
  ArrowLeft,
  Check,
  CreditCard,
  Crown,
  ExternalLink,
  Loader2,
  Sparkles,
  Zap,
} from "lucide-react";

import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Progress } from "@/components/ui/progress";
import { Separator } from "@/components/ui/separator";
import {
  ApiClient,
  type PlanResponse,
  type SubscriptionResponse,
  type UsageTodayResponse,
  type PlanLimitsResponse,
} from "@/lib/api-client";
import { useAuthStore } from "@/stores/auth-store";

// Map known Stripe price IDs to actual dollar amounts
const STRIPE_PRICE_MAP: Record<string, string> = {
  "price_1THLwCFON5kZQ6GJ0aZBoQer": "$20",   // Pro Plan
  "price_1THMC5FON5kZQ6GJw1uRQTW0": "$200",  // Business Plan
};

function formatPlanPrice(price: string | undefined | null): { isFree: boolean; label: string } {
  if (!price) return { isFree: true, label: "Free" };
  // Look up known Stripe price IDs
  if (price.startsWith("price_")) {
    const mapped = STRIPE_PRICE_MAP[price];
    return { isFree: false, label: mapped ?? "Paid" };
  }
  const num = parseFloat(price);
  if (isNaN(num) || num === 0) return { isFree: true, label: "Free" };
  return { isFree: false, label: `$${num}` };
}

export default function BillingPage() {
  const user = useAuthStore((s) => s.user);

  const [plans, setPlans] = useState<PlanResponse[]>([]);
  const [subscription, setSubscription] = useState<SubscriptionResponse | null>(null);
  const [usage, setUsage] = useState<UsageTodayResponse | null>(null);
  const [limits, setLimits] = useState<PlanLimitsResponse | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [checkoutLoadingId, setCheckoutLoadingId] = useState<number | null>(null);
  const [isOpeningPortal, setIsOpeningPortal] = useState(false);
  const searchParams = useSearchParams();
  const checkoutStatus = searchParams.get("status");

  const initials = useMemo(() => {
    const source = user?.name || user?.username || "U";
    return source.split(" ").map((p) => p[0]).join("").slice(0, 2).toUpperCase();
  }, [user]);

  useEffect(() => {
    let mounted = true;
    async function load() {
      setIsLoading(true);
      try {
        const [plansR, subR, usageR, limitsR] = await Promise.allSettled([
          ApiClient.getAllPlans(),
          ApiClient.getMySubscription(),
          ApiClient.getTodayUsage(),
          ApiClient.getPlanLimits(),
        ]);
        if (!mounted) return;
        if (plansR.status === "fulfilled") setPlans(plansR.value.data);
        if (subR.status === "fulfilled") setSubscription(subR.value.data);
        if (usageR.status === "fulfilled") setUsage(usageR.value.data);
        if (limitsR.status === "fulfilled") setLimits(limitsR.value.data);
      } finally {
        if (mounted) setIsLoading(false);
      }
    }
    void load();
    return () => { mounted = false; };
  }, []);

  async function onCheckout(planId: number) {
    setCheckoutLoadingId(planId);
    try {
      const response = await ApiClient.createCheckoutSession({ planId });
      window.location.href = response.data.checkoutUrl;
    } catch {
      setCheckoutLoadingId(null);
    }
  }

  async function onOpenPortal() {
    setIsOpeningPortal(true);
    try {
      const response = await ApiClient.openCustomerPortal();
      window.location.href = response.data.portalUrl;
    } catch {
      setIsOpeningPortal(false);
    }
  }

  const tokenPercent = usage && usage.tokensLimit > 0
    ? Math.min(Math.round((usage.tokensUsed / usage.tokensLimit) * 100), 100) : 0;
  const previewPercent = usage && usage.previewsLimit > 0
    ? Math.min(Math.round((usage.previewsRunning / usage.previewsLimit) * 100), 100) : 0;

  if (isLoading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-zinc-950 text-zinc-300">
        <Loader2 className="mr-2 size-5 animate-spin" />
        Loading billing...
      </div>
    );
  }

  return (
    <div className="relative min-h-screen overflow-hidden bg-zinc-950 text-zinc-100">
      <div className="pointer-events-none absolute inset-0 z-0 bg-[radial-gradient(ellipse_at_top_right,_var(--tw-gradient-stops))] from-indigo-900/20 via-zinc-950 to-zinc-950" />
      <div className="relative z-10">
        <header className="sticky top-0 z-20 border-b border-white/10 bg-zinc-950/70 backdrop-blur-xl">
          <div className="mx-auto flex h-16 max-w-5xl items-center justify-between px-6">
            <div className="flex items-center gap-3">
              <div className="grid size-9 place-items-center rounded-lg border border-indigo-400/30 bg-indigo-500/10 text-indigo-300">
                <Sparkles className="size-4" />
              </div>
              <div>
                <p className="text-sm font-semibold tracking-wide">Lovable Clone</p>
                <p className="text-xs text-zinc-400">Billing &amp; Plans</p>
              </div>
            </div>
            <div className="flex items-center gap-3">
              <Avatar className="size-7 border border-white/10">
                <AvatarFallback className="bg-zinc-800 text-xs text-zinc-200">{initials}</AvatarFallback>
              </Avatar>
              <span className="text-sm text-zinc-300">{user?.name ?? user?.username}</span>
            </div>
          </div>
        </header>

        <main className="mx-auto max-w-5xl px-6 py-8">
          <Link href="/dashboard" className="mb-6 inline-flex items-center gap-1 text-sm text-zinc-400 transition hover:text-zinc-200">
            <ArrowLeft className="size-4" /> Back to Dashboard
          </Link>

          <h1 className="text-2xl font-semibold mb-1">Billing &amp; Plans</h1>
          <p className="text-sm text-zinc-400 mb-8">Manage your subscription, view current usage, and upgrade your plan.</p>

          {checkoutStatus === "success" && (
            <div className="mb-6 rounded-lg border border-emerald-500/20 bg-emerald-500/10 p-4 text-sm text-emerald-300 flex items-center gap-2">
              <Check className="size-5 shrink-0" />
              <div>
                <p className="font-medium">Payment successful!</p>
                <p className="text-emerald-400/80 text-xs mt-0.5">Your subscription has been activated. It may take a moment to reflect below.</p>
              </div>
            </div>
          )}

          {checkoutStatus === "cancelled" && (
            <div className="mb-6 rounded-lg border border-amber-500/20 bg-amber-500/10 p-4 text-sm text-amber-300 flex items-center gap-2">
              <Zap className="size-5 shrink-0" />
              <div>
                <p className="font-medium">Checkout cancelled</p>
                <p className="text-amber-400/80 text-xs mt-0.5">No charges were made. You can try again anytime.</p>
              </div>
            </div>
          )}
          {/* ─── Current Subscription ─── */}
          <section className="mb-10">
            <h2 className="text-lg font-medium mb-4 flex items-center gap-2">
              <Crown className="size-5 text-amber-400" /> Current Subscription
            </h2>
            <Card className="border-white/10 bg-zinc-900/60">
              <CardContent className="p-6">
                {subscription && subscription.plan ? (
                  <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
                    <div className="space-y-1">
                      <div className="flex items-center gap-2">
                        <p className="text-lg font-semibold text-zinc-100">{subscription.plan.name}</p>
                        <span className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-[10px] font-medium uppercase tracking-wider ${subscription.status === "active" ? "bg-emerald-500/15 text-emerald-400 border border-emerald-500/20" : "bg-zinc-700/50 text-zinc-400 border border-white/10"}`}>
                          {subscription.status}
                        </span>
                      </div>
                      <p className="text-sm text-zinc-400">
                        {formatPlanPrice(subscription.plan.price).label}
                        {!formatPlanPrice(subscription.plan.price).isFree && <span>/month</span>}
                      </p>
                      {subscription.currentPeriod && (
                        <p className="text-xs text-zinc-500">
                          Current period ends {new Date(subscription.currentPeriod).toLocaleDateString("en-US", { month: "short", day: "numeric", year: "numeric" })}
                        </p>
                      )}
                    </div>
                    <Button variant="outline" onClick={onOpenPortal} disabled={isOpeningPortal}
                      className="border-white/10 bg-zinc-900 text-zinc-200 hover:bg-zinc-800 hover:text-white">
                      {isOpeningPortal ? <Loader2 className="mr-2 size-4 animate-spin" /> : <ExternalLink className="mr-2 size-4" />}
                      Manage Billing
                    </Button>
                  </div>
                ) : (
                  <p className="text-sm text-zinc-400">No active subscription found. Choose a plan below to get started.</p>
                )}
              </CardContent>
            </Card>
          </section>

          {/* ─── Today's Usage ─── */}
          <section className="mb-10">
            <h2 className="text-lg font-medium mb-4 flex items-center gap-2">
              <Zap className="size-5 text-sky-400" /> Today&apos;s Usage
            </h2>
            <div className="grid gap-4 sm:grid-cols-2">
              <Card className="border-white/10 bg-zinc-900/60">
                <CardContent className="p-5 space-y-3">
                  <div className="flex items-center justify-between">
                    <p className="text-sm font-medium text-zinc-300">AI Tokens</p>
                    <p className="text-xs text-zinc-500">
                      {limits?.unlimitedAi ? "Unlimited" : `${usage?.tokensUsed?.toLocaleString() ?? 0} / ${usage?.tokensLimit?.toLocaleString() ?? 0}`}
                    </p>
                  </div>
                  {!limits?.unlimitedAi ? (
                    <Progress value={tokenPercent} className="h-2 bg-zinc-800 [&>div]:bg-gradient-to-r [&>div]:from-indigo-500 [&>div]:to-sky-400" />
                  ) : (
                    <div className="h-2 rounded-full bg-gradient-to-r from-indigo-500 to-sky-400" />
                  )}
                  <p className="text-xs text-zinc-500">
                    {limits?.unlimitedAi ? "Your plan includes unlimited AI tokens." : `${tokenPercent}% of daily limit used`}
                  </p>
                </CardContent>
              </Card>
              <Card className="border-white/10 bg-zinc-900/60">
                <CardContent className="p-5 space-y-3">
                  <div className="flex items-center justify-between">
                    <p className="text-sm font-medium text-zinc-300">Running Previews</p>
                    <p className="text-xs text-zinc-500">{usage?.previewsRunning ?? 0} / {usage?.previewsLimit ?? 0}</p>
                  </div>
                  <Progress value={previewPercent} className="h-2 bg-zinc-800 [&>div]:bg-gradient-to-r [&>div]:from-emerald-500 [&>div]:to-teal-400" />
                  <p className="text-xs text-zinc-500">{previewPercent}% of preview slots used</p>
                </CardContent>
              </Card>
            </div>
            {limits && (
              <div className="mt-4 flex flex-wrap gap-4 text-xs text-zinc-500">
                <span>Plan: <strong className="text-zinc-300">{limits.planeName}</strong></span>
                <Separator orientation="vertical" className="h-4 bg-white/10" />
                <span>Max projects: <strong className="text-zinc-300">{limits.maxProjects}</strong></span>
                <Separator orientation="vertical" className="h-4 bg-white/10" />
                <span>Daily tokens: <strong className="text-zinc-300">{limits.unlimitedAi ? "Unlimited" : limits.maxTokensPerDay.toLocaleString()}</strong></span>
              </div>
            )}
          </section>

          {/* ─── Available Plans ─── */}
          <section>
            <h2 className="text-lg font-medium mb-4 flex items-center gap-2">
              <CreditCard className="size-5 text-violet-400" /> Available Plans
            </h2>
            <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
              {plans.map((plan, index) => {
                const isCurrent = subscription?.plan?.id === plan.id;
                const { isFree, label: priceLabel } = formatPlanPrice(plan.price);
                return (
                  <motion.div key={plan.id} initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }}
                    transition={{ duration: 0.2, delay: Math.min(index * 0.06, 0.3) }}>
                    <Card className={`relative overflow-hidden border transition-all ${isCurrent ? "border-indigo-400/40 bg-indigo-500/5 shadow-[0_0_30px_rgba(99,102,241,0.08)]" : "border-white/10 bg-zinc-900/60 hover:border-indigo-300/20"}`}>
                      {isCurrent && (
                        <div className="absolute right-3 top-3">
                          <span className="inline-flex items-center gap-1 rounded-full bg-indigo-500/15 px-2.5 py-0.5 text-[10px] font-medium uppercase tracking-wider text-indigo-300 border border-indigo-500/20">
                            <Check className="size-3" /> Current
                          </span>
                        </div>
                      )}
                      <CardContent className="p-6 space-y-4">
                        <div>
                          <h3 className="text-lg font-semibold text-zinc-100">{plan.name}</h3>
                          <p className="mt-1 text-2xl font-bold text-zinc-100">
                            {priceLabel}
                            {!isFree && <span className="text-sm font-normal text-zinc-400">/mo</span>}
                          </p>
                        </div>
                        <Separator className="bg-white/5" />
                        <ul className="space-y-2 text-sm text-zinc-400">
                          <li className="flex items-center gap-2"><Check className="size-3.5 text-emerald-400" />{plan.maxProjects} projects</li>
                          <li className="flex items-center gap-2"><Check className="size-3.5 text-emerald-400" />{plan.unlimitedAi ? "Unlimited AI tokens" : `${plan.maxTokensPerDay.toLocaleString()} tokens/day`}</li>
                        </ul>
                        {isCurrent ? (
                          <Button disabled className="w-full bg-zinc-800 text-zinc-400 cursor-default">Current Plan</Button>
                        ) : (
                          <Button onClick={() => onCheckout(plan.id)} disabled={checkoutLoadingId === plan.id}
                            className="w-full bg-indigo-500 text-white hover:bg-indigo-400">
                            {checkoutLoadingId === plan.id ? <><Loader2 className="mr-2 size-4 animate-spin" />Redirecting...</> : <><Zap className="mr-2 size-4" />{isFree ? "Downgrade" : "Upgrade"}</>}
                          </Button>
                        )}
                      </CardContent>
                    </Card>
                  </motion.div>
                );
              })}
              {plans.length === 0 && <p className="col-span-full text-sm text-zinc-500 text-center py-8">No plans available at the moment.</p>}
            </div>
          </section>
        </main>
      </div>
    </div>
  );
}
