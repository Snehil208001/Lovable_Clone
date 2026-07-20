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

// The backend resolves each plan's Stripe price id to a plain decimal amount
// (e.g. "20"). A raw "price_..." id only arrives when Stripe was unreachable —
// render that as "Paid" rather than an id.
function formatPlanPrice(
  price: string | undefined | null,
  amountInr?: number | null,
): { isFree: boolean; label: string } {
  if (amountInr != null && amountInr > 0) {
    return { isFree: false, label: `₹${amountInr}` };
  }
  if (!price) return { isFree: true, label: "Free" };
  if (price.startsWith("price_")) {
    return { isFree: false, label: "Paid" };
  }
  const num = parseFloat(price);
  if (isNaN(num) || num === 0) return { isFree: true, label: "Free" };
  return { isFree: false, label: `₹${num}` };
}

export default function BillingPage() {
  const user = useAuthStore((s) => s.user);

  const [plans, setPlans] = useState<PlanResponse[]>([]);
  const [subscription, setSubscription] = useState<SubscriptionResponse | null>(null);
  const [usage, setUsage] = useState<UsageTodayResponse | null>(null);
  const [limits, setLimits] = useState<PlanLimitsResponse | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [checkoutLoadingKey, setCheckoutLoadingKey] = useState<string | null>(null);
  const [isOpeningPortal, setIsOpeningPortal] = useState(false);
  const searchParams = useSearchParams();
  const [checkoutStatus, setCheckoutStatus] = useState<string | null>(null);

  // Capture the checkout outcome Stripe redirected back with, then strip the
  // query param so a refresh or bookmark doesn't re-show a stale banner.
  useEffect(() => {
    const status = searchParams.get("status");
    if (status) {
      setCheckoutStatus(status);
      window.history.replaceState(null, "", window.location.pathname);
    }
  }, [searchParams]);

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

  async function onCheckout(planId: number, provider: "STRIPE" | "CASHFREE") {
    const loadingKey = `${provider}-${planId}`;
    setCheckoutLoadingKey(loadingKey);
    try {
      const response = await ApiClient.createCheckoutSession({ planId, provider });
      const data = response.data;
      if (provider === "CASHFREE" && data.paymentSessionId) {
        const { load } = await import("@cashfreepayments/cashfree-js");
        const mode = data.cashfreeEnv === "sandbox" ? "sandbox" : "production";
        const cashfree = await load({ mode });
        await cashfree.checkout({
          paymentSessionId: data.paymentSessionId,
          redirectTarget: "_self",
        });
        return;
      }
      if (data.checkoutUrl) {
        window.location.href = data.checkoutUrl;
        return;
      }
      setCheckoutLoadingKey(null);
    } catch {
      setCheckoutLoadingKey(null);
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
      {/* Background pattern and gradient orbs */}
      <div className="pointer-events-none absolute inset-0 z-0 bg-grid-pattern opacity-[0.25]" />
      <div className="pointer-events-none absolute inset-0 z-0 bg-[radial-gradient(ellipse_at_top_right,rgba(99,102,241,0.12),transparent_40%),radial-gradient(circle_at_bottom_left,rgba(14,165,233,0.08),transparent_45%)]" />

      <div className="relative z-10">
        <header className="sticky top-0 z-20 border-b border-white/5 bg-zinc-950/60 backdrop-blur-xl">
          <div className="mx-auto flex h-16 max-w-5xl items-center justify-between px-6">
            <div className="flex items-center gap-3">
              <div className="grid size-9 place-items-center rounded-xl border border-indigo-400/30 bg-indigo-500/10 text-indigo-300 shadow-glow-indigo">
                <Sparkles className="size-4.5" />
              </div>
              <div>
                <p className="text-sm font-bold tracking-wide bg-gradient-to-r from-zinc-50 to-zinc-300 bg-clip-text text-transparent">AuraCode</p>
                <p className="text-[10px] uppercase tracking-wider font-semibold text-zinc-500">Billing &amp; Plans</p>
              </div>
            </div>
            <div className="flex items-center gap-2 rounded-xl border border-white/5 bg-zinc-900/40 px-3 py-1.5 backdrop-blur-sm">
              <Avatar className="size-6 border border-white/10">
                <AvatarFallback className="bg-gradient-to-br from-indigo-500 to-sky-500 text-[10px] font-bold text-white">{initials}</AvatarFallback>
              </Avatar>
              <span className="text-xs font-medium text-zinc-300">{user?.name ?? user?.username}</span>
            </div>
          </div>
        </header>

        <main className="mx-auto max-w-5xl px-6 py-8">
          <Link href="/dashboard" className="mb-6 inline-flex items-center gap-1.5 text-xs text-zinc-400 hover:text-zinc-200 transition-colors bg-zinc-900/50 hover:bg-zinc-900 border border-white/5 px-3 py-1.5 rounded-xl">
            <ArrowLeft className="size-3.5" /> Back to Dashboard
          </Link>

          <div className="mb-8">
            <h1 className="text-2xl font-bold tracking-tight mb-1 text-zinc-100">Billing &amp; Plans</h1>
            <p className="text-xs text-zinc-400">Manage your subscription, view daily token usage, and upgrade your plan.</p>
          </div>

          {checkoutStatus === "success" && (
            <div className="mb-6 rounded-xl border border-emerald-500/20 bg-emerald-500/10 p-4 text-xs text-emerald-300 flex items-center gap-3">
              <div className="grid size-8 place-items-center rounded-lg bg-emerald-500/20 text-emerald-400 shrink-0">
                <Check className="size-4" />
              </div>
              <div>
                <p className="font-semibold text-sm">Payment completed successfully!</p>
                <p className="text-emerald-400/80 text-[10px] mt-0.5">Your plan is active now. It may take a moment to sync below.</p>
              </div>
            </div>
          )}

          {checkoutStatus === "cancelled" && (
            <div className="mb-6 rounded-xl border border-amber-500/20 bg-amber-500/10 p-4 text-xs text-amber-300 flex items-center gap-3">
              <div className="grid size-8 place-items-center rounded-lg bg-amber-500/20 text-amber-400 shrink-0">
                <Zap className="size-4" />
              </div>
              <div>
                <p className="font-semibold text-sm">Checkout cancelled</p>
                <p className="text-amber-400/80 text-[10px] mt-0.5">No transactions were processed. Feel free to upgrade anytime.</p>
              </div>
            </div>
          )}
          
          {/* ─── Current Subscription ─── */}
          <section className="mb-10">
            <h2 className="text-sm font-bold uppercase tracking-wider text-zinc-400 mb-4 flex items-center gap-2">
              <Crown className="size-4 text-amber-400" /> Current Subscription
            </h2>
            <Card className="border-white/5 bg-zinc-900/40 backdrop-blur-md shadow-lg overflow-hidden rounded-2xl">
              <CardContent className="p-6">
                {subscription && subscription.plan ? (
                  <div className="flex flex-col gap-5 sm:flex-row sm:items-center sm:justify-between">
                    <div className="space-y-1">
                      <div className="flex items-center gap-2.5">
                        <p className="text-base font-bold text-zinc-100">{subscription.plan.name}</p>
                        <span className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-[9px] font-bold uppercase tracking-wider ${subscription.status === "active" ? "bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 shadow-glow-emerald" : "bg-zinc-800 text-zinc-400 border border-white/5"}`}>
                          {subscription.status}
                        </span>
                      </div>
                      <p className="text-xs text-zinc-400">
                        <strong className="text-zinc-200">{formatPlanPrice(subscription.plan.price).label}</strong>
                        {!formatPlanPrice(subscription.plan.price).isFree && <span> / month</span>}
                      </p>
                      {subscription.currentPeriod && (
                        <p className="text-[10px] text-zinc-500">
                          Period ends {new Date(subscription.currentPeriod).toLocaleDateString("en-US", { month: "short", day: "numeric", year: "numeric" })}
                        </p>
                      )}
                    </div>
                    <Button variant="outline" onClick={onOpenPortal} disabled={isOpeningPortal}
                      className="border-white/10 bg-zinc-900/60 text-zinc-300 hover:bg-zinc-800/80 hover:text-white rounded-xl h-9.5">
                      {isOpeningPortal ? <Loader2 className="mr-2 size-4 animate-spin" /> : <ExternalLink className="mr-2 size-4 text-zinc-400" />}
                      Manage Portal
                    </Button>
                  </div>
                ) : (
                  <p className="text-xs text-zinc-400">No active subscription found. Choose a plan below to get started.</p>
                )}
              </CardContent>
            </Card>
          </section>

          {/* ─── Today's Usage ─── */}
          <section className="mb-10">
            <h2 className="text-sm font-bold uppercase tracking-wider text-zinc-400 mb-4 flex items-center gap-2">
              <Zap className="size-4 text-indigo-400" /> Today&apos;s Usage Limit
            </h2>
            <div className="grid gap-5 sm:grid-cols-2">
              <Card className="border-white/5 bg-zinc-900/40 backdrop-blur-md shadow-lg rounded-2xl">
                <CardContent className="p-5.5 space-y-3.5">
                  <div className="flex items-center justify-between">
                    <p className="text-xs font-semibold text-zinc-300">AI Token Generations</p>
                    <p className="text-[11px] font-bold text-zinc-400">
                      {limits?.unlimitedAi ? "Unlimited" : `${usage?.tokensUsed?.toLocaleString() ?? 0} / ${usage?.tokensLimit?.toLocaleString() ?? 0}`}
                    </p>
                  </div>
                  {!limits?.unlimitedAi ? (
                    <Progress value={tokenPercent} className="h-2 bg-zinc-800/80 rounded-full overflow-hidden [&>div]:bg-gradient-to-r [&>div]:from-indigo-500 [&>div]:to-sky-400" />
                  ) : (
                    <div className="h-2 rounded-full bg-gradient-to-r from-indigo-500 to-sky-400" />
                  )}
                  <p className="text-[10px] text-zinc-500">
                    {limits?.unlimitedAi ? "Your plan includes unlimited daily AI code runs." : `${tokenPercent}% of daily allowance consumed`}
                  </p>
                </CardContent>
              </Card>
              <Card className="border-white/5 bg-zinc-900/40 backdrop-blur-md shadow-lg rounded-2xl">
                <CardContent className="p-5.5 space-y-3.5">
                  <div className="flex items-center justify-between">
                    <p className="text-xs font-semibold text-zinc-300">Running Previews</p>
                    <p className="text-[11px] font-bold text-zinc-400">{usage?.previewsRunning ?? 0} / {usage?.previewsLimit ?? 0}</p>
                  </div>
                  <Progress value={previewPercent} className="h-2 bg-zinc-800/80 rounded-full overflow-hidden [&>div]:bg-gradient-to-r [&>div]:from-emerald-500 [&>div]:to-teal-400" />
                  <p className="text-[10px] text-zinc-500">{previewPercent}% of concurrent preview slots active</p>
                </CardContent>
              </Card>
            </div>
            {limits && (
              <div className="mt-4 flex flex-wrap items-center gap-3.5 text-[10px] text-zinc-500 px-1">
                <span>Active Plan: <strong className="text-zinc-300">{limits.planeName}</strong></span>
                <span className="size-1 rounded-full bg-zinc-800" />
                <span>Max Projects limit: <strong className="text-zinc-300">{limits.maxProjects}</strong></span>
                <span className="size-1 rounded-full bg-zinc-800" />
                <span>Daily tokens: <strong className="text-zinc-300">{limits.unlimitedAi ? "Unlimited" : limits.maxTokensPerDay.toLocaleString()}</strong></span>
              </div>
            )}
          </section>

          {/* ─── Available Plans ─── */}
          <section>
            <h2 className="text-sm font-bold uppercase tracking-wider text-zinc-400 mb-4 flex items-center gap-2">
              <CreditCard className="size-4 text-violet-400" /> Available Subscription Plans
            </h2>
            <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
              {plans.map((plan, index) => {
                const isCurrent = subscription?.plan?.id === plan.id;
                const { isFree, label: priceLabel } = formatPlanPrice(plan.price, plan.amountInr);
                const nameLower = plan.name.toLowerCase();
                const isPro = nameLower.includes("pro");
                const isBusiness = nameLower.includes("business");
                const isFeatured = isPro || isBusiness;
                const stripeLoading = checkoutLoadingKey === `STRIPE-${plan.id}`;
                const cashfreeLoading = checkoutLoadingKey === `CASHFREE-${plan.id}`;
                
                return (
                  <motion.div key={plan.id} initial={{ opacity: 0, y: 15 }} animate={{ opacity: 1, y: 0 }}
                    transition={{ duration: 0.3, delay: Math.min(index * 0.06, 0.3) }}>
                    <Card className={`relative overflow-hidden border transition-all duration-300 rounded-2xl flex flex-col h-full ${
                      isCurrent 
                        ? "border-primary/40 bg-primary/5 shadow-[0_0_40px_rgba(34,197,94,0.12)]" 
                        : isFeatured 
                          ? "border-white/15 bg-zinc-900/50 hover:border-primary/35 hover:bg-zinc-900/70 hover:shadow-[0_0_30px_rgba(34,197,94,0.08)]"
                          : "border-white/10 bg-zinc-900/30 hover:border-white/20 hover:bg-zinc-900/50"
                    }`}>
                      {isCurrent && (
                        <div className="absolute right-4 top-4">
                          <span className="inline-flex items-center gap-1 rounded-full bg-primary/15 px-2.5 py-0.5 text-[9px] font-bold uppercase tracking-wider text-primary border border-primary/25">
                            <Check className="size-2.5" /> Current
                          </span>
                        </div>
                      )}
                      
                      {isFeatured && !isCurrent && (
                        <div className="absolute right-4 top-4">
                          <span className="inline-flex items-center gap-1 rounded-full bg-primary/10 px-2.5 py-0.5 text-[9px] font-bold uppercase tracking-wider text-primary border border-primary/20">
                            <Crown className="size-2.5" /> {isBusiness ? "Best value" : "Popular"}
                          </span>
                        </div>
                      )}

                      <CardContent className="p-6 space-y-5 flex-1 flex flex-col justify-between">
                        <div className="space-y-2 pr-16">
                          <h3 className="text-base font-bold text-foreground">{plan.name}</h3>
                          <div className="flex items-baseline">
                            <span className="text-2xl font-extrabold text-foreground tracking-tight">{priceLabel}</span>
                            {!isFree && <span className="text-xs font-medium text-muted-foreground ml-1">/mo</span>}
                          </div>
                        </div>
                        
                        <Separator className="bg-white/5" />
                        
                        <ul className="space-y-2.5 text-xs text-zinc-400 flex-1">
                          <li className="flex items-center gap-2">
                            <div className="grid size-4 place-items-center rounded-full bg-emerald-500/10 text-emerald-400 shrink-0">
                              <Check className="size-2.5" />
                            </div>
                            <span>Up to <strong>{plan.maxProjects}</strong> active projects</span>
                          </li>
                          <li className="flex items-center gap-2">
                            <div className="grid size-4 place-items-center rounded-full bg-emerald-500/10 text-emerald-400 shrink-0">
                              <Check className="size-2.5" />
                            </div>
                            <span>{plan.unlimitedAi ? <strong>Unlimited AI tokens</strong> : <><strong>{plan.maxTokensPerDay.toLocaleString()}</strong> daily tokens</>}</span>
                          </li>
                          <li className="flex items-center gap-2">
                            <div className="grid size-4 place-items-center rounded-full bg-emerald-500/10 text-emerald-400 shrink-0">
                              <Check className="size-2.5" />
                            </div>
                            <span>Real-time preview rendering</span>
                          </li>
                        </ul>
                        
                        <div className="pt-3 space-y-2">
                          {isCurrent ? (
                            <Button disabled className="w-full bg-zinc-800 text-zinc-500 border border-white/5 rounded-xl cursor-default text-xs h-10">Active Plan</Button>
                          ) : (
                            <>
                              {!isFree && (
                                <Button onClick={() => onCheckout(plan.id, "CASHFREE")} disabled={Boolean(checkoutLoadingKey)}
                                  className={`w-full text-xs font-bold h-10 rounded-xl transition-all duration-200 active:scale-[0.98] ${
                                    isFeatured
                                      ? "bg-primary text-primary-foreground hover:bg-emerald-400 shadow-[0_4px_15px_rgba(34,197,94,0.25)]"
                                      : "bg-zinc-800 text-zinc-100 hover:bg-zinc-700 hover:text-white"
                                  }`}>
                                  {cashfreeLoading ? <><Loader2 className="mr-2 size-3.5 animate-spin" />Opening Cashfree...</> : <><Zap className="mr-1.5 size-3.5" />Pay with Cashfree (UPI)</>}
                                </Button>
                              )}
                              <Button onClick={() => onCheckout(plan.id, "STRIPE")} disabled={Boolean(checkoutLoadingKey)}
                                variant={!isFree ? "outline" : "default"}
                                className={`w-full text-xs font-bold h-10 rounded-xl transition-all duration-200 active:scale-[0.98] ${
                                  !isFree
                                    ? "border-white/10 bg-transparent text-zinc-300 hover:bg-zinc-800 hover:text-white"
                                    : isFeatured
                                      ? "bg-primary text-primary-foreground hover:bg-emerald-400 shadow-[0_4px_15px_rgba(34,197,94,0.25)]"
                                      : "bg-zinc-800 text-zinc-100 hover:bg-zinc-700 hover:text-white"
                                }`}>
                                {stripeLoading ? <><Loader2 className="mr-2 size-3.5 animate-spin" />Connecting...</> : <><CreditCard className="mr-1.5 size-3.5" />{isFree ? "Downgrade" : "Pay with Stripe"}</>}
                              </Button>
                            </>
                          )}
                        </div>
                      </CardContent>
                    </Card>
                  </motion.div>
                );
              })}
              {plans.length === 0 && <p className="col-span-full text-xs text-zinc-500 text-center py-8">No subscription tiers configured currently.</p>}
            </div>
          </section>
        </main>
      </div>
    </div>
  );
}
