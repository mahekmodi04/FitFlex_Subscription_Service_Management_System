import { Link, useNavigate } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import {
  ArrowRight,
  ArrowUpCircle,
  CreditCard,
  Dumbbell,
  RefreshCw,
  ShieldCheck,
  Wallet,
  Zap,
} from "lucide-react";
import { getPlans } from "@/api/plans";
import { useAuth } from "@/context/AuthContext";
import { Button } from "@/components/ui/button";
import { PlanCard } from "@/components/shared/PlanCard";
import { Skeleton } from "@/components/ui/skeleton";

const HERO_IMAGE =
  "https://images.unsplash.com/photo-1517836357463-d25dfeac3438?q=80&w=1800&auto=format&fit=crop";
const CTA_IMAGE =
  "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?q=80&w=1800&auto=format&fit=crop";

// Real, verifiable product facts — not made-up social proof numbers
const STATS = [
  { value: "3", label: "Plan tiers to choose from" },
  { value: "Day-precise", label: "Proration on every upgrade" },
  { value: "Auto-retry", label: "Grace period on failed payments" },
  { value: "₹25", label: "Wallet cashback per payment" },
];

const FEATURES = [
  {
    icon: Dumbbell,
    title: "Flexible memberships",
    description: "Pick a plan that fits your training, upgrade any time with prorated pricing.",
  },
  {
    icon: Zap,
    title: "Add-ons that scale",
    description: "Personal training sessions, guest passes, class packs — buy exactly what you use.",
  },
  {
    icon: ShieldCheck,
    title: "Never lose your spot",
    description: "Automatic retry on failed payments with a grace period, so a missed card never means a lost membership.",
  },
];

const HOW_IT_WORKS = [
  {
    icon: CreditCard,
    title: "Subscribe in seconds",
    description: "Pick a plan, add extras like PT sessions, and pay by card, UPI, or wallet — checkout confirms instantly.",
  },
  {
    icon: RefreshCw,
    title: "Protected against missed payments",
    description: "If a renewal fails, FitFlex automatically retries and gives you a grace period before anything lapses.",
  },
  {
    icon: ArrowUpCircle,
    title: "Upgrade whenever, pay only the difference",
    description: "Switch to a higher plan mid-cycle and you're billed exactly the prorated difference — down to the day.",
  },
];

export default function Landing() {
  const navigate = useNavigate();
  const { isAuthenticated } = useAuth();
  const { data: plans, isLoading } = useQuery({
    queryKey: ["plans"],
    queryFn: getPlans,
  });

  const featuredPlans = (plans ?? []).filter((p) => p.active).slice(0, 3);
  // logged-in members go through /plans, where the duplicate-subscription guard lives
  const handlePlanClick = () => navigate(isAuthenticated ? "/plans" : "/register");

  return (
    <div>
      <section className="relative overflow-hidden bg-ink text-white">
        <img
          src={HERO_IMAGE}
          alt=""
          className="absolute inset-0 size-full object-cover opacity-40"
          loading="eager"
        />
        <div className="absolute inset-0 bg-gradient-to-b from-ink/70 via-ink/85 to-ink" />
        <div className="pointer-events-none absolute -left-24 top-10 size-72 rounded-full bg-accent/30 blur-[100px]" />
        <div className="pointer-events-none absolute -right-24 bottom-0 size-72 rounded-full bg-accent/20 blur-[100px]" />

        <div className="relative mx-auto max-w-6xl px-4 py-24 text-center sm:px-6 sm:py-32">
          <span className="inline-flex items-center gap-1.5 rounded-full border border-white/15 bg-white/10 px-3 py-1 text-xs font-medium text-white/90 backdrop-blur">
            <Zap className="size-3.5 text-accent" />
            Now with usage-based add-ons
          </span>
          <h1 className="mx-auto mt-6 max-w-3xl font-display text-5xl font-bold tracking-tight sm:text-6xl">
            Train on <span className="text-accent">your</span> terms.
          </h1>
          <p className="mx-auto mt-5 max-w-xl text-lg text-white/70">
            FitFlex memberships that flex with you — upgrade, add extras, and never worry about a
            missed payment costing you your spot.
          </p>
          <div className="mt-9 flex flex-wrap items-center justify-center gap-3">
            <Button asChild size="lg" className="shadow-lg shadow-accent/30">
              <Link to="/register">
                Get started
                <ArrowRight className="size-4" />
              </Link>
            </Button>
            <Button
              asChild
              size="lg"
              variant="outline"
              className="border-white/20 bg-white/5 text-white backdrop-blur hover:bg-white/15 hover:text-white"
            >
              <Link to="/plans">View plans</Link>
            </Button>
          </div>

          <dl className="mx-auto mt-16 grid max-w-3xl grid-cols-2 gap-6 border-t border-white/10 pt-10 sm:grid-cols-4">
            {STATS.map((stat) => (
              <div key={stat.label}>
                <dt className="font-display text-xl font-bold text-white sm:text-2xl">{stat.value}</dt>
                <dd className="mt-1 text-xs text-white/60 sm:text-sm">{stat.label}</dd>
              </div>
            ))}
          </dl>
        </div>
      </section>

      <section className="mx-auto max-w-6xl px-4 py-20 sm:px-6">
        <div className="grid gap-8 sm:grid-cols-3">
          {FEATURES.map((feature) => (
            <div
              key={feature.title}
              className="group flex flex-col items-start gap-3 rounded-2xl border border-border bg-white p-6 transition-all hover:-translate-y-1 hover:border-accent/40 hover:shadow-lg hover:shadow-accent/5"
            >
              <div className="flex size-11 items-center justify-center rounded-xl bg-gradient-to-br from-accent to-orange-600 text-white shadow-md shadow-accent/30">
                <feature.icon className="size-5" />
              </div>
              <h3 className="font-display text-lg font-semibold text-ink">{feature.title}</h3>
              <p className="text-sm text-muted-foreground">{feature.description}</p>
            </div>
          ))}
        </div>
      </section>

      <section className="border-y border-border bg-surface-muted py-20">
        <div className="mx-auto max-w-6xl px-4 sm:px-6">
          <div className="mb-10 flex flex-wrap items-end justify-between gap-4">
            <div>
              <h2 className="font-display text-2xl font-bold text-ink sm:text-3xl">Membership plans</h2>
              <p className="mt-1 text-muted-foreground">Simple pricing, no surprises.</p>
            </div>
            <Button asChild variant="ghost">
              <Link to="/plans">
                See all plans
                <ArrowRight className="size-4" />
              </Link>
            </Button>
          </div>

          {isLoading ? (
            <div className="grid gap-6 sm:grid-cols-3">
              {[0, 1, 2].map((i) => (
                <Skeleton key={i} className="h-80 w-full rounded-xl" />
              ))}
            </div>
          ) : featuredPlans.length === 0 ? (
            <p className="text-muted-foreground">Plans are coming soon — check back shortly.</p>
          ) : (
            <div className="grid gap-6 sm:grid-cols-3">
              {featuredPlans.map((plan, i) => (
                <PlanCard
                  key={plan.id}
                  plan={plan}
                  highlighted={i === 1}
                  actionLabel={isAuthenticated ? "View plans" : "Get started"}
                  onAction={handlePlanClick}
                />
              ))}
            </div>
          )}
        </div>
      </section>

      <section className="mx-auto max-w-6xl px-4 py-20 sm:px-6">
        <div className="mb-10 text-center">
          <Wallet className="mx-auto size-6 text-accent" />
          <h2 className="mt-3 font-display text-2xl font-bold text-ink sm:text-3xl">
            How the billing actually works
          </h2>
          <p className="mx-auto mt-2 max-w-xl text-muted-foreground">
            Not just a pricing page — real dunning retries, grace periods, and day-precise proration.
          </p>
        </div>
        <div className="grid gap-6 sm:grid-cols-3">
          {HOW_IT_WORKS.map((step, i) => (
            <div
              key={step.title}
              className="relative rounded-2xl border border-border bg-white p-6 transition-shadow hover:shadow-lg hover:shadow-black/5"
            >
              <span className="absolute -top-3 -left-3 flex size-7 items-center justify-center rounded-full bg-ink text-xs font-semibold text-white">
                {i + 1}
              </span>
              <div className="flex size-10 items-center justify-center rounded-lg bg-accent-soft text-accent">
                <step.icon className="size-5" />
              </div>
              <h3 className="mt-4 font-display text-base font-semibold text-ink">{step.title}</h3>
              <p className="mt-2 text-sm leading-relaxed text-muted-foreground">{step.description}</p>
            </div>
          ))}
        </div>
      </section>

      <section className="relative overflow-hidden py-20 text-center text-white">
        <img src={CTA_IMAGE} alt="" className="absolute inset-0 size-full object-cover" loading="lazy" />
        <div className="absolute inset-0 bg-ink/85" />
        <div className="relative mx-auto max-w-2xl px-4 sm:px-6">
          <h2 className="font-display text-2xl font-bold sm:text-3xl">Ready to train on your terms?</h2>
          <p className="mt-3 text-white/70">Join in under a minute — no card required to browse plans.</p>
          <div className="mt-8">
            <Button asChild size="lg" className="shadow-lg shadow-accent/30">
              <Link to="/register">
                Create your account
                <ArrowRight className="size-4" />
              </Link>
            </Button>
          </div>
        </div>
      </section>
    </div>
  );
}
