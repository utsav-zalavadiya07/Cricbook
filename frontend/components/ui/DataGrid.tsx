import type { ReactNode } from "react";

export type DataGridColumn<T> = {
  key: string;
  header: string;
  render: (row: T) => ReactNode;
  mobileLabel?: string;
  className?: string;
  mobileClassName?: string;
};

type DataGridProps<T> = {
  columns: DataGridColumn<T>[];
  rows: T[];
  getRowKey: (row: T) => string | number;
  gridTemplateColumns?: string;
  emptyState?: ReactNode;
  className?: string;
};

export function DataGrid<T>({
  columns,
  rows,
  getRowKey,
  gridTemplateColumns,
  emptyState,
  className = "",
}: DataGridProps<T>) {
  if (rows.length === 0) {
    return <>{emptyState}</>;
  }

  const templateColumns = gridTemplateColumns ?? `repeat(${columns.length}, minmax(150px, 1fr))`;

  return (
    <div className={`overflow-hidden rounded-3xl border border-[var(--theme-border)] bg-[var(--theme-card)] shadow-2xl shadow-black/5 ${className}`}>
      <div className="hidden overflow-x-auto xl:block">
        <div className="min-w-max">
          <div
            className="grid border-b border-[var(--theme-border)] bg-[var(--theme-panel-soft)]"
            style={{ gridTemplateColumns: templateColumns }}
          >
            {columns.map((column) => (
              <div
                key={column.key}
                className={`px-4 py-4 text-left text-[11px] font-black uppercase tracking-[0.18em] text-[var(--theme-soft)] ${column.className ?? ""}`}
              >
                {column.header}
              </div>
            ))}
          </div>

          <div className="divide-y divide-[var(--theme-border)]">
            {rows.map((row) => (
              <div
                key={getRowKey(row)}
                className="grid items-center transition hover:bg-emerald-500/[0.04]"
                style={{ gridTemplateColumns: templateColumns }}
              >
                {columns.map((column) => (
                  <div key={column.key} className={`min-w-0 px-4 py-4 ${column.className ?? ""}`}>
                    {column.render(row)}
                  </div>
                ))}
              </div>
            ))}
          </div>
        </div>
      </div>

      <div className="grid gap-3 p-3 xl:hidden">
        {rows.map((row) => (
          <article key={getRowKey(row)} className="theme-panel-soft rounded-2xl p-4">
            <div className="grid gap-4 sm:grid-cols-2">
              {columns.map((column) => (
                <div key={column.key} className={`min-w-0 ${column.mobileClassName ?? ""}`}>
                  <p className="theme-soft text-[11px] font-black uppercase tracking-[0.16em]">{column.mobileLabel ?? column.header}</p>
                  <div className="mt-1 text-sm">{column.render(row)}</div>
                </div>
              ))}
            </div>
          </article>
        ))}
      </div>
    </div>
  );
}
