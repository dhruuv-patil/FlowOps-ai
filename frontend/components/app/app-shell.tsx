"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import {
  Activity,
  Bot,
  ChevronLeft,
  FileText,
  LayoutDashboard,
  LogOut,
  PanelsTopLeft,
  Plug,
  Search,
  Settings,
  Users,
  Workflow,
  AlertTriangle,
} from "lucide-react";
import { toast } from "sonner";

import { getErrorMessage, logout } from "@/lib/api";
import { useAuthStore } from "@/lib/auth-store";
import { cn, initialsFrom } from "@/lib/utils";
import { Button } from "@/components/ui/button";
import { LogoMark } from "@/components/brand/logo";
import { OrgSwitcher } from "@/components/app/org-switcher";
import { NotificationsMenu } from "@/components/app/notifications-menu";
import { CommandPalette } from "@/components/app/command-palette";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { HoverLift } from "@/components/motion/motion-primitives";
import { PageTransition } from "@/components/motion/page-transition";

interface NavItem {
  label: string;
  href: string;
  icon: React.ComponentType<{ className?: string }>;
  ready?: boolean;
}

interface NavGroup {
  label: string;
  items: NavItem[];
}

const NAV: NavGroup[] = [
  {
    label: "Workspace",
    items: [
      {
        label: "Dashboard",
        href: "/dashboard",
        icon: LayoutDashboard,
        ready: true,
      },
      {
        label: "Workflows",
        href: "/workflows",
        icon: Workflow,
        ready: true,
      },
      {
        label: "Executions",
        href: "/executions",
        icon: Activity,
        ready: true,
      },
      {
        label: "Templates",
        href: "/templates",
        icon: PanelsTopLeft,
        ready: true,
      },
    ],
  },
  {
    label: "Reliability",
    items: [
      {
        label: "Anomalies",
        href: "/reliability",
        icon: AlertTriangle,
        ready: true,
      },
    ],
  },
  {
    label: "Automation",
    items: [
      {
        label: "AI Agents",
        href: "/agents",
        icon: Bot,
        ready: true,
      },
      {
        label: "Integrations",
        href: "/integrations",
        icon: Plug,
        ready: true,
      },
      {
        label: "Logs",
        href: "/logs",
        icon: FileText,
        ready: true,
      },
    ],
  },
  {
    label: "Organization",
    items: [
      {
        label: "Team",
        href: "/team",
        icon: Users,
        ready: true,
      },
      {
        label: "Settings",
        href: "/settings",
        icon: Settings,
        ready: true,
      },
    ],
  },
];

export function AppShell({
  children,
}: {
  children: React.ReactNode;
}) {
  const [collapsed, setCollapsed] = useState(false);
  const [paletteOpen, setPaletteOpen] = useState(false);

  useEffect(() => {
    function onKey(e: KeyboardEvent) {
      if (
        (e.metaKey || e.ctrlKey) &&
        e.key.toLowerCase() === "k"
      ) {
        const el = document.activeElement;

        const typing =
          el instanceof HTMLElement &&
          (el.tagName === "INPUT" ||
            el.tagName === "TEXTAREA" ||
            el.isContentEditable);

        if (typing) return;

        e.preventDefault();
        setPaletteOpen((v) => !v);
      }
    }

    document.addEventListener("keydown", onKey);

    return () =>
      document.removeEventListener("keydown", onKey);
  }, []);

  return (
    <div className="flex min-h-screen bg-[#050505]">
      <Sidebar
        collapsed={collapsed}
        onToggle={() => setCollapsed((c) => !c)}
      />

      <div className="flex min-w-0 flex-1 flex-col">
        <Topbar
          onOpenPalette={() => setPaletteOpen(true)}
        />

        <main className="flex-1 overflow-y-auto px-5 py-5 lg:px-6 lg:py-6">
          <PageTransition>{children}</PageTransition>
        </main>
      </div>

      <CommandPalette
        open={paletteOpen}
        onOpenChange={setPaletteOpen}
      />
    </div>
  );
}

function Sidebar({
  collapsed,
  onToggle,
}: {
  collapsed: boolean;
  onToggle: () => void;
}) {
  const pathname = usePathname();

  return (
    <aside
      className={cn(
        "sticky top-0 flex h-screen shrink-0 flex-col border-r border-white/[0.075] bg-[#0a0a0a] transition-[width] duration-200",
        collapsed ? "w-[68px]" : "w-60"
      )}
    >
      {/* =====================================================
          BRAND
          Expanded: logo + toggle on same row
          Collapsed: logo, then toggle underneath
         ===================================================== */}

      <div className="shrink-0 border-b border-white/[0.075]">
        <div
          className={cn(
            "flex items-center",
            collapsed
              ? "h-12 justify-center px-3"
              : "h-12 justify-between px-3"
          )}
        >
          <Link
            href="/dashboard"
            className="flex min-w-0 items-center gap-2"
          >
            <LogoMark className="!h-6 !w-6 shrink-0" />

            {!collapsed && (
              <span className="truncate text-sm font-semibold tracking-tight text-white/90">
                FlowOps
              </span>
            )}
          </Link>

          {/* Expanded: toggle beside logo */}
          {!collapsed && (
            <Button
              variant="ghost"
              size="icon"
              onClick={onToggle}
              aria-label="Collapse sidebar"
              className={cn(
                "!h-7 !w-7 shrink-0 rounded-lg",
                "border border-white/[0.08]",
                "bg-white/[0.025]",
                "text-white/40",
                "hover:bg-white/[0.07]",
                "hover:text-white/80"
              )}
            >
              <ChevronLeft className="size-3.5" />
            </Button>
          )}
        </div>

        {/* Collapsed: toggle directly underneath logo */}
        {collapsed && (
          <div className="flex h-9 items-center justify-center pb-1">
            <Button
              variant="ghost"
              size="icon"
              onClick={onToggle}
              aria-label="Expand sidebar"
              className={cn(
                "!h-7 !w-7 rounded-lg",
                "border border-white/[0.08]",
                "bg-white/[0.025]",
                "text-white/40",
                "hover:bg-white/[0.07]",
                "hover:text-white/80"
              )}
            >
              <ChevronLeft className="size-3.5 rotate-180" />
            </Button>
          </div>
        )}
      </div>

      {/* =====================================================
          ORGANIZATION
          Keep this visible only in expanded mode.
         ===================================================== */}

      {!collapsed && (
        <div className="shrink-0 border-b border-white/[0.075] px-3 py-2.5">
          <OrgSwitcher collapsed={false} />
        </div>
      )}

      {/* =====================================================
          NAVIGATION
         ===================================================== */}

      <nav className="flex-1 space-y-5 overflow-y-auto px-2.5 py-3">
        {NAV.map((group) => (
          <div key={group.label}>
            {!collapsed && (
              <p className="px-2.5 pb-1.5 mono-eyebrow">
                {group.label}
              </p>
            )}

            <ul className="space-y-0.5">
              {group.items.map((item) => {
                const active =
                  item.ready &&
                  (pathname === item.href ||
                    pathname.startsWith(`${item.href}/`));

                const Icon = item.icon;

                const inner = (
                  <>
                    <Icon className="size-4 shrink-0 opacity-60" />

                    {!collapsed && (
                      <>
                        <span className="flex-1 truncate text-[13px]">
                          {item.label}
                        </span>

                        {!item.ready && (
                          <span className="rounded-full border border-white/[0.08] bg-white/[0.04] px-1.5 py-0.5 text-[9px] font-medium text-white/40">
                            Beta
                          </span>
                        )}
                      </>
                    )}
                  </>
                );

                const base =
                  "relative flex items-center gap-2.5 rounded-lg px-2.5 py-2 text-[13px] font-medium transition-all duration-[180ms]";

                return (
                  <li key={item.href}>
                    {item.ready ? (
                      <HoverLift scale={1.01} y={-1}>
                        <Link
                          href={item.href}
                          title={
                            collapsed
                              ? item.label
                              : undefined
                          }
                          className={cn(
                            base,
                            collapsed &&
                              "justify-center px-2.5",
                            active
                              ? "bg-white/[0.06] text-white/90 before:absolute before:left-0 before:top-1/2 before:h-5 before:w-px before:-translate-y-1/2 before:rounded-r before:bg-white/80"
                              : "text-white/45 hover:bg-white/[0.04] hover:text-white/80"
                          )}
                        >
                          {inner}
                        </Link>
                      </HoverLift>
                    ) : (
                      <span
                        title={
                          collapsed
                            ? `${item.label} (coming soon)`
                            : undefined
                        }
                        aria-disabled="true"
                        className={cn(
                          base,
                          collapsed &&
                            "justify-center px-2.5",
                          "cursor-not-allowed text-white/20"
                        )}
                      >
                        {inner}
                      </span>
                    )}
                  </li>
                );
              })}
            </ul>
          </div>
        ))}
      </nav>

      {/* =====================================================
          USER
         ===================================================== */}

      <UserBlock collapsed={collapsed} />
    </aside>
  );
}

