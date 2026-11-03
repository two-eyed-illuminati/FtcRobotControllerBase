import {
  mkdirSync,
  mkdtempSync,
  rmSync,
  utimesSync,
  writeFileSync,
} from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { afterEach, beforeEach, describe, expect, it } from "vitest";
import {
  AUTO_DIR,
  EXPORTS_DIR,
  autosMessage,
  listAutos,
  ppFileFor,
  readSelected,
  writeSelected,
} from "../server/autos";
import { PathFeed } from "../src/state/viewer.svelte";

const FACTORY = "public static AutoBuilder createRoutine(Follower f) {}";

let root: string;

function addSource(name: string, text: string, time = Date.now() / 1000) {
  const file = join(root, AUTO_DIR, name);
  mkdirSync(join(file, ".."), { recursive: true });
  writeFileSync(file, text);
  utimesSync(file, time, time);
}

function addExport(name: string, time: number) {
  const file = ppFileFor(root, name);
  writeFileSync(file, "{}");
  utimesSync(file, time, time);
}

beforeEach(() => {
  root = mkdtempSync(join(tmpdir(), "autos-test-"));
  mkdirSync(join(root, EXPORTS_DIR), { recursive: true });
});

afterEach(() => rmSync(root, { recursive: true, force: true }));

describe("listAutos", () => {
  it("lists only top-level classes with a createRoutine factory", () => {
    addSource("RedAuto.java", FACTORY);
    addSource("BlueAuto.java", FACTORY);
    addSource("Helper.java", "class Helper {}");
    addSource("utils/Nested.java", FACTORY);
    expect(listAutos(root)).toEqual(["BlueAuto", "RedAuto"]);
  });

  it("returns an empty list when auto/ is missing", () => {
    expect(listAutos(root)).toEqual([]);
  });
});

describe("selection", () => {
  it("falls back to ExampleAuto, then the first auto", () => {
    addSource("RedAuto.java", FACTORY);
    expect(readSelected(root)).toBe("RedAuto");
    addSource("ExampleAuto.java", FACTORY);
    expect(readSelected(root)).toBe("ExampleAuto");
  });

  it("keeps a valid choice and ignores a deleted class", () => {
    addSource("ExampleAuto.java", FACTORY);
    addSource("RedAuto.java", FACTORY);
    writeSelected(root, "RedAuto");
    expect(readSelected(root)).toBe("RedAuto");
    writeSelected(root, "Gone");
    expect(readSelected(root)).toBe("ExampleAuto");
  });
});

describe("autosMessage", () => {
  it("marks exported, stale, failed and missing autos", () => {
    const now = Date.now() / 1000;
    addSource("Fresh.java", FACTORY, now - 100);
    addSource("Old.java", FACTORY, now - 100);
    addSource("Never.java", FACTORY, now - 100);
    addSource("Broken.java", FACTORY, now - 100);
    addSource("Newest.java", FACTORY, now);
    addExport("Fresh", now + 10);
    addExport("Old", now - 50);
    writeFileSync(`${ppFileFor(root, "Broken")}.error`, "boom");

    const byName = Object.fromEntries(
      autosMessage(root, false).autos.map((a) => [a.name, a]),
    );
    expect(byName.Fresh).toMatchObject({ stale: false, failed: false });
    expect(byName.Old).toMatchObject({ stale: true });
    expect(byName.Never).toMatchObject({ exportedAt: null, stale: false });
    expect(byName.Broken).toMatchObject({ failed: true });
  });

  it("reports the exporting marker and the lock", () => {
    addSource("ExampleAuto.java", FACTORY);
    writeFileSync(join(root, EXPORTS_DIR, ".exporting"), "ExampleAuto");
    expect(autosMessage(root, true)).toMatchObject({
      exporting: "ExampleAuto",
      selected: "ExampleAuto",
      locked: true,
    });
  });
});

describe("PathFeed autos", () => {
  it("stores the pushed auto list", () => {
    const feed = new PathFeed();
    feed.applyAutos({
      autos: [{ name: "A", exportedAt: 1, stale: false, failed: false }],
      selected: "A",
      exporting: null,
      locked: false,
    });
    expect(feed.autos.selected).toBe("A");
  });
});
