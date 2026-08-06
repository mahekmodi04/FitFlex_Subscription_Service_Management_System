import { Link } from "react-router-dom";
import { Button } from "@/components/ui/button";

export default function NotFound() {
  return (
    <div className="mx-auto flex min-h-[60vh] max-w-lg flex-col items-center justify-center gap-3 px-4 text-center">
      <p className="font-display text-6xl font-bold text-accent">404</p>
      <h1 className="font-display text-2xl font-semibold text-ink">Page not found</h1>
      <p className="text-muted-foreground">The page you&apos;re looking for doesn&apos;t exist.</p>
      <Button asChild className="mt-2">
        <Link to="/">Back home</Link>
      </Button>
    </div>
  );
}
