import { describe, expect, it } from "vitest";
import { SAMPLE_COLUMNS } from "../src/sim/simData";
import { PathFeed, Playback } from "../src/state/viewer.svelte";

function runText(duration: number) {
  const dt = 0.01;
  const count = Math.round(duration / dt) + 1;
  const columns = Object.fromEntries(
    SAMPLE_COLUMNS.map((name) => [
      name,
      Array.from({ length: count }, () => 0),
    ]),
  );
  columns.t = Array.from({ length: count }, (_, i) => i * dt);
  return JSON.stringify({
    ok: true,
    dt,
    duration,
    summary: {
      maxSpeed: 0,
      endSpeed: 0,
      endPositionError: 0,
      endHeadingError: 0,
    },
    samples: columns,
  });
}

describe("loaded runs", () => {
  it("adds runs with distinct colors and ids", () => {
    const feed = new PathFeed();
    feed.addRun("a.json", runText(1));
    feed.addRun("b.json", runText(2));
    expect(feed.runs.map((run) => run.id)).toEqual([1, 2]);
    expect(feed.runs[0].color).not.toBe(feed.runs[1].color);
    expect(feed.runError).toBeNull();
  });

  it("rejects a bad file and keeps the existing runs", () => {
    const feed = new PathFeed();
    feed.addRun("a.json", runText(1));
    feed.addRun("bad.json", "not json");
    expect(feed.runs).toHaveLength(1);
    expect(feed.runError).toContain("bad.json");
  });

  it("removes a run by id", () => {
    const feed = new PathFeed();
    feed.addRun("a.json", runText(1));
    feed.addRun("b.json", runText(2));
    feed.removeRun(1);
    expect(feed.runs.map((run) => run.label)).toEqual(["b.json"]);
  });

  it("plays for as long as the longest visible run", () => {
    const feed = new PathFeed();
    const playback = new Playback(feed);
    feed.addRun("a.json", runText(1));
    feed.addRun("b.json", runText(3));
    expect(playback.duration).toBe(3);
    feed.runs[1].visible = false;
    expect(playback.duration).toBe(1);
  });
});
