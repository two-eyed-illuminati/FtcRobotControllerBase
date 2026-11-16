import { describe, expect, it } from "vitest";
import { PROJECT_VERSION, parseProject } from "../src/io";
import { DEFAULT_ROBOT } from "./support/fixtures";

const minimal = {
  startPoint: { x: 0, y: 0, headingDeg: 0 },
  lines: [],
};

function parse(project: object) {
  return parseProject(JSON.stringify(project), DEFAULT_ROBOT);
}

describe("parseProject", () => {
  it("rejects text that isn't JSON", () => {
    expect(parseProject("{not json", DEFAULT_ROBOT)).toMatchObject({
      ok: false,
    });
  });

  it("rejects JSON without a start point or paths", () => {
    expect(parse({ lines: [] })).toMatchObject({ ok: false });
    expect(parse({ startPoint: minimal.startPoint })).toMatchObject({
      ok: false,
    });
  });

  it("warns about files from a newer version but still loads them", () => {
    const result = parse({ ...minimal, version: "99.0.0" });
    expect(result.ok && result.warnings).toHaveLength(1);
  });

  it("doesn't warn about current or older files", () => {
    for (const version of [PROJECT_VERSION, "1.5.0", undefined]) {
      const result = parse({ ...minimal, version });
      expect(result.ok && result.warnings).toEqual([]);
    }
  });

  it("falls back when a file doesn't record it", () => {
    const result = parse(minimal);
    expect(result.ok && result.doc.robot).toEqual(DEFAULT_ROBOT);
  });

  it("falls back per key for missing or invalid values", () => {
    const result = parse({
      ...minimal,
      version: PROJECT_VERSION,
      robot: { rLength: 20, rWidth: "wide", forwardVelocity: null },
    });
    expect(result.ok && result.doc.robot).toEqual({
      ...DEFAULT_ROBOT,
      rLength: 20,
    });
  });

  it("ignores a settings block in a current file", () => {
    const result = parse({
      ...minimal,
      version: PROJECT_VERSION,
      robot: { rLength: 20 },
      settings: { rWidth: 30 },
    });
    expect(result.ok && result.doc.robot.rLength).toBe(20);
  });
});
