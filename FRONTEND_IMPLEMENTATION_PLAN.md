# FitFlex — Gym & Fitness Membership Frontend
### Implementation Plan for React Frontend (Claude Code execution doc)

This document is the complete brief for building a production-quality React frontend on top of the existing Spring Boot subscription-service backend. The backend logic (auth, plans, subscriptions, coupons, add-ons, auto-renewal, dunning/grace period, upgrades) is done and tested — this doc covers everything needed to build the frontend against it.

---

## 1. Positioning

The backend is a general-purpose subscription billing engine (state machine, dunning/retry, prorated upgrades, usage-based add-ons) that happens to have no content baked in yet. We're skinning it as **a gym / fitness membership platform** rather than a streaming service, because:

- The add-on model (units purchased, usage tracked, capped at what was paid for) maps directly onto real gym products: extra personal training sessions, guest passes, class packs, locker rental.
- The billing engine — grace periods, dunning retries, idempotent scheduled jobs, prorated upgrades — is the part worth showing off, and a gym app lets the frontend spend its effort making *that* visible (renewal status, retry countdown, usage meters) instead of building unrelated content-catalog infrastructure a streaming clone would need.
- It's a less saturated portfolio niche than another Netflix clone.

**Brand name (placeholder, change freely):** FitFlex
**Tagline:** "Train on your terms."

---

## 2. Tech Stack

| Layer | Choice | Why |
|---|---|---|
| Framework | React 18 + Vite | Fast dev server, no need for Next.js SSR since this is an authenticated app, not content that needs SEO |
| Language | TypeScript | Catches DTO shape mismatches against the backend at compile time |
| Routing | React Router v6 | Standard, supports protected routes/layouts |
| Styling | Tailwind CSS | Fast to build a polished UI without fighting a component library |
| Component primitives | shadcn/ui (Radix-based) | Accessible primitives (dialog, select, tabs, toast) that are easy to restyle |
| State / data fetching | TanStack Query (React Query) | Handles caching, loading/error states, and refetching after mutations (e.g. re-fetch subscription after attaching an add-on) cleanly |
| Forms | React Hook Form + Zod | Validation that mirrors the backend's Bean Validation constraints |
| HTTP client | Axios with an interceptor | Attach JWT to every request, handle 401 globally (redirect to login) |
| Charts (usage meters, admin dashboard) | Recharts | Simple usage bar/progress visualizations |
| Icons | lucide-react | Consistent icon set |

---

## 3. Design System

**Color palette** — warm, energetic but not garish; avoid the generic "SaaS purple" and avoid neon gym clichés.

| Token | Hex | Usage |
|---|---|---|
| `--primary` | `#0F172A` (slate-900) | Headers, primary text, nav background |
| `--accent` | `#F97316` (orange-500) | CTAs, active states, progress bars — energy without being red/alarm-coded |
| `--accent-soft` | `#FFEDD5` (orange-100) | Badges, subtle highlights |
| `--success` | `#16A34A` (green-600) | ACTIVE status, successful payment |
| `--warning` | `#EAB308` (yellow-500) | GRACE status, retry pending |
| `--danger` | `#DC2626` (red-600) | EXPIRED/CANCELLED status, failed payment |
| `--surface` | `#FFFFFF` | Cards |
| `--surface-muted` | `#F8FAFC` (slate-50) | Page background |
| `--border` | `#E2E8F0` (slate-200) | Card borders, dividers |

**Typography:** `Inter` for UI text, `Manrope` or `Space Grotesk` for headings (something with a bit more character for hero/marketing sections). Base size 16px, generous line-height (1.6) for body copy.

**Tone:** confident, minimal, data-forward. Status badges (ACTIVE/GRACE/EXPIRED/CANCELLED/PENDING) should be color-coded pills used consistently everywhere (dashboard, admin table, subscription detail).

---

## 4. Information Architecture

### Public (unauthenticated)
- `/` — Landing page: hero, plan comparison teaser, testimonials placeholder, CTA to sign up
- `/plans` — Public plan comparison page (pulls `GET /plans`)
- `/login` — Login form
- `/register` — Registration form (posts to `POST /users`)

