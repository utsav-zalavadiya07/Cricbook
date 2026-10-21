type TaxInclusiveNoteProps = {
  className?: string;
};

export function TaxInclusiveNote({ className = "" }: TaxInclusiveNoteProps) {
  return (
    <span className={`inline-flex items-center text-[11px] font-semibold text-emerald-400 ${className}`}>
      Includes 18% GST
    </span>
  );
}
