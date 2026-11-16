import {
  existsSync,
  readdirSync,
  readFileSync,
  statSync,
  writeFileSync,
} from "node:fs";
import { join, resolve } from "node:path";

export const AUTO_DIR =
  "TeamCode/src/main/java/org/firstinspires/ftc/teamcode/auto";
export const EXPORTS_DIR = "TeamCode/build/exports";
export const SELECTED_FILE = ".selected";
export const EXPORTING_FILE = ".exporting";
export const DEFAULT_AUTO = "ExampleAuto";

const FACTORY = /static\s+AutoBuilder\s+createRoutine\s*\(/;

export type AutoStatus = {
  name: string;
  exportedAt: number | null;
  stale: boolean;
  failed: boolean;
};

export type AutosMessage = {
  autos: AutoStatus[];
  selected: string | null;
  exporting: string | null;
  locked: boolean;
};

const autoDirOf = (repoRoot: string) => resolve(repoRoot, AUTO_DIR);
const exportsDirOf = (repoRoot: string) => resolve(repoRoot, EXPORTS_DIR);

export const ppFileFor = (repoRoot: string, name: string) =>
  join(exportsDirOf(repoRoot), `${name}.pp`);

/** Classes directly in auto/ that define `static AutoBuilder createRoutine(...)`. */
export function listAutos(repoRoot: string): string[] {
  const dir = autoDirOf(repoRoot);
  if (!existsSync(dir)) return [];
  return readdirSync(dir, { withFileTypes: true })
    .filter((entry) => entry.isFile() && entry.name.endsWith(".java"))
    .filter((entry) =>
      FACTORY.test(readFileSync(join(dir, entry.name), "utf8")),
    )
    .map((entry) => entry.name.slice(0, -".java".length))
    .sort();
}

function readMarker(repoRoot: string, file: string): string | null {
  try {
    return (
      readFileSync(join(exportsDirOf(repoRoot), file), "utf8").trim() || null
    );
  } catch {
    return null;
  }
}

/** The chosen auto, or the default one when nothing valid was chosen yet. */
export function readSelected(repoRoot: string): string | null {
  const autos = listAutos(repoRoot);
  const chosen = readMarker(repoRoot, SELECTED_FILE);
  if (chosen && autos.includes(chosen)) return chosen;
  return autos.includes(DEFAULT_AUTO) ? DEFAULT_AUTO : (autos[0] ?? null);
}

export function writeSelected(repoRoot: string, name: string) {
  writeFileSync(join(exportsDirOf(repoRoot), SELECTED_FILE), name);
}

function newestSourceMtime(repoRoot: string): number {
  const dir = autoDirOf(repoRoot);
  return readdirSync(dir, { recursive: true })
    .map(String)
    .filter((name) => name.endsWith(".java"))
    .reduce(
      (newest, name) => Math.max(newest, statSync(join(dir, name)).mtimeMs),
      0,
    );
}

export function autosMessage(repoRoot: string, locked: boolean): AutosMessage {
  const autos = listAutos(repoRoot);
  const sourceMtime = autos.length ? newestSourceMtime(repoRoot) : 0;
  return {
    autos: autos.map((name) => {
      const file = ppFileFor(repoRoot, name);
      const exportedAt = existsSync(file) ? statSync(file).mtimeMs : null;
      return {
        name,
        exportedAt,
        stale: exportedAt !== null && sourceMtime > exportedAt,
        failed: existsSync(`${file}.error`),
      };
    }),
    selected: readSelected(repoRoot),
    exporting: readMarker(repoRoot, EXPORTING_FILE),
    locked,
  };
}
