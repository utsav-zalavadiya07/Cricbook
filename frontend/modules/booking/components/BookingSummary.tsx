interface BookingSummaryProps {
  turfId: number;
  slotId?: number;
  slots?: { slotId: number; startTime: string; endTime: string; price: number }[];
  date: string;
  price: number;
  startTime?: string;
  endTime?: string;
  turfName?: string;
}

const Row = ({ label, value }: { label: string; value: React.ReactNode }) => (
  <div className="flex items-center justify-between gap-4 border-b border-white/5 py-3 last:border-0">
    <span className="theme-muted text-sm">{label}</span>
    <span className="theme-text text-sm font-medium">{value}</span>
  </div>
);

export default function BookingSummary({
  turfId, slotId, slots = [], date, price, startTime, endTime, turfName,
}: BookingSummaryProps) {
  const formattedDate = date
    ? new Date(date).toLocaleDateString("en-IN", { weekday: "long", year: "numeric", month: "long", day: "numeric" })
    : "-";

  return (
    <div className="theme-panel rounded-2xl p-6 space-y-1">
      <div className="flex items-center gap-3 mb-4">
        <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-emerald-500/20">
          <span className="text-lg text-emerald-500">C</span>
        </div>
        <div>
          <h3 className="theme-title text-base font-bold">Booking Summary</h3>
          <p className="theme-soft text-xs">Review before confirming</p>
        </div>
      </div>

      <Row label="Turf" value={turfName ?? `Turf #${turfId}`} />
      {slots.length > 0 ? (
        <Row
          label="Selected Slots"
          value={
            <div className="space-y-1 text-right">
              {slots.map((slot) => (
                <div key={slot.slotId}>
                  {slot.startTime} - {slot.endTime} - Rs {slot.price}
                </div>
              ))}
            </div>
          }
        />
      ) : startTime && endTime && (
        <Row label="Time Slot" value={`${startTime} - ${endTime}`} />
      )}
      <Row label="Date" value={formattedDate} />
      <Row
        label={slots.length > 1 ? "Slot IDs" : "Slot ID"}
        value={slots.length ? slots.map((slot) => `#${slot.slotId}`).join(", ") : `#${slotId}`}
      />

      <div className="mt-4 flex items-center justify-between border-t border-white/10 pt-4">
        <span className="theme-text font-medium">Total Amount</span>
        <span className="text-2xl font-bold text-emerald-500 dark:text-emerald-400">Rs {price}</span>
      </div>
    </div>
  );
}
