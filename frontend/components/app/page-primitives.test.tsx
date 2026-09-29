import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import {
  AppStateSurface,
  MetricStrip,
} from "@/components/app/page-primitives";

describe("MetricStrip", () => {
  it("renders every metric label, value, and accessible status detail", () => {
    render(
      <MetricStrip
        items={[
          {
            label: "Published",
            value: 4,
            detail: "Active",
            tone: "success",
          },
        ]}
      />,
    );

    expect(screen.getByText("Published")).toBeInTheDocument();
    expect(screen.getByText("4")).toBeInTheDocument();
    expect(screen.getByText("Active")).toBeInTheDocument();
  });
});

describe("AppStateSurface", () => {
  it("keeps empty state copy and an optional recovery action visible", () => {
    render(
      <AppStateSurface
        kind="empty"
        title="No workflows"
        description="Create one to begin."
        action={<button type="button">Create workflow</button>}
      />,
    );

    expect(screen.getByText("No workflows")).toBeInTheDocument();
    expect(
      screen.getByRole("button", {
        name: "Create workflow",
      }),
    ).toBeInTheDocument();
  });
});
