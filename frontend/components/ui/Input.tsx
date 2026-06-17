"use client";

import type { InputHTMLAttributes, TextareaHTMLAttributes } from "react";

const baseInputClass =
  "theme-input w-full rounded-xl px-4 py-3 text-sm outline-none transition focus:border-emerald-500 focus:ring-1 focus:ring-emerald-500 disabled:cursor-not-allowed disabled:opacity-60";

export function Input({ className = "", ...props }: InputHTMLAttributes<HTMLInputElement>) {
  return <input className={`${baseInputClass} ${className}`} {...props} />;
}

export function Textarea({ className = "", ...props }: TextareaHTMLAttributes<HTMLTextAreaElement>) {
  return <textarea className={`${baseInputClass} resize-none ${className}`} {...props} />;
}
