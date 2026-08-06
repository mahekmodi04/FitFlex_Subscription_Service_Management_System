import { NavLink, Outlet } from "react-router-dom";
import { cn } from "@/lib/utils";

const NAV_ITEMS = [
  { to: "/admin", label: "Overview", end: true },
  { to: "/admin/subscriptions", label: "Subscriptions" },
  { to: "/admin/plans", label: "Plans" },
  { to: "/admin/coupons", label: "Coupons" },
  { to: "/admin/addons", label: "Add-ons" },
  { to: "/admin/users", label: "Users" },
];

export function AdminLayout() {
  return (
    <div className="mx-auto flex max-w-6xl gap-6 px-4 pt-6 sm:px-6">
      <aside className="hidden w-48 shrink-0 sm:block">
        <nav className="sticky top-24 space-y-1">
          {NAV_ITEMS.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.end}
              className={({ isActive }) =>
                cn(
                  "block rounded-lg px-3 py-2 text-sm font-medium transition-colors",
                  isActive
                    ? "bg-accent-soft text-accent-foreground"
                    : "text-muted-foreground hover:bg-muted hover:text-ink"
                )
              }
            >
              {item.label}
            </NavLink>
          ))}
        </nav>
      </aside>
      <div className="min-w-0 flex-1">
        <Outlet />
      </div>
    </div>
  );
}
