"use client";

import { motion } from "framer-motion";
import {
  Bot,
  GitBranch,
  Plug,
  Shield,
  Workflow,
  Zap,
} from "lucide-react";
import { cn } from "@/lib/utils";

const ICONS = [
  { Icon: Workflow, delay: 0, x: -20, y: -10 },
  { Icon: Bot, delay: 0.5, x: 25, y: -25 },
  { Icon: Zap, delay: 1, x: -30, y: 15 },
  { Icon: GitBranch, delay: 1.5, x: 15, y: 20 },
  { Icon: Plug, delay: 2, x: -10, y: -30 },
  { Icon: Shield, delay: 2.5, x: 30, y: 10 },
];

interface FloatingIconsProps {
  className?: string;
  count?: number;
  speed?: number;
  intensity?: number;
}

export function FloatingIcons({
  className,
  count = 6,
  speed = 6,
  intensity = 24,
}: FloatingIconsProps) {
  const iconsToShow = ICONS.slice(0, count);

  return (
    <div
      className={cn(
        "pointer-events-none absolute inset-0 overflow-hidden",
        className,
      )}
      aria-hidden="true"
    >
      {iconsToShow.map(({ Icon, delay, x, y }, i) => (
        <motion.div
          key={i}
          initial={false}
          animate={{
            x: [x, -x * 0.5, x],
            y: [y, -y * 0.5, y],
            rotate: [-3, 3, -3],
          }}
          transition={{
            duration: speed + i * 1.5,
            delay,
            repeat: Infinity,
            ease: "easeInOut",
          }}
          style={{
            left: `calc(50% + ${x}px)`,
            top: `calc(50% + ${y}px)`,
          }}
        >
          <div className="flex size-10 items-center justify-center rounded-xl bg-brand-500/10 backdrop-blur-sm border border-brand-500/20">
            <Icon className="size-5 text-brand-500" />
          </div>
        </motion.div>
      ))}
    </div>
  );
}

/**
 * Simpler version for auth panels and smaller spaces.
 */
export function FloatingIconsSmall({
  className,
  count = 4,
}: { className?: string; count?: number } = {}) {
  const iconsToShow = ICONS.slice(0, count);

  return (
    <div
      className={cn(
        "pointer-events-none absolute inset-0 overflow-hidden opacity-60",
        className,
      )}
      aria-hidden="true"
    >
      {iconsToShow.map(({ Icon, delay, x, y }, i) => (
        <motion.div
          key={i}
          initial={false}
          animate={{
            x: [x * 0.3, -x * 0.15, x * 0.3],
            y: [y * 0.3, -y * 0.15, y * 0.3],
          }}
          transition={{
            duration: 8 + i * 1.5,
            delay,
            repeat: Infinity,
            ease: "easeInOut",
          }}
          style={{
            left: `calc(50% + ${x * 0.4}px)`,
            top: `calc(50% + ${y * 0.4}px)`,
          }}
        >
          <Icon className="size-6 text-brand-500/40" />
        </motion.div>
      ))}
    </div>
  );
}