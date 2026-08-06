import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { RouterProvider } from "react-router-dom";
import { TooltipProvider } from "@/components/ui/tooltip";
import { Toaster } from "@/components/ui/sonner";
import { AuthProvider } from "@/context/AuthContext";
import { ErrorBoundary } from "@/components/layout/ErrorBoundary";
import { router } from "@/router";

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: false,
      staleTime: 30_000,
      // "online" mode pauses failed requests indefinitely if the browser thinks it's
      // offline instead of surfacing an error — we want real API failures (backend down,
      // 500s, etc.) to always resolve to an error state so the UI can show it.
      networkMode: "always",
    },
    mutations: {
      networkMode: "always",
    },
  },
});

function App() {
  return (
    <ErrorBoundary>
      <QueryClientProvider client={queryClient}>
        <AuthProvider>
          <TooltipProvider>
            <RouterProvider router={router} />
            <Toaster />
          </TooltipProvider>
        </AuthProvider>
      </QueryClientProvider>
    </ErrorBoundary>
  );
}

export default App;
