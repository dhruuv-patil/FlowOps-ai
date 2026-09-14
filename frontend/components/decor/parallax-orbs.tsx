"use client";

import { motion, useScroll, useTransform } from "framer-motion";
import { cn } from "@/lib/utils";

interface ParallaxOrbsProps {
  className?: string;
  count?: number;
}

const ORB_CONFIG = [
  { size: 400, x: "-15%", y: "-15%", color: "rgba(129,140,248,0.12)", depth: 0.3 },
  { size: 300, x: "85%", y: "15%", color: "rgba(167,139,250,0.10)", depth: 0.2 },
  { size: 500, x: "25%", y: "75%", color: "rgba(99,102,241,0.08)", depth: 0.15 },
  { size: 250, x: "65%", y: "55%", color: "rgba(129,140,248,0.08)", depth: 0.25 },
];

/**
 * Blurred gradient orbs that drift subtly as the page scrolls — a cheap,
 * dependency-light parallax depth layer for hero sections.
 */
export function ParallaxOrbs({ className, count = 3 }: ParallaxOrbsProps) {
  const { scrollY } = useScroll();
  const visible = ORB_CONFIG.slice(0, count);

  return (
    <div
      className={cn(
        "pointer-events-none absolute inset-0 -z-10 overflow-hidden",
        className,
      )}
      aria-hidden="true"
    >
      {visible.map((orb, i) => {
        const travel = orb.size * orb.depth * 0.5;
        const y = useTransform(scrollY, [0, 800], [0, travel]);
        const x = useTransform(scrollY, [0, 800], [0, travel * 0.6]);

        return (
          <motion.div
            key={i}
            className="absolute rounded-full"
            style={{
              width: orb.size,
              height: orb.size,
              left: orb.x,
              top: orb.y,
              x,
              y,
              background: `radial-gradient(circle at 30% 30%, ${orb.color}, transparent 70%)`,
              filter: "blur(60px)",
            }}
          />
        );
      })}
    </div>
  );
}