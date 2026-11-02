import { describe, expect, it } from "vitest";
import { fixtures, goldenPath, loadProject } from "./support/fixtures";

describe.each(fixtures)("loading $name", ({ name, text }) => {
  it("normalizes to the golden document", async () => {
    const doc = loadProject(text);
    await expect(JSON.stringify(doc, null, 2) + "\n").toMatchFileSnapshot(
      goldenPath(name, "normalized.json"),
    );
  });
});
