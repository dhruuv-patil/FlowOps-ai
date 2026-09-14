"use client";

import { motion } from "framer-motion";
import { cn } from "@/lib/utils";

interface AnimatedGradientProps {
  className?: string;
  children?: React.ReactNode;
  speed?: number;
}

export function AnimatedGradient({
  className,
  children,
  speed = 8,
}: AnimatedGradientProps) {
  return (
    <motion.div
      className={cn(
        "relative overflow-hidden rounded-cards bg-brand-gradient bg-[length:300%_300%] animate-gradient-shift",
        className,
      )}
      animate={{
        backgroundPosition: ["0% 50%", "100% 50%", "0% 50%"],
      }}
      transition={{ duration: speed, ease: "linear", repeat: Infinity }}
    >
      {children && (
        <div className="relative z-10">{children}</div>
      )}
    </motion.div>
  );
}

/**
 * Text gradient component for headlines.
 */
export function GradientText({
  children,
  className,
}: { children: React.ReactNode; className?: string }) {
  return (
    <span className={cn("bg-brand-gradient bg-clip-text text-transparent", className)}>
      {children}
    </span>
  );
}

/**
 * Border gradient — a gradient border using a pseudo-element.
 */
export function GradientBorder({
  children,
  className,
  radius = "rounded-cards",
}: { children: React.ReactNode; className?: string; radius?: string }) {
  return (
    <div className={cn("relative", radius, className)}>
      <div className="absolute inset-0 -z-10 bg-brand-gradient opacity-0 group-hover:opacity-100 transition-opacity" style={{ borderRadius: "inherit" }} />
      <div className="relative bg-card/90 backdrop-blur-sm" style={{ borderRadius: "inherit" }}>
        {children}
      </div>
    </div>
  );
}