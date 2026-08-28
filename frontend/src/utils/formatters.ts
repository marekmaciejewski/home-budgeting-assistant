const amountFormatter = new Intl.NumberFormat("en-US", {
  minimumFractionDigits: 2,
  maximumFractionDigits: 2
});

const timeFormatter = new Intl.DateTimeFormat("en-US", {
  dateStyle: "medium",
  timeStyle: "short"
});

export function formatAmount(value: number): string {
  return amountFormatter.format(value);
}

export function formatTimestamp(value: string): string {
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : timeFormatter.format(date);
}

export function formatShortHash(value: string): string {
  return value.length <= 17 ? value : `${value.slice(0, 8)}...${value.slice(-8)}`;
}
