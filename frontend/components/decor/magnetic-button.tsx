"use client";

import { useRef, useState, type ReactNode } from "react";
import { motion, useMotionValue, useSpring, useTransform } from "framer-motion";
import { cn } from "@/lib/utils";

interface MagneticButtonProps {
  children: ReactNode;
  className?: string;
  strength?: number;
  onClick?: () => void;
}

/**
 * Magnetic primary CTA — the button is drawn toward the cursor on hover,
 * with a subtle 3D tilt for depth. Pure Framer Motion, no WebGL.
 */
export function MagneticButton({
  children,
  className,
  strength = 0.3,
  onClick,
}: MagneticButtonProps) {
  const ref = useRef<HTMLButtonElement>(null);

  const x = useMotionValue(0);
  const y = useMotionValue(0);

  const springX = useSpring(x, { stiffness: 300, damping: 30 });
  const springY = useSpring(y, { stiffness: 300, damping: 30 });

  const rotateX = useTransform(springY, [-20, 20], [4, -4]);
  const rotateY = useTransform(springX, [-20, 20], [-4, 4]);

  const [hovering, setHovering] = useState(false);

  function onMouseMove(e: React.MouseEvent<HTMLButtonElement>) {
    const rect = ref.current?.getBoundingClientRect();
    if (!rect) return;
    const cx = rect.left + rect.width / 2;
    const cy = rect.top + rect.height / 2;
    x.set((e.clientX - cx) * strength);
    y.set((e.clientY - cy) * strength);
  }

  function reset() {
    x.set(0);
    y.set(0);
    setHovering(false);
  }

  return (
    <motion.button
      ref={ref}
      type="button"
      onClick={onClick}
      onMouseMove={onMouseMove}
      onMouseEnter={() => setHovering(true)}
      onMouseLeave={reset}
      style={{
        x: springX,
        y: springY,
        rotateX: hovering ? rotateX : 0,
        rotateY: hovering ? rotateY : 0,
      }}
      className={cn(
        "inline-flex items-center justify-center gap-2 rounded-large-buttons bg-brand-gradient px-7 py-3 text-base font-medium tracking-tight text-white shadow-glow transition-[box-shadow,filter] hover:brightness-110 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-500/60 focus-visible:ring-offset-2 focus-visible:ring-offset-background disabled:pointer-events-none disabled:opacity-40",
        className,
      )}
    >
      {children}
    </motion.button>
  );
}