export function PlaceholderPage({ title, description }) {
  return (
    <div className="mx-auto flex min-h-[50vh] max-w-2xl flex-col items-center justify-center gap-2 px-4 text-center">
      <h1 className="text-2xl font-semibold text-ink">{title}</h1>
      {description && <p className="text-muted-foreground">{description}</p>}
      <p className="mt-2 text-sm text-muted-foreground">This page is coming up in a later build step.</p>
    </div>
  );
}
