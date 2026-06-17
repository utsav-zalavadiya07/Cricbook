import type { HTMLAttributes, ReactNode } from "react";

interface CardProps extends HTMLAttributes<HTMLDivElement> {
  children: ReactNode;
  variant?: "default" | "strong" | "soft";
}

const variantClasses = {
  default: "theme-panel",
  strong: "theme-panel-strong",
  soft: "theme-panel-soft",
};

export function Card({ children, variant = "default", className = "", ...props }: CardProps) {
  return (
    <div className={`${variantClasses[variant]} rounded-2xl ${className}`} {...props}>
      {children}
    </div>
  );
}
