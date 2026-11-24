import { describe, expect, it } from "vitest";
import { isTraceName, parseTraceListing } from "../server/traceFetchPlugin";

describe("isTraceName", () => {
  it("accepts robot log and trace names", () => {
    expect(isTraceName("MyAuto-1712345678901.run.jsonl")).toBe(true);
    expect(isTraceName("latest.trace.json")).toBe(true);
  });

  it("rejects paths and other extensions", () => {
    for (const name of [
      "../x.run.jsonl",
      "a/b.run.jsonl",
      "x.sh",
      "x; rm.run.jsonl",
      "",
    ]) {
      expect(isTraceName(name)).toBe(false);
    }
    expect(isTraceName(null)).toBe(false);
  });
});

describe("parseTraceListing", () => {
  const listing = [
    "total 12",
    "-rw-rw---- 1 root sdcard_rw 1200 2026-11-24 10:01 Old-1700000000000.run.jsonl",
    "-rw-rw---- 1 root sdcard_rw 5400 2026-11-24 10:09 New-1700000500000.run.jsonl",
    "-rw-rw---- 1 root sdcard_rw   99 2026-11-24 10:09 notes.txt",
    "-rw-rw---- 1 root sdcard_rw  300 2025-01-01 09:00 latest.trace.json",
  ].join("\n");

  it("lists traces newest first and skips other files", () => {
    const traces = parseTraceListing(listing);
    expect(traces.map((trace) => trace.name)).toEqual([
      "New-1700000500000.run.jsonl",
      "Old-1700000000000.run.jsonl",
      "latest.trace.json",
    ]);
    expect(traces[0]).toMatchObject({ bytes: 5400, savedAt: 1700000500000 });
    expect(traces[2].savedAt).toBeNull();
  });
});
