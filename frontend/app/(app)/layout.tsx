import { AppGuard } from "@/components/app/app-guard";
import { AppShell } from "@/components/app/app-shell";
import { PageTransition } from "@/components/motion/page-transition";

export default function AppLayout({ children }: { children: React.ReactNode }) {
  return (
    <AppGuard>
      <AppShell>
        <PageTransition>{children}</PageTransition>
      </AppShell>
    </AppGuard>
  );
}
