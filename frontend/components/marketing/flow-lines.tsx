"use client";

import { useEffect, useState } from "react";
import { cn } from "@/lib/utils";

/**
 * Animated flowing-line background art for auth panels and hero sections.
 * Gradient strokes with subtle particle drift and pulsing junction.
 */
export function FlowLines({
  className,
  animated = true,
}: {
  className?: string;
  animated?: boolean;
}) {
  const [mounted, setMounted] = useState(false);

  useEffect(() => {
    setMounted(true);
  }, []);

  if (!animated) {
    return <FlowLinesStatic className={className} />;
  }

  return (
    <svg
      aria-hidden="true"
      viewBox="0 0 1200 800"
      preserveAspectRatio="xMidYMid slice"
      className={cn(
        "pointer-events-none absolute inset-0 h-full w-full select-none",
        className,
      )}
    >
      <defs>
        {/* Animated brand gradient for lines */}
        <linearGradient id="flowlines-brand" x1="0" y1="0" x2="1" y2="0">
          <stop offset="0%" stopColor="#818cf8" stopOpacity="0" />
          <stop offset="20%" stopColor="#818cf8" stopOpacity="0.5" />
          <stop offset="50%" stopColor="#6366f1" stopOpacity="0.7" />
          <stop offset="80%" stopColor="#a78bfa" stopOpacity="0.5" />
          <stop offset="100%" stopColor="#a78bfa" stopOpacity="0" />

          <animate
            attributeName="x1"
            values="0;1;0"
            dur="12s"
            repeatCount="indefinite"
          />
          <animate
            attributeName="x2"
            values="1;2;1"
            dur="12s"
            repeatCount="indefinite"
          />
        </linearGradient>

        {/* Junction glow pulse */}
        <radialGradient id="flowlines-junction-glow">
          <stop offset="0%" stopColor="#818cf8" stopOpacity="0.4" />
          <stop offset="60%" stopColor="#6366f1" stopOpacity="0.1" />
          <stop offset="100%" stopColor="#a78bfa" stopOpacity="0" />

          <animate
            attributeName="r"
            values="0.5;1;0.5"
            dur="3s"
            repeatCount="indefinite"
          />
        </radialGradient>

        {/* Particle for drifting along paths */}
        <circle
          id="flow-particle"
          r="2"
          fill="#818cf8"
          opacity="0.8"
        />
      </defs>

      {/* Soft bloom behind the convergence point */}
      <circle
        cx="330"
        cy="470"
        r="200"
        fill="url(#flowlines-junction-glow)"
        filter="blur(40px)"
      />

      {/* Flowing paths */}
      <g
        fill="none"
        stroke="url(#flowlines-brand)"
        strokeWidth="1.5"
        strokeLinecap="round"
        strokeLinejoin="round"
      >
        {/* Converging from upper-left */}
        <path d="M -40 250 C 150 250, 220 470, 330 470" />
        <path d="M -40 330 C 170 330, 240 470, 330 470" />

        {/* Spine */}
        <path d="M -40 470 H 330" />

        {/* Converging from lower-left */}
        <path d="M -40 610 C 170 610, 240 470, 330 470" />
        <path d="M -40 690 C 150 690, 220 470, 330 470" />

        {/* Fanning back out to the right */}
        <path d="M 330 470 C 520 470, 600 210, 820 210" />
        <path d="M 330 470 C 540 470, 640 350, 900 350" />
        <path d="M 330 470 H 1240" />
        <path d="M 330 470 C 540 470, 640 600, 900 600" />
        <path d="M 330 470 C 520 470, 600 730, 820 730" />
      </g>

      {/* Junction and endpoint nodes */}
      <g fill="#818cf8">
        <circle
          cx="330"
          cy="470"
          r="6"
          opacity="0.9"
        >
          <animate
            attributeName="r"
            values="6;8;6"
            dur="2.5s"
            repeatCount="indefinite"
          />
          <animate
            attributeName="opacity"
            values="0.9;1;0.9"
            dur="2.5s"
            repeatCount="indefinite"
          />
        </circle>

        <circle cx="352" cy="470" r="4" opacity="0.6" />
        <circle cx="820" cy="210" r="3" opacity="0.5" />
        <circle cx="900" cy="350" r="3" opacity="0.5" />
        <circle cx="900" cy="600" r="3" opacity="0.5" />
        <circle cx="820" cy="730" r="3" opacity="0.5" />
      </g>

      {/* Drifting particles along key paths */}
      {mounted && (
        <>
          {/* Particle on spine */}
          <animateMotion
            path="M -40 470 H 330"
            dur="4s"
            repeatCount="indefinite"
            rotate="auto"
          >
            <use href="#flow-particle" />
          </animateMotion>

          {/* Particle on upper branch */}
          <animateMotion
            path="M -40 250 C 150 250, 220 470, 330 470"
            dur="5s"
            repeatCount="indefinite"
            begin="0.5s"
            rotate="auto"
          >
            <use href="#flow-particle" />
          </animateMotion>

          {/* Particle on lower branch */}
          <animateMotion
            path="M -40 690 C 150 690, 220 470, 330 470"
            dur="5s"
            repeatCount="indefinite"
            begin="1s"
            rotate="auto"
          >
            <use href="#flow-particle" />
          </animateMotion>

          {/* Particle on fan-out top */}
          <animateMotion
            path="M 330 470 C 520 470, 600 210, 820 210"
            dur="6s"
            repeatCount="indefinite"
            begin="1.5s"
            rotate="auto"
          >
            <use href="#flow-particle" />
          </animateMotion>

          {/* Particle on fan-out bottom */}
          <animateMotion
            path="M 330 470 C 520 470, 600 730, 820 730"
            dur="6s"
            repeatCount="indefinite"
            begin="2s"
            rotate="auto"
          >
            <use href="#flow-particle" />
          </animateMotion>
        </>
      )}
    </svg>
  );
}

/**
 * Static fallback for reduced motion / SSR.
 */
function FlowLinesStatic({ className }: { className?: string }) {
  return (
    <svg
      aria-hidden="true"
      viewBox="0 0 1200 800"
      preserveAspectRatio="xMidYMid slice"
      className={cn(
        "pointer-events-none absolute inset-0 h-full w-full select-none",
        className,
      )}
    >
      <defs>
        <linearGradient id="flowlines-fade" x1="0" y1="0" x2="1" y2="0">
          <stop offset="0%" stopColor="#818cf8" stopOpacity="0" />
          <stop offset="35%" stopColor="#818cf8" stopOpacity="0.4" />
          <stop offset="70%" stopColor="#6366f1" stopOpacity="0.25" />
          <stop offset="100%" stopColor="#a78bfa" stopOpacity="0" />
        </linearGradient>

        <radialGradient id="flowlines-glow">
          <stop offset="0%" stopColor="#818cf8" stopOpacity="0.3" />
          <stop offset="100%" stopColor="#818cf8" stopOpacity="0" />
        </radialGradient>
      </defs>

      <circle
        cx="330"
        cy="470"
        r="190"
        fill="url(#flowlines-glow)"
      />

      <g
        fill="none"
        stroke="url(#flowlines-fade)"
        strokeWidth="1.25"
        strokeLinecap="round"
      >
        <path d="M -40 250 C 150 250, 220 470, 330 470" />
        <path d="M -40 330 C 170 330, 240 470, 330 470" />
        <path d="M -40 470 H 330" />
        <path d="M -40 610 C 170 610, 240 470, 330 470" />
        <path d="M -40 690 C 150 690, 220 470, 330 470" />

        <path d="M 330 470 C 520 470, 600 210, 820 210" />
        <path d="M 330 470 C 540 470, 640 350, 900 350" />
        <path d="M 330 470 H 1240" />
        <path d="M 330 470 C 540 470, 640 600, 900 600" />
        <path d="M 330 470 C 520 470, 600 730, 820 730" />
      </g>

      <g fill="#818cf8">
        <circle
          cx="330"
          cy="470"
          r="5"
          opacity="0.7"
        />
        <circle
          cx="352"
          cy="470"
          r="3.5"
          opacity="0.4"
        />
        <circle
          cx="820"
          cy="210"
          r="3"
          opacity="0.3"
        />
        <circle
          cx="900"
          cy="350"
          r="3"
          opacity="0.3"
        />
        <circle
          cx="900"
          cy="600"
          r="3"
          opacity="0.3"
        />
        <circle
          cx="820"
          cy="730"
          r="3"
          opacity="0.3"
        />
      </g>
    </svg>
  );
}