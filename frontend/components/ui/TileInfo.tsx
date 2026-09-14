"use client";
import { usePathname } from "next/navigation";
import type { ReactNode } from "react";

type TileInfoProps = {
  children: ReactNode;
  label?: string;
  align?: "left" | "right";
  focusable?: boolean;
  className?: string;
  variant?: "standard" | "cricket";
};

export function TileInfo({
  children,
  label = "More information",
  align = "right",
  focusable = true,
  className = "",
  variant = "cricket",
}: TileInfoProps) {
  const pathname = usePathname() || "";
  const isOwner = pathname.startsWith("/owner");
  const iconColor = isOwner ? "text-sky-500" : "text-emerald-500";

  const placement =
    align === "left"
      ? "right-full top-1/2 -translate-y-1/2 mr-2"
      : "left-full top-1/2 -translate-y-1/2 ml-2";

  return (
    <span className={`group/tileinfo relative inline-flex ${className}`}>
      <span
        aria-label={label}
        className="inline-flex shrink-0 items-center justify-center outline-none transition-transform hover:scale-105"
        role="img"
        tabIndex={focusable ? 0 : undefined}
      >
        {variant === "standard" ? (
          <svg
            xmlns="http://www.w3.org/2000/svg"
            viewBox="0 0 100 100"
            className={`h-[1.25em] w-[1.25em] ${iconColor} drop-shadow-sm opacity-90 transition-all group-hover/tileinfo:scale-110 group-hover/tileinfo:opacity-100`}
          >
            <circle cx="50" cy="50" r="44" fill="none" stroke="currentColor" strokeWidth="7" />
            <text 
              x="50" 
              y="74" 
              fontFamily="Georgia, 'Times New Roman', serif" 
              fontStyle="italic" 
              fontWeight="900" 
              fontSize="70" 
              fill="currentColor" 
              textAnchor="middle"
            >
              i
            </text>
          </svg>
        ) : (
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100" width="1.25em" height="1.25em" className="drop-shadow-md">
            <defs>
              <linearGradient id="bgNavy" x1="0%" y1="0%" x2="0%" y2="100%">
                <stop offset="0%" stopColor="#0F172A" />
                <stop offset="100%" stopColor="#020617" />
              </linearGradient>
              <linearGradient id="borderGold" x1="0%" y1="0%" x2="100%" y2="100%">
                <stop offset="0%" stopColor="#FDE047" />
                <stop offset="50%" stopColor="#F59E0B" />
                <stop offset="100%" stopColor="#B45309" />
              </linearGradient>
              <linearGradient id="ballRed" x1="0%" y1="0%" x2="100%" y2="100%">
                <stop offset="0%" stopColor="#EF4444" />
                <stop offset="100%" stopColor="#B91C1C" />
              </linearGradient>
              <linearGradient id="stemWhite" x1="0%" y1="0%" x2="100%" y2="100%">
                <stop offset="0%" stopColor="#FFFFFF" />
                <stop offset="100%" stopColor="#E2E8F0" />
              </linearGradient>
            </defs>
  
            {/* Background Circle */}
            <circle cx="50" cy="50" r="46" fill="url(#bgNavy)" stroke="url(#borderGold)" strokeWidth="4" />
  
            {/* Golden Swooshes under the stem */}
            <path d="M 28 86 Q 48 88, 66 82" fill="none" stroke="url(#borderGold)" strokeWidth="3" strokeLinecap="round" />
            <path d="M 34 92 Q 48 93, 60 88" fill="none" stroke="url(#borderGold)" strokeWidth="2.5" strokeLinecap="round" />
  
            {/* Stem of 'i' */}
            <path d="M 35 40 C 40 40, 43 37, 45 34 L 56 34 L 52 66 C 52 71, 56 76, 64 78 L 32 82 C 40 78, 42 61, 40 46 Q 39 40, 35 40 Z" fill="url(#stemWhite)" />
  
            {/* Ball (Dot of 'i') */}
            <circle cx="50" cy="24" r="12" fill="url(#ballRed)" stroke="url(#borderGold)" strokeWidth="3" />
            
            {/* Ball Seam */}
            <g transform="rotate(45, 50, 24)">
              <path d="M 43 14 Q 48 24, 43 34" fill="none" stroke="#FFFFFF" strokeWidth="1.5" strokeDasharray="2 1.5" />
              <path d="M 57 14 Q 52 24, 57 34" fill="none" stroke="#FFFFFF" strokeWidth="1.5" strokeDasharray="2 1.5" />
            </g>
          </svg>
        )}
      </span>
      <span
        className={`pointer-events-none absolute ${placement} z-50 hidden w-max max-w-[min(18rem,calc(100vw-3rem))] rounded-xl border border-slate-200 bg-white px-3 py-2 text-left text-xs font-semibold leading-5 text-slate-600 shadow-xl group-hover/tileinfo:block group-focus-within/tileinfo:block dark:border-white/10 dark:bg-slate-950 dark:text-slate-300`}
        role="tooltip"
      >
        {children}
      </span>
    </span>
  );
}
