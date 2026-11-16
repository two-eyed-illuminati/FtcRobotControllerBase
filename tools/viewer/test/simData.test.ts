import { describe, expect, it } from "vitest";
import { SAMPLE_COLUMNS, parseSim, sampleAt } from "../src/sim/simData";

function simText(overrides: Record<string, unknown> = {}) {
  const columns = Object.fromEntries(
    SAMPLE_COLUMNS.map((name) => [name, [0, 0]]),
  );
  Object.assign(columns, {
    t: [0, 0.01],
    x: [0, 10],
    y: [0, 20],
    heading: [Math.PI - 0.1, -Math.PI + 0.1],
    speed: [0, 8],
    braking: [0, 1],
    segment: [0, 1],
  });
  return JSON.stringify({
    ok: true,
    dt: 0.01,
    duration: 0.01,
    summary: {
      maxSpeed: 8,
      endSpeed: 0,
      endPositionError: 0,
      endHeadingError: 0,
    },
    samples: columns,
    ...overrides,
  });
}

describe("parseSim", () => {
  it("reads a valid file", () => {
    const result = parseSim(simText());
    expect(result.ok).toBe(true);
    if (result.ok) expect(result.sim.samples.x).toEqual([0, 10]);
  });

  it("reports a failed simulation with its message", () => {
    const result = parseSim('{"ok":false,"error":"Simulation failed: boom"}');
    expect(result).toEqual({ ok: false, error: "Simulation failed: boom" });
  });

  it("rejects invalid JSON, missing columns and uneven columns", () => {
    expect(parseSim("{nope").ok).toBe(false);
    expect(parseSim(simText({ samples: {} })).ok).toBe(false);
    const uneven = JSON.parse(simText());
    uneven.samples.x = [0];
    expect(parseSim(JSON.stringify(uneven)).ok).toBe(false);
  });
});

describe("sampleAt", () => {
  const parsed = parseSim(simText());
  if (!parsed.ok) throw new Error(parsed.error);
  const sim = parsed.sim;

  it("interpolates position and speed", () => {
    const sample = sampleAt(sim, 0.005);
    expect(sample.x).toBeCloseTo(5);
    expect(sample.y).toBeCloseTo(10);
    expect(sample.speed).toBeCloseTo(4);
  });

  it("interpolates heading the short way across +-pi", () => {
    expect(Math.abs(sampleAt(sim, 0.005).heading)).toBeCloseTo(Math.PI);
  });

  it("takes flags from the earlier sample and clamps time", () => {
    expect(sampleAt(sim, 0.005).braking).toBe(false);
    expect(sampleAt(sim, 5).x).toBe(10);
    expect(sampleAt(sim, -1).x).toBe(0);
  });
});
