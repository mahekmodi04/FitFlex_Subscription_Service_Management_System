import { Component } from "react";
import { Button } from "@/components/ui/button";

export class ErrorBoundary extends Component {
  state = { hasError: false };

  static getDerivedStateFromError() {
    return { hasError: true };
  }

  componentDidCatch(error, info) {
    console.error("Unhandled render error:", error, info);
  }

  render() {
    if (this.state.hasError) {
      return (
        <div className="mx-auto flex min-h-screen max-w-lg flex-col items-center justify-center gap-3 px-4 text-center">
          <h1 className="font-display text-2xl font-semibold text-ink">Something went wrong</h1>
          <p className="text-muted-foreground">
            An unexpected error occurred. Try refreshing the page.
          </p>
          <Button onClick={() => window.location.reload()}>Refresh</Button>
        </div>
      );
    }
    return this.props.children;
  }
}
