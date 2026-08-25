"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useState } from "react";
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
} from "lucide-react";
import { toast } from "sonner";

import { getErrorMessage, logout } from "@/lib/api";
import { useAuthStore } from "@/lib/auth-store";
import { cn, initialsFrom } from "@/lib/utils";
import { Button } from "@/components/ui/button";
import { LogoMark } from "@/components/brand/logo";
import { ThemeToggle } from "@/components/theme-toggle";
import { OrgSwitcher } from "@/components/app/org-switcher";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";

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

// `ready` marks the routes M1 actually serves. Everything else lands in later
// milestones and is shown with a "Beta" pill and a disabled affordance, rather
// than a dead link that 404s — honesty over fake breadth.
const NAV: NavGroup[] = [
  {
    label: "Workspace",
    items: [
      { label: "Dashboard", href: "/dashboard", icon: LayoutDashboard, ready: true },
      { label: "Workflows", href: "/workflows", icon: Workflow, ready: true },
      { label: "Executions", href: "/executions", icon: Activity },
      { label: "Templates", href: "/templates", icon: PanelsTopLeft },
    ],
  },
  {
    label: "Automation",
    items: [
      { label: "AI Agents", href: "/agents", icon: Bot },
      { label: "Integrations", href: "/integrations", icon: Plug },
      { label: "Logs", href: "/logs", icon: FileText },
    ],
  },
  {
    label: "Organization",
    items: [
      { label: "Team", href: "/team", icon: Users },
      { label: "Settings", href: "/settings", icon: Settings },
    ],
  },
];

export function AppShell({ children }: { children: React.ReactNode }) {
  const [collapsed, setCollapsed] = useState(false);

  return (
    <div className="flex min-h-screen">
      <Sidebar collapsed={collapsed} onToggle={() => setCollapsed((c) => !c)} />
      <div className="flex min-w-0 flex-1 flex-col">
        <Topbar />
        <main className="flex-1 overflow-y-auto px-6 py-6 lg:px-8">{children}</main>
      </div>
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
        "sticky top-0 flex h-screen shrink-0 flex-col border-r border-border/60 bg-card/40 transition-[width]",
        collapsed ? "w-16" : "w-64",
      )}
    >
      <div className="flex h-16 items-center gap-2 px-3">
        <Link href="/dashboard" className="flex items-center gap-2">
          <LogoMark />
          {!collapsed && (
            <span className="text-lg font-semibold tracking-tight">FlowOps</span>
          )}
        </Link>
        <Button
          variant="ghost"
          size="icon"
          className="ml-auto"
          onClick={onToggle}
          aria-label={collapsed ? "Expand sidebar" : "Collapse sidebar"}
        >
          <ChevronLeft
            className={cn("transition-transform", collapsed && "rotate-180")}
          />
        </Button>
      </div>

      <div className="px-3 pb-2">
        <OrgSwitcher collapsed={collapsed} />
      </div>

      <nav className="flex-1 space-y-4 overflow-y-auto px-3 py-2">
        {NAV.map((group) => (
          <div key={group.label}>
            {!collapsed && (
              <p className="px-2 pb-1 font-mono text-[10px] uppercase tracking-widest text-muted-foreground">
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
                    <Icon className="size-4 shrink-0" />
                    {!collapsed && (
                      <>
                        <span className="flex-1 truncate">{item.label}</span>
                        {!item.ready && (
                          <span className="rounded bg-muted px-1.5 py-0.5 text-[10px] font-medium text-muted-foreground">
                            Beta
                          </span>
                        )}
                      </>
                    )}
                  </>
                );
                const base =
                  "flex items-center gap-2 rounded-md px-2 py-2 text-sm transition-colors";
                return (
                  <li key={item.href}>
                    {item.ready ? (
                      <Link
                        href={item.href}
                        title={collapsed ? item.label : undefined}
                        className={cn(
                          base,
                          active
                            ? "bg-accent font-medium text-accent-foreground"
                            : "text-muted-foreground hover:bg-accent/60 hover:text-foreground",
                        )}
                      >
                        {inner}
                      </Link>
                    ) : (
                      <span
                        title={collapsed ? `${item.label} (coming soon)` : undefined}
                        aria-disabled="true"
                        className={cn(
                          base,
                          "cursor-not-allowed text-muted-foreground/50",
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

      <UserBlock collapsed={collapsed} />
    </aside>
  );
}

function Topbar() {
  return (
    <header className="sticky top-0 z-30 flex h-16 items-center gap-4 border-b border-border/60 bg-background/80 px-6 backdrop-blur lg:px-8">
      <button
        type="button"
        disabled
        className="flex flex-1 items-center gap-2 rounded-md border border-border bg-card px-3 py-1.5 text-sm text-muted-foreground/70 lg:max-w-md"
        title="Global search — coming soon"
      >
        <Search className="size-4" />
        <span>Search…</span>
        <kbd className="ml-auto rounded border border-border bg-muted px-1.5 font-mono text-[10px]">
          ⌘K
        </kbd>
      </button>
      <ThemeToggle />
    </header>
  );
}

function UserBlock({ collapsed }: { collapsed: boolean }) {
  const router = useRouter();
  const user = useAuthStore((s) => s.user);
  const role = useAuthStore((s) => s.currentRole);

  if (!user) return null;

  async function onLogout() {
    try {
      await logout();
    } catch (err) {
      // logout() clears local state in its finally block regardless.
      toast.error(getErrorMessage(err));
    } finally {
      router.replace("/login");
    }
  }

  return (
    <div className="border-t border-border/60 p-3">
      <DropdownMenu>
        <DropdownMenuTrigger
          className={cn(
            "flex w-full items-center gap-2 rounded-md px-2 py-2 text-left text-sm transition-colors hover:bg-accent focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring",
            collapsed && "justify-center px-0",
          )}
        >
          <span className="flex size-8 shrink-0 items-center justify-center rounded-full bg-primary/15 text-xs font-semibold text-primary">
            {initialsFrom(user.fullName)}
          </span>
          {!collapsed && (
            <span className="min-w-0 flex-1">
              <span className="block truncate font-medium">{user.fullName}</span>
              <span className="block truncate text-xs text-muted-foreground">
                {role}
              </span>
            </span>
          )}
        </DropdownMenuTrigger>
        <DropdownMenuContent align="end" side="top" className="w-56">
          <DropdownMenuLabel className="font-normal">
            <span className="block truncate font-medium">{user.fullName}</span>
            <span className="block truncate text-xs text-muted-foreground">
              {user.email}
            </span>
          </DropdownMenuLabel>
          <DropdownMenuSeparator />
          <DropdownMenuItem
            onSelect={(e) => {
              e.preventDefault();
              void onLogout();
            }}
            className="gap-2 text-destructive focus:text-destructive"
          >
            <LogOut className="size-4" />
            Log out
          </DropdownMenuItem>
        </DropdownMenuContent>
      </DropdownMenu>
    </div>
  );
}
