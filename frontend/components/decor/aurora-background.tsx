"use client";

import { motion } from "framer-motion";
import { cn } from "@/lib/utils";

interface AuroraBackgroundProps {
  className?: string;
  intensity?: "subtle" | "normal" | "strong";
  animated?: boolean;
}

export function AuroraBackground({
  className,
  intensity = "normal",
  animated = true,
}: AuroraBackgroundProps) {
  const opacity = {
    subtle: 0.06,
    normal: 0.12,
    strong: 0.18,
  }[intensity];

  if (!animated) {
    return (
      <div
        aria-hidden="true"
        className={cn(
          "pointer-events-none absolute inset-0 bg-aurora",
          className
        )}
        style={{ opacity }}
      />
    );
  }

  return (
    <motion.div
      aria-hidden="true"
      className={cn(
        "pointer-events-none absolute inset-0 bg-aurora",
        className
      )}
      style={{ opacity }}
      initial={false}
      animate={{
        backgroundPosition: [
          "0% 0%",
          "100% 100%",
          "0% 0%",
        ],
        opacity: [opacity, opacity * 1.35, opacity],
      }}
      transition={{
        duration: 20,
        ease: "linear",
        repeat: Infinity,
      }}
    />
  );
}

interface RadialGlowProps {
  className?: string;
  color?: string;
  size?: number;
}

export function RadialGlow({
  className,
  color = "rgba(129,140,248,0.25)",
  size = 300,
}: RadialGlowProps) {
  return (
    <div
      aria-hidden="true"
      className={cn(
        "pointer-events-none absolute -z-10 rounded-full blur-[80px]",
        className
      )}
      style={{
        width: size,
        height: size,
        background: `radial-gradient(circle at center, ${color}, transparent 70%)`,
      }}
    />
  );
}