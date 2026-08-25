import Link from "next/link";

import { Logo } from "@/components/brand/logo";
import { ThemeToggle } from "@/components/theme-toggle";
import { FlowLines } from "@/components/marketing/flow-lines";

/**
 * The split auth layout: brand + flowing line-art on the left, the auth card on
 * the right (contract §6 UX). On mobile the left panel collapses and only the
 * card shows.
 */
export default function AuthLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="relative flex min-h-screen flex-col lg:flex-row">
      {/* Left: brand panel */}
      <div className="relative hidden overflow-hidden border-r border-border/60 bg-card/40 lg:flex lg:w-1/2 lg:flex-col lg:justify-between lg:p-12">
        <FlowLines className="opacity-70" />
        <div className="relative">
          <Link href="/">
            <Logo />
          </Link>
        </div>
        <div className="relative max-w-md">
          <h2 className="text-balance text-4xl font-semibold tracking-tight">
            Build workflows.{" "}
            <span className="text-primary">Let AI run the work.</span>
          </h2>
          <p className="mt-4 text-pretty text-muted-foreground">
            The visual automation platform for building, executing, and
            monitoring intelligent workflows — end to end.
          </p>
        </div>
        <p className="relative font-mono text-xs uppercase tracking-widest text-muted-foreground">
          Trigger → Process → AI → Decision → Action
        </p>
      </div>

      {/* Right: form panel */}
      <div className="relative flex flex-1 flex-col">
        <header className="flex items-center justify-between p-6 lg:justify-end">
          <Link href="/" className="lg:hidden">
            <Logo />
          </Link>
          <ThemeToggle />
        </header>
        <main className="flex flex-1 items-center justify-center px-6 pb-16">
          <div className="w-full max-w-sm">{children}</div>
        </main>
      </div>
    </div>
  );
}
