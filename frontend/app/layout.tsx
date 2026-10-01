import type { Metadata } from "next";

import "./globals.css";
import { Providers } from "@/components/providers";

export const metadata: Metadata = {
  title: {
    default: "Trace.run — Build workflows. Let AI run the work.",
    template: "%s · Trace.run",
  },
  description:
    "Trace.run is a visual automation platform for building, executing, and monitoring intelligent AI workflows.",
  metadataBase: new URL("http://localhost:3000"),

  icons: {
    icon: "/logo-mark.svg",
    shortcut: "/logo-mark.svg",
    apple: "/logo-mark.svg",
  },
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html
      lang="en"
      suppressHydrationWarning
      style={{ backgroundColor: "#050505" }}
    >
      <head>
        <link
          rel="preload"
          as="image"
          href="https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?auto=format&fit=crop&w=2400&q=88"
        />
      </head>

      <body
        className="min-h-screen font-sans text-foreground antialiased"
        style={{ backgroundColor: "#050505" }}
      >
        <Providers>{children}</Providers>
      </body>
    </html>
  );
}