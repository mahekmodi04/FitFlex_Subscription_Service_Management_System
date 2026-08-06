import { Progress } from "@/components/ui/progress";

export function UsageMeter({ label, used, total, unitName = "units" }) {
  const pct = total > 0 ? Math.min(100, Math.round((used / total) * 100)) : 0;
  const isExhausted = total > 0 && used >= total;

  return (
    <div>
      <div className="mb-1.5 flex items-baseline justify-between text-sm">
        <span className="font-medium text-ink">{label}</span>
        <span className={isExhausted ? "text-danger" : "text-muted-foreground"}>
          {used} / {total} {unitName}
        </span>
      </div>
      <Progress value={pct} className={isExhausted ? "[&>div]:bg-danger" : "[&>div]:bg-accent"} />
    </div>
  );
}
