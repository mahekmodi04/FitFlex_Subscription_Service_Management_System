import { lazy } from "react";
import { createBrowserRouter } from "react-router-dom";
import { AppShell } from "@/components/layout/AppShell";
import { AdminLayout } from "@/components/layout/AdminLayout";
import { ProtectedRoute, GuestOnlyRoute } from "@/components/layout/ProtectedRoute";
import { UserRole } from "@/types/enums";

const Landing = lazy(() => import("@/pages/public/Landing"));
const PlansPublic = lazy(() => import("@/pages/public/PlansPublic"));
const Login = lazy(() => import("@/pages/public/Login"));
const Register = lazy(() => import("@/pages/public/Register"));
const NotFound = lazy(() => import("@/pages/public/NotFound"));

const Dashboard = lazy(() => import("@/pages/member/Dashboard"));
const Subscribe = lazy(() => import("@/pages/member/Subscribe"));
const SubscriptionDetail = lazy(() => import("@/pages/member/SubscriptionDetail"));
const Upgrade = lazy(() => import("@/pages/member/Upgrade"));
const Billing = lazy(() => import("@/pages/member/Billing"));
const Account = lazy(() => import("@/pages/member/Account"));

const AdminOverview = lazy(() => import("@/pages/admin/AdminOverview"));
const AdminPlans = lazy(() => import("@/pages/admin/AdminPlans"));
const AdminCoupons = lazy(() => import("@/pages/admin/AdminCoupons"));
const AdminAddOns = lazy(() => import("@/pages/admin/AdminAddOns"));
const AdminUsers = lazy(() => import("@/pages/admin/AdminUsers"));
const AdminSubscriptions = lazy(() => import("@/pages/admin/AdminSubscriptions"));

export const router = createBrowserRouter([
  {
    element: <AppShell />,
    children: [
      { path: "/", element: <Landing /> },
      { path: "/plans", element: <PlansPublic /> },

      {
        element: <GuestOnlyRoute />,
        children: [
          { path: "/login", element: <Login /> },
          { path: "/register", element: <Register /> },
        ],
      },

      {
        element: <ProtectedRoute role={UserRole.USER} />,
        children: [
          { path: "/dashboard", element: <Dashboard /> },
          { path: "/subscribe", element: <Subscribe /> },
          { path: "/subscription/:id", element: <SubscriptionDetail /> },
          { path: "/subscription/:id/upgrade", element: <Upgrade /> },
          { path: "/billing", element: <Billing /> },
          { path: "/account", element: <Account /> },
        ],
      },

      {
        element: <ProtectedRoute role={UserRole.ADMIN} />,
        children: [
          {
            element: <AdminLayout />,
            children: [
              { path: "/admin", element: <AdminOverview /> },
              { path: "/admin/plans", element: <AdminPlans /> },
              { path: "/admin/coupons", element: <AdminCoupons /> },
              { path: "/admin/addons", element: <AdminAddOns /> },
              { path: "/admin/users", element: <AdminUsers /> },
              { path: "/admin/subscriptions", element: <AdminSubscriptions /> },
            ],
          },
        ],
      },

      { path: "*", element: <NotFound /> },
    ],
  },
]);
