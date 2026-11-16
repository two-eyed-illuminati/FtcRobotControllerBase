import { describe, expect, it } from "vitest";
import { parseRun, parseTrace, TRACE_DT } from "../src/sim/trace";

const MODES = ["FOLLOW", "HOLD", "MANUAL", "IDLE"];

function trace(rows: number[][], extra: Record<string, unknown> = {}) {
  const names = [
    "t",
    "x",
    "y",
    "heading",
    "vx",
    "vy",
    "omega",
    "closestT",
    "mode",
    "pathIndex",
    "group",
  ];
  return {
    kind: "trace",
    version: 1,
    name: "TestAuto",
    modes: MODES,
    samples: Object.fromEntries(
      names.map((name, c) => [name, rows.map((row) => row[c])]),
    ),
    ...extra,
  };
}

// t, x, y, heading, vx, vy, omega, closestT, mode, pathIndex, group
const IDLE = (t: number) => [t, 5, 5, 0, 0, 0, 0, 1, 3, -1, -1];

describe("parseTrace", () => {
  it("starts time at the first FOLLOW sample", () => {
    const result = parseTrace(
      trace([
        IDLE(10),
        IDLE(10.5),
        [11, 0, 0, 0, 30, 40, 0, 0, 0, 0, 0],
        [11.5, 10, 0, 0, 30, 40, 0, 0.5, 0, 0, 0],
        [12, 20, 0, 0, 0, 0, 0, 1, 1, -1, 0],
      ]),
    );
    expect(result.ok).toBe(true);
    if (!result.ok) return;
    expect(result.sim.duration).toBeCloseTo(1, 5);
    expect(result.sim.samples.x[0]).toBe(0);
    expect(result.sim.samples.x.at(-1)).toBeCloseTo(20, 5);
    expect(result.sim.samples.t[1]).toBeCloseTo(TRACE_DT, 9);
  });

  it("resamples uneven loop times to a fixed step", () => {
    const result = parseTrace(
      trace([
        [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0],
        [0.013, 1.3, 0, 0, 100, 0, 0, 0.1, 0, 0, 0],
        [0.05, 5, 0, 0, 100, 0, 0, 0.5, 0, 0, 0],
      ]),
    );
    expect(result.ok).toBe(true);
    if (!result.ok) return;
    expect(result.sim.samples.t).toHaveLength(6);
    expect(result.sim.samples.x[3]).toBeCloseTo(3, 5);
    expect(result.sim.samples.speed[3]).toBeCloseTo(100, 5);
  });

  it("interpolates the heading across the wrap", () => {
    const result = parseTrace(
      trace([
        [0, 0, 0, Math.PI - 0.1, 0, 0, 0, 0, 0, 0, 0],
        [0.02, 0, 0, -Math.PI + 0.1, 0, 0, 0, 0, 0, 0, 0],
      ]),
    );
    expect(result.ok).toBe(true);
    if (!result.ok) return;
    expect(Math.abs(result.sim.samples.heading[1])).toBeCloseTo(Math.PI, 5);
  });

  it("marks HOLD samples and warns about a long loop", () => {
    const result = parseTrace(
      trace([
        [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0],
        [0.3, 0, 0, 0, 0, 0, 0, 1, 1, -1, 0],
        [0.4, 0, 0, 0, 0, 0, 0, 1, 1, -1, 0],
      ]),
    );
    expect(result.ok).toBe(true);
    if (!result.ok) return;
    expect(result.sim.samples.holding[40]).toBe(1);
    expect(result.sim.warnings.join(" ")).toContain("300 ms");
  });

  it("warns when the trace never follows", () => {
    const result = parseTrace(trace([IDLE(0), IDLE(0.1), IDLE(0.2)]));
    expect(result.ok).toBe(true);
    if (result.ok) expect(result.sim.warnings[0]).toContain("never entered");
  });

  it("reports a missing column", () => {
    const raw = trace([IDLE(0), IDLE(1)]);
    delete (raw.samples as Record<string, unknown>).vx;
    const result = parseTrace(raw);
    expect(result.ok).toBe(false);
    if (!result.ok) expect(result.error).toContain('"vx"');
  });
});

describe("parseRun", () => {
  it("sends a trace to the trace reader", () => {
    const text = JSON.stringify(
      trace([IDLE(0), [1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0], IDLE(2)]),
    );
    const result = parseRun(text);
    expect(result.ok).toBe(true);
    if (result.ok) expect(result.sim.assumptions.source).toBe("robot trace");
  });

  it("rejects text that is not JSON", () => {
    expect(parseRun("nope").ok).toBe(false);
  });
});
