import { describe, expect, it } from "vitest";
import { telemetryAt } from "../src/sim/simData";
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

const HEADER = JSON.stringify({
  kind: "run",
  version: 2,
  name: "TestAuto",
  startedAt: 1,
  columns: [
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
  ],
  modes: MODES,
});

function runLog(lines: unknown[], tail = "\n") {
  return (
    [HEADER, ...lines.map((line) => JSON.stringify(line))].join("\n") + tail
  );
}

const FOLLOWING = [
  [10, 0, 0, 0, 30, 40, 0, 0, 0, 0, 0],
  [10.5, 10, 0, 0, 30, 40, 0, 0.5, 0, 0, 0],
  [11, 20, 0, 0, 0, 0, 0, 1, 1, -1, 0],
];

describe("parseRun with a streamed robot log", () => {
  it("reads rows and starts time at the first FOLLOW sample", () => {
    const result = parseRun(runLog([IDLE(9), IDLE(9.5), ...FOLLOWING]));
    expect(result.ok).toBe(true);
    if (!result.ok) return;
    expect(result.sim.assumptions.source).toBe("robot log");
    expect(result.sim.duration).toBeCloseTo(1, 5);
    expect(result.sim.samples.x.at(-1)).toBeCloseTo(20, 5);
  });

  it("shifts telemetry times by the first FOLLOW time", () => {
    const result = parseRun(
      runLog([
        { t: 1, tel: { Pose: "init" } },
        IDLE(9),
        ...FOLLOWING,
        { t: 10.5, tel: { Pose: "mid" } },
      ]),
    );
    expect(result.ok).toBe(true);
    if (!result.ok) return;
    expect(result.sim.telemetry?.Pose.t).toEqual([-9, 0.5]);
    expect(telemetryAt(result.sim, 0)).toEqual({ Pose: "init" });
    expect(telemetryAt(result.sim, 0.5)).toEqual({ Pose: "mid" });
  });

  it("skips a cut-off last line with a warning", () => {
    const text = runLog([...FOLLOWING]) + "[11.5,30,0,0,0,0,0,1,1,-1,0";
    const result = parseRun(text);
    expect(result.ok).toBe(true);
    if (!result.ok) return;
    expect(result.sim.warnings.join(" ")).toContain("ends mid-line");
    expect(result.sim.duration).toBeCloseTo(1, 5);
  });

  it("names the line of a bad middle line", () => {
    const text =
      [HEADER, "[10,0", JSON.stringify(FOLLOWING[0])].join("\n") + "\n";
    const result = parseRun(text);
    expect(result.ok).toBe(false);
    if (!result.ok) expect(result.error).toContain("Line 2");
  });

  it("still reads an old columnar trace", () => {
    const result = parseRun(JSON.stringify(trace([...FOLLOWING])));
    expect(result.ok).toBe(true);
    if (result.ok) expect(result.sim.telemetry).toBeUndefined();
  });
});

describe("telemetryAt", () => {
  const sim = {
    telemetry: {
      Pose: { t: [1, 4], v: ["a", "b"] },
      Late: { t: [5], v: ["x"] },
    },
  } as unknown as Parameters<typeof telemetryAt>[0];

  it("holds the last value until the next change", () => {
    expect(telemetryAt(sim, 3).Pose).toBe("a");
    expect(telemetryAt(sim, 4).Pose).toBe("b");
  });

  it("leaves out keys that have no value yet", () => {
    expect(telemetryAt(sim, 0.5)).toEqual({});
    expect(telemetryAt(sim, 4)).not.toHaveProperty("Late");
  });
});
