interface PaymentSummaryProps {
  amount: number;
  regularAmount?: number;
  bookingId: number;
  bookingIds?: number[];
  turfName?: string;
  date?: string;
  startTime?: string;
  endTime?: string;
  paymentMethod?: string;
}

const Row = ({ label, value }: { label: string; value: React.ReactNode }) => (
  <div className="flex items-center justify-between gap-4 border-b border-white/5 py-3 last:border-0">
    <span className="theme-muted text-sm">{label}</span>
    <span className="theme-text text-right text-sm font-medium">{value}</span>
  </div>
);

export default function PaymentSummary({
  amount, regularAmount, bookingId, bookingIds = [bookingId], turfName, date, startTime, endTime, paymentMethod = "Razorpay",
}: PaymentSummaryProps) {
  const formattedDate = date
    ? new Date(date).toLocaleDateString("en-IN", {
        weekday: "long", year: "numeric", month: "long", day: "numeric",
      })
    : "-";

  return (
    <div className="theme-panel rounded-2xl p-6 space-y-1">
      <div className="flex items-center gap-3 mb-4">
        <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-violet-500/20">
          <span className="text-lg">Pay</span>
        </div>
        <div>
          <h3 className="theme-title text-base font-bold">Payment Details</h3>
          <p className="theme-soft text-xs">
            {bookingIds.length > 1 ? `${bookingIds.length} bookings` : `Booking #${bookingId}`}
          </p>
        </div>
      </div>

      {turfName && <Row label="Turf" value={turfName} />}
      {startTime && endTime && <Row label="Time Slot" value={`${startTime} - ${endTime}`} />}
      {date && <Row label="Date" value={formattedDate} />}
      {bookingIds.length > 1 && <Row label="Booking IDs" value={bookingIds.map((id) => `#${id}`).join(", ")} />}
      <Row label="Payment Method" value={paymentMethod} />
      {regularAmount != null && regularAmount > amount && (
        <>
          <Row label="Regular slot total" value={`₹${regularAmount.toLocaleString("en-IN")}`} />
          <Row label="Membership discount" value={<span className="font-bold text-emerald-400">−₹{(regularAmount - amount).toLocaleString("en-IN")}</span>} />
        </>
      )}

      <div className="mt-4 flex items-center justify-between border-t border-white/10 pt-4">
        <span className="theme-text font-medium">Total Payable</span>
        <span className="text-2xl font-bold text-emerald-500 dark:text-emerald-400">₹{amount.toLocaleString("en-IN")}</span>
      </div>

      <div className="flex items-center gap-2 pt-2">
        <span className="theme-soft text-xs">Secured by Razorpay - 256-bit SSL</span>
      </div>
    </div>
  );
}
