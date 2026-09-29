"use client";

import { type ReactNode, useMemo } from "react";
import { motion, type Transition } from "framer-motion";
import { usePathname } from "next/navigation";
import { useReducedMotion } from "./use-reduced-motion";

interface PageTransitionProps {
  children: ReactNode;
  className?: string;
  transition?: Transition;
}

/**
 * PageTransition — wraps page content and animates on route change.
 * Uses the pathname as the key to trigger the animation on every navigation.
 */
export function PageTransition({ children, className, transition }: PageTransitionProps) {
  const pathname = usePathname();
  const prefersReduced = useReducedMotion();

  const defaultTransition = useMemo<Transition>(
    () => ({
      type: "tween",
      ease: [0.25, 0.46, 0.45, 0.94],
      duration: 0.35,
    }),
    [],
  );

  if (prefersReduced) {
    return <div className={className}>{children}</div>;
  }

  return (
    <motion.div
      key={pathname}
      initial={{ opacity: 0, y: 12 }}
      animate={{ opacity: 1, y: 0 }}
      transition={transition ?? defaultTransition}
      className={className}
    >
      {children}
    </motion.div>
  );
}