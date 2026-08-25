import { clsx, type ClassValue } from "clsx";
import { twMerge } from "tailwind-merge";

/** Merge conditional class names, de-duplicating Tailwind utilities. */
export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

/**
 * Up to two uppercase initials for an avatar fallback.
 *
 * `avatarUrl` is always `null` in M1, so every avatar renders initials derived
 * from the display name. Uses `Array.from` so an astral-plane first character
 * (an emoji in a display name) is not split mid-surrogate-pair.
 */
export function initialsFrom(name: string, fallback = "?"): string {
  const words = name.trim().split(/\s+/).filter(Boolean);
  if (words.length === 0) return fallback;
  const letters = (words.length === 1 ? [words[0]] : [words[0], words[1]]).map(
    (word) => Array.from(word)[0] ?? "",
  );
  const initials = letters.join("").toUpperCase();
  return initials.length > 0 ? initials : fallback;
}

