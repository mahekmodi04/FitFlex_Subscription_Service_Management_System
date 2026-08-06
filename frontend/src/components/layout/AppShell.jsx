import { Suspense } from "react";
import { Outlet } from "react-router-dom";
import { Loader2 } from "lucide-react";
import { Navbar } from "./Navbar";

function RouteFallback() {
  return (
    <div className="flex min-h-[50vh] items-center justify-center">
      <Loader2 className="size-6 animate-spin text-accent" />
    </div>
  );
}

export function AppShell() {
  return (
    <div className="flex min-h-screen flex-col bg-surface-muted">
      <Navbar />
      <main className="flex-1">
        <Suspense fallback={<RouteFallback />}>
          <Outlet />
        </Suspense>
      </main>
      <footer className="border-t border-border py-6 text-center text-sm text-muted-foreground">
        <p>FitFlex — Train on your terms.</p>
        <p className="mt-1 text-xs">made with 🧡 by Mahek ✨</p>
      </footer>
    </div>
  );
}
