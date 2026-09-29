import type { Config } from "tailwindcss";

const config: Config = {
  darkMode: ["class"],
  content: ["./app/**/*.{ts,tsx}", "./components/**/*.{ts,tsx}"],
  theme: {
    container: {
      center: true,
      padding: "2rem",
      screens: { "2xl": "1400px" },
    },
    extend: {
      colors: {
        /* Brand */
        brand: {
          400: "#a78bfa",
          500: "#818cf8",
          600: "#6366f1",
          700: "#4f46e5",
        },
        /* Surface palette */
        "surface-0": "#09090b",
        "surface-1": "#0f1114",
        "surface-2": "#16191d",
        "surface-3": "#1c2025",
        "bone-white": "#fafafa",
        "off-white": "#e4e4e7",
        "mute-gray": "#71717a",
        "wire-gray": "#27272a",
        "ghost-gray": "#18181b",
        "console-charcoal": "#09090b",
        "true-black": "#000000",
        "nav-ink": "#09090b",
        "recess-black": "#0f1114",
        smoke: "#1c2025",
        ash: "#52525b",

        /* shadcn/ui HSL mappings */
        border: "hsl(var(--border))",
        input: "hsl(var(--input))",
        ring: "hsl(var(--ring))",
        background: "hsl(var(--background))",
        foreground: "hsl(var(--foreground))",
        primary: {
          DEFAULT: "hsl(var(--primary))",
          foreground: "hsl(var(--primary-foreground))",
        },
        secondary: {
          DEFAULT: "hsl(var(--secondary))",
          foreground: "hsl(var(--secondary-foreground))",
        },
        destructive: {
          DEFAULT: "hsl(var(--destructive))",
          foreground: "hsl(var(--destructive-foreground))",
        },
        muted: {
          DEFAULT: "hsl(var(--muted))",
          foreground: "hsl(var(--muted-foreground))",
        },
        accent: {
          DEFAULT: "hsl(var(--accent))",
          foreground: "hsl(var(--accent-foreground))",
        },
        popover: {
          DEFAULT: "hsl(var(--popover))",
          foreground: "hsl(var(--popover-foreground))",
        },
        card: {
          DEFAULT: "hsl(var(--card))",
          foreground: "hsl(var(--card-foreground))",
        },
        success: {
          DEFAULT: "#22c55e",
          foreground: "#ffffff",
        },
        warning: {
          DEFAULT: "#f59e0b",
          foreground: "#ffffff",
        },
      },
      backgroundImage: {
        "brand-gradient": "var(--gradient-brand)",
        "brand-soft": "var(--gradient-brand-soft)",
        "aurora": "radial-gradient(40% 50% at 20% 20%, rgba(129,140,248,0.18), transparent 70%), radial-gradient(40% 50% at 80% 30%, rgba(167,139,250,0.16), transparent 70%), radial-gradient(50% 50% at 60% 80%, rgba(99,102,241,0.14), transparent 70%)",
      },
      borderRadius: {
        lg: "var(--radius)",
        md: "calc(var(--radius) - 2px)",
        sm: "calc(var(--radius) - 4px)",
        tags: "var(--radius-tags)",
        cards: "var(--radius-cards)",
        xl: "var(--radius-xl)",
        icons: "var(--radius-icons)",
        inputs: "var(--radius-inputs)",
        buttons: "var(--radius-buttons)",
        "large-buttons": "var(--radius-large-buttons)",
      },
      fontFamily: {
        sans: ["var(--font-geistsans)", "system-ui", "sans-serif"],
        mono: ["var(--font-geistmono)", "ui-monospace", "monospace"],
        geistsans: ["var(--font-geistsans)", "system-ui", "sans-serif"],
        geistmono: ["var(--font-geistmono)", "ui-monospace", "monospace"],
      },
      fontSize: {
        caption: ["var(--text-caption)", { lineHeight: "var(--leading-caption)", letterSpacing: "var(--tracking-caption)" }],
        "body-sm": ["var(--text-body-sm)", { lineHeight: "var(--leading-body-sm)", letterSpacing: "var(--tracking-body-sm)" }],
        body: ["var(--text-body)", { lineHeight: "var(--leading-body)", letterSpacing: "var(--tracking-body)" }],
        subheading: ["var(--text-subheading)", { lineHeight: "var(--leading-subheading)", letterSpacing: "var(--tracking-subheading)" }],
        heading: ["var(--text-heading)", { lineHeight: "var(--leading-heading)", letterSpacing: "var(--tracking-heading)" }],
        "heading-lg": ["var(--text-heading-lg)", { lineHeight: "var(--leading-heading-lg)", letterSpacing: "var(--tracking-heading-lg)" }],
        display: ["var(--text-display)", { lineHeight: "var(--leading-display)", letterSpacing: "var(--tracking-display)" }],
        "display-xl": ["var(--text-display-xl)", { lineHeight: "var(--leading-display-xl)", letterSpacing: "var(--tracking-display-xl)" }],
      },
      spacing: {
        /* Only override sizes that Tailwind doesn't provide by default */
        "80": "var(--spacing-80)",
        "112": "var(--spacing-112)",
        "160": "var(--spacing-160)",
      },
      boxShadow: {
        card: "var(--shadow-card)",
        "card-hover": "var(--shadow-card-hover)",
        glow: "var(--shadow-glow)",
        float: "var(--shadow-float)",
        subtle: "var(--shadow-subtle)",
        elevated: "var(--shadow-elevated)",
      },
      keyframes: {
        "accordion-down": {
          from: { height: "0" },
          to: { height: "var(--radix-accordion-content-height)" },
        },
        "accordion-up": {
          from: { height: "var(--radix-accordion-content-height)" },
          to: { height: "0" },
        },
        "flow-dash": {
          to: { strokeDashoffset: "-16" },
        },
        float: {
          "0%, 100%": { transform: "translateY(0)" },
          "50%": { transform: "translateY(-12px)" },
        },
        "float-slow": {
          "0%, 100%": { transform: "translateY(0) rotate(0deg)" },
          "50%": { transform: "translateY(-20px) rotate(2deg)" },
        },
        shimmer: {
          "100%": { transform: "translateX(100%)" },
        },
        "gradient-shift": {
          "0%, 100%": { backgroundPosition: "0% 50%" },
          "50%": { backgroundPosition: "100% 50%" },
        },
        "pulse-glow": {
          "0%, 100%": { opacity: "0.6", transform: "scale(1)" },
          "50%": { opacity: "1", transform: "scale(1.05)" },
        },
        rise: {
          from: { opacity: "0", transform: "translateY(16px)" },
          to: { opacity: "1", transform: "translateY(0)" },
        },
      },
      animation: {
        "accordion-down": "accordion-down 0.2s ease-out",
        "accordion-up": "accordion-up 0.2s ease-out",
        "flow-dash": "flow-dash 1s linear infinite",
        float: "float 6s ease-in-out infinite",
        "float-slow": "float-slow 9s ease-in-out infinite",
        shimmer: "shimmer 1.5s infinite",
        "gradient-shift": "gradient-shift 8s ease infinite",
        "pulse-glow": "pulse-glow 3s ease-in-out infinite",
        rise: "rise 0.5s ease-out both",
      },
    },
  },
  plugins: [require("tailwindcss-animate")],
};

export default config;