### Authenticated — Member
- `/dashboard` — Overview: current subscription card (plan, status badge, renewal date, auto-renew toggle), attached add-ons with usage meters, quick actions (upgrade, cancel, buy add-on)
- `/plans` (authenticated variant) — Same plan list but with "Subscribe" / "Upgrade" actions wired to the user's current state
- `/subscribe` — Subscription checkout flow: pick plan → pick add-ons (optional) → enter coupon code → review total → pay (`POST /subscriptions`)
- `/subscription/:id` — Full subscription detail: dates, plan, coupon applied, status history context, attached add-ons list (`GET /subscriptions/{id}/add-ons`), "Buy more add-on units" action, cancel action
- `/subscription/:id/upgrade` — Upgrade flow: pick new (higher) plan → optionally add new add-on units → see prorated price breakdown before confirming (`PUT /subscriptions/change-plan`)
- `/billing` — Payment history (`GET /payments/subscription/{subscriptionId}`), with a "Request refund" action per payment (UI only for now — backend endpoint not built yet, see §7)
- `/account` — Profile edit (name/email/password), no role field exposed

### Authenticated — Admin
- `/admin` — Overview dashboard: active subscriber count, MRR-ish total (sum of finalPrice for ACTIVE subs), subscriptions currently in GRACE (at-risk revenue), recent payments
- `/admin/plans` — CRUD for plans
- `/admin/coupons` — CRUD for coupons
- `/admin/addons` — CRUD for add-on catalog
- `/admin/users` — List/view users (no role editing from UI — that's intentionally not exposed, per the backend fix that blocks self-escalation)
- `/admin/subscriptions` — Table of all subscriptions with status filter, search by user

---

## 5. Full API Reference

All endpoints are relative to the backend base URL (no `/api` prefix — see backend notes). JWT goes in `Authorization: Bearer <token>` header after login.

### Auth
| Method | Endpoint | Auth | Notes |
|---|---|---|---|
| POST | `/auth/login` | public | `{ email, password }` → `{ token, ... }` |
| POST | `/users` | public | Registration. Body: `User` shape (`name`, `email`, `password`) — do NOT send `role`, it's ignored server-side |

### Users
| Method | Endpoint | Auth | Notes |
|---|---|---|---|
| GET | `/users` | ADMIN | List all |
| GET | `/users/{id}` | ADMIN or self | |
| PUT | `/users/{id}` | ADMIN or self | Update name/email/password only — role is not editable via this endpoint |
| DELETE | `/users/{id}` | ADMIN | |

### Plans
| Method | Endpoint | Auth | Notes |
|---|---|---|---|
| GET | `/plans` | public | |
| GET | `/plans/{id}` | public | |
| POST | `/plans` | ADMIN | |
| PUT | `/plans/{id}` | ADMIN | |
| DELETE | `/plans/{id}` | ADMIN | |

### Coupons
| Method | Endpoint | Auth | Notes |
|---|---|---|---|
| POST | `/coupons` | ADMIN | |
| GET | `/coupons` | ADMIN | |
| GET | `/coupons/{id}` | ADMIN | |
| GET | `/coupons/code/{code}` | ADMIN | |
| PUT | `/coupons/{id}` | ADMIN | |
| DELETE | `/coupons/{id}` | ADMIN | |

> Note: coupon lookup by code is currently ADMIN-only. The subscribe flow validates the coupon code server-side inside `POST /subscriptions` (pass `couponCode` in the request body) — the frontend does not need a public coupon-lookup endpoint, just submit the code and read the error if invalid.

### Subscriptions
| Method | Endpoint | Auth | Notes |
|---|---|---|---|
| POST | `/subscriptions` | USER | Create + pay in one call. Body: `{ userId, planId, couponCode?, autoRenew, paymentMethod, addOns?: [{ addOnId, unitsIncluded }] }` |
| GET | `/subscriptions/{id}` | ADMIN or owner | *(newly added)* |
| GET | `/subscriptions/user/{userId}` | ADMIN or self | *(newly added)* list a user's subscriptions |
| PUT | `/subscriptions/change-plan` | ADMIN or owner | Upgrade only. Body: `{ subscriptionId, newPlanId, paymentMethod, addOns?: [...] }`. On payment failure, subscription stays ACTIVE on the old plan — check `paymentStatus` in the response |
| PUT | `/subscriptions/{id}/cancel` | ADMIN or owner | |

### Add-Ons
| Method | Endpoint | Auth | Notes |
|---|---|---|---|
| GET | `/addons` | any authenticated | Catalog of active add-ons |
| POST | `/addons` | ADMIN | Create add-on |
| POST | `/addons/attach?subscriptionId=&addOnId=&unitsIncluded=` | ADMIN or subscription owner | Charges immediately. Merges with existing attached units if already attached this cycle |
| POST | `/addons/usage?subscriptionId=&addOnId=&units=` | ADMIN or subscription owner | Increments usage; blocked once `unitsUsed` would exceed `unitsIncluded` (no overage billing by design) |
| GET | `/addons/subscription/{subscriptionId}` | ADMIN or owner | *(newly added)* list add-ons attached to a subscription with usage |

### Payments
| Method | Endpoint | Auth | Notes |
|---|---|---|---|
| GET | `/payments/{id}` | ADMIN or owner | |
| GET | `/payments` | ADMIN | |
| GET | `/payments/subscription/{subscriptionId}` | ADMIN or owner | Payment history for the billing page |

### Enums to hardcode in the frontend (mirror backend enums exactly)
- `SubscriptionStatus`: `PENDING, ACTIVE, GRACE, CANCELLED, EXPIRED`
- `PaymentStatus`: `SUCCESS, FAILED, REFUNDED`
- `PaymentMethod`: `CARD, UPI, WALLET`
- `PaymentType`: `SUBSCRIPTION, RENEWAL, UPGRADE, ADDON`
- `CouponType`: `PERCENTAGE, AMOUNT, BOTH, FREE_TRIAL`
- `UserRole`: `USER, ADMIN` (never editable from the UI)

---

## 6. Key Frontend Flows (implementation notes)

### Subscribe flow (`/subscribe`)
1. Plan selection (cards, highlight recommended tier)
2. Optional add-on selection — for each catalog add-on, a quantity stepper; running subtotal shown live
3. Coupon code input (optional) — validated server-side on submit, show inline error if invalid/expired/limit-hit
4. Payment method selector (CARD/UPI/WALLET) — this is a simulated gateway (`PaymentGatewaySimulator`: transaction ID ending in an even digit succeeds, odd fails), so no real card form needed — a method selector is enough
5. Review screen: plan price + add-on total − coupon discount = final price
6. Submit → `POST /subscriptions` → on `paymentStatus: FAILED`, show a clear failure state with a retry button (re-submit); on `SUCCESS`, redirect to `/dashboard`

### Dashboard subscription card
Show status as a colored pill using the palette in §3. For `GRACE` status, surface the dunning context prominently: "Payment retry scheduled for {nextRetryDate}. Your plan will expire on {graceEndDate} if payment isn't recovered." This is the single most interesting UI moment in the app — don't bury it in a table.

### Upgrade flow (`/subscription/:id/upgrade`)
1. Show only plans with a higher price than the current plan (client-side filter of `GET /plans`)
2. Show a live prorated price preview: days used, credit for unused plan time, total due today — this can be computed client-side for display purposes using the same formula the backend uses (`plan.price / plan.durationDays * daysUsed`, capped at plan.price), but the actual charge is authoritative from the backend response
3. Optional: buy additional add-on units at the same time (merges with carried-forward remaining units — mention in the UI: "You have 1 unused session carrying over for free")
4. On failure, explicitly show: "Your current plan is unaffected — payment did not go through" (matches actual backend behavior)

### Add-on usage tracking (subscription detail page)
For each attached add-on: a progress bar (`unitsUsed / unitsIncluded`), and a "Log usage" button (calls `/addons/usage`) for demo purposes (in a real gym app this would be triggered by a trainer/staff check-in, but for a portfolio demo, self-service logging is fine). When `unitsUsed === unitsIncluded`, disable further logging and show "Buy more units" instead, which opens the attach-add-on modal.

### Billing page
Table of payments (`GET /payments/subscription/{id}`) — payment type badge (SUBSCRIPTION/RENEWAL/UPGRADE/ADDON), amount, status, date. Include a "Request refund" button per successful payment that's disabled with a tooltip ("Coming soon") until you build the refund endpoint — don't fake success.

---

## 7. Known Backend Gaps (frontend should design around these, not hide them)

- **Refunds**: `Payment` entity has refund fields but no service/endpoint exists yet. Build the refund button as a disabled/"coming soon" state now; wire it up once the endpoint exists.
- **No content-specific fields**: plans currently have no gym-specific metadata (e.g. included classes/week, guest passes). If you want richer plan cards, either extend the `Plan` entity's `description` field with structured marketing copy, or keep it simple and let `description` be a plain marketing blurb for now.
- **Route shapes**: no `/api` prefix, add-on routes use query params. This is fine and intentional — just make sure the frontend's API client matches these exact shapes (see §5).

---

## 8. Suggested Folder Structure

```
frontend/
├── src/
│   ├── api/                    # axios instance + one file per resource (auth.ts, plans.ts, subscriptions.ts, addons.ts, payments.ts, coupons.ts, users.ts)
│   ├── types/                  # TS interfaces mirroring backend DTOs/enums exactly
│   ├── components/
│   │   ├── ui/                 # shadcn primitives
│   │   ├── layout/              # AppShell, Navbar, Sidebar (admin), ProtectedRoute
│   │   └── shared/              # StatusBadge, UsageMeter, PriceBreakdown, PlanCard
│   ├── pages/
│   │   ├── public/              # Landing, PlansPublic, Login, Register
│   │   ├── member/               # Dashboard, Subscribe, SubscriptionDetail, Upgrade, Billing, Account
│   │   └── admin/                # AdminOverview, AdminPlans, AdminCoupons, AdminAddOns, AdminUsers, AdminSubscriptions
│   ├── hooks/                    # useAuth, useSubscription, useAddOns (React Query wrappers)
│   ├── context/                  # AuthContext (JWT + current user)
│   ├── lib/                      # formatCurrency, formatDate, proration preview calc
│   └── App.tsx / main.tsx / router.tsx
├── tailwind.config.ts
└── package.json
```

---

## 9. Build Order (for Claude Code to execute sequentially)

1. **Scaffold** — Vite + React + TS + Tailwind + shadcn init, base layout, color tokens, routing skeleton with placeholder pages
2. **Auth** — login/register pages, AuthContext, axios interceptor, protected route wrapper, role-based redirect (admin → `/admin`, user → `/dashboard`)
3. **Public plan browsing** — landing page, `/plans` fed by `GET /plans`
4. **Subscribe flow** — full checkout flow described in §6, wired to `POST /subscriptions`
5. **Member dashboard** — subscription card with status pill, add-on usage meters, quick actions
6. **Subscription detail + add-ons** — `/subscription/:id`, attach add-on modal, usage logging, billing cycle display
7. **Upgrade flow** — `/subscription/:id/upgrade` with proration preview
8. **Billing page** — payment history table
9. **Admin — plans/coupons/addons CRUD** — three near-identical CRUD screens, build one well and reuse the pattern
10. **Admin — overview + subscriptions table** — aggregate stats, filterable subscription list
11. **Polish pass** — empty states, loading skeletons, error boundaries, responsive check, accessibility pass on forms/dialogs

---

## 10. Environment Config

Frontend needs a single env var pointing at the backend:
```
VITE_API_BASE_URL=http://localhost:8080
```
Backend, per the fixes already applied, now requires these env vars to start:
```
DB_URL=jdbc:mysql://localhost:3306/subscription_db
DB_USERNAME=root
DB_PASSWORD=<your local mysql password>
JWT_SECRET=<any long random string>
```