function Topbar({
  onOpenPalette,
}: {
  onOpenPalette: () => void;
}) {
  return (
    <header className="sticky top-0 z-30 flex h-12 items-center gap-4 border-b border-white/[0.075] bg-[#050505]/80 px-5 backdrop-blur-md lg:px-6">
      <button
        type="button"
        onClick={onOpenPalette}
        className="flex flex-1 items-center gap-2 rounded-lg border border-white/[0.08] bg-white/[0.03] px-3 py-1.5 text-[13px] text-white/40 transition-colors duration-[180ms] hover:bg-white/[0.06] hover:text-white/70 lg:max-w-md"
        title="Search and commands"
      >
        <Search className="size-3.5" />

        <span>Search…</span>

        <kbd className="ml-auto rounded-md border border-white/[0.10] bg-white/[0.04] px-1.5 py-0.5 font-mono text-[10px] text-white/30">
          ⌘K
        </kbd>
      </button>

      <NotificationsMenu />
    </header>
  );
}

function UserBlock({
  collapsed,
}: {
  collapsed: boolean;
}) {
  const router = useRouter();
  const user = useAuthStore((s) => s.user);
  const role = useAuthStore((s) => s.currentRole);

  if (!user) return null;

  async function onLogout() {
    try {
      await logout();
    } catch (err) {
      toast.error(getErrorMessage(err));
    } finally {
      router.replace("/login");
    }
  }

  return (
    <div className="border-t border-white/[0.075] p-2.5">
      <DropdownMenu>
        <DropdownMenuTrigger
          className={cn(
            "flex w-full items-center gap-2.5 rounded-lg px-2 py-1.5 text-left text-[13px] transition-colors duration-[180ms] hover:bg-white/[0.04] focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-white/[0.15]",
            collapsed && "justify-center px-0"
          )}
        >
          <span className="flex size-7 shrink-0 items-center justify-center rounded-full bg-white/[0.06] text-[10px] font-semibold text-white/70">
            {initialsFrom(user.fullName)}
          </span>

          {!collapsed && (
            <span className="min-w-0 flex-1">
              <span className="block truncate font-medium text-white/80">
                {user.fullName}
              </span>

              <span className="block truncate text-[11px] text-white/40">
                {role}
              </span>
            </span>
          )}
        </DropdownMenuTrigger>

        <DropdownMenuContent
          align="end"
          side="top"
          className="w-56"
        >
          <DropdownMenuLabel className="font-normal">
            <span className="block truncate font-medium text-white/80">
              {user.fullName}
            </span>

            <span className="block truncate text-xs text-white/40">
              {user.email}
            </span>
          </DropdownMenuLabel>

          <DropdownMenuSeparator />

          <DropdownMenuItem
            onSelect={(e) => {
              e.preventDefault();
              void onLogout();
            }}
            className="gap-2 text-red-400 focus:text-red-400"
          >
            <LogOut className="size-4" />
            Log out
          </DropdownMenuItem>
        </DropdownMenuContent>
      </DropdownMenu>
    </div>
  );
}