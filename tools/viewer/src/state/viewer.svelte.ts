import { getContext, setContext } from "svelte";
import { SvelteSet } from "svelte/reactivity";
import { DEFAULT_SETTINGS } from "../config";
import { parseProject, robotConfigOf, type ProjectDoc } from "../io";

export type FeedStatus = "loading" | "ok" | "error";

type FeedMessage = (
  | { ok: true; file: string; text: string; mtimeMs: number }
  | { ok: false; file: string; error: string }
) & { exportError: string | null };

const ROBOT_FALLBACK = robotConfigOf(DEFAULT_SETTINGS);

/** The exported path as last pushed by the dev server; keeps the last good drawing on errors. */
export class PathFeed {
  status = $state<FeedStatus>("loading");
  doc = $state.raw<ProjectDoc | null>(null);
  error = $state<string | null>(null);
  file = $state("");
  updatedAt = $state<number | null>(null);
  warnings = $state.raw<string[]>([]);
  /** Compiler or runtime output from the last failed Java export. */
  exportError = $state<string | null>(null);

  async start() {
    import.meta.hot?.on("pp:update", (message: FeedMessage) =>
      this.apply(message),
    );
    try {
      const response = await fetch("/__pp");
      this.apply(await response.json());
    } catch (error) {
      this.fail(
        `No live feed from the dev server (${String(error)}). Start with pnpm dev.`,
      );
    }
  }

  apply(message: FeedMessage) {
    this.file = message.file;
    this.exportError = message.exportError;
    if (message.exportError) {
      console.error(`[viewer] Java export failed:\n${message.exportError}`);
    }
    if (!message.ok) return this.fail(message.error);

    const result = parseProject(message.text, ROBOT_FALLBACK);
    if (!result.ok) return this.fail(`Invalid .pp file: ${result.error}`);

    this.doc = result.doc;
    this.warnings = result.warnings;
    this.error = null;
    this.status = "ok";
    this.updatedAt = message.mtimeMs;
    console.info(
      `[viewer] loaded ${message.file}: ${result.doc.lines.length} paths, ${message.text.length} bytes`,
    );
  }

  fail(error: string) {
    this.error = error;
    this.status = "error";
    console.error(`[viewer] ${error}`);
  }
}

/** What the layers panel toggles. Nothing is saved between reloads. */
export class ViewOptions {
  fieldMap = $state(DEFAULT_SETTINGS.fieldMap);
  showControlPoints = $state(true);
  showHeadings = $state(true);
  showRobotAtStart = $state(true);
  showFootprints = $state(false);
  footprintSpacing = $state(6);
  showSwept = $state(false);
  hiddenPathIds = new SvelteSet<string>();

  togglePath(id: string) {
    if (!this.hiddenPathIds.delete(id)) this.hiddenPathIds.add(id);
  }
}

const FEED_KEY = Symbol("feed");
const OPTIONS_KEY = Symbol("options");

export function provideViewer(feed: PathFeed, options: ViewOptions) {
  setContext(FEED_KEY, feed);
  setContext(OPTIONS_KEY, options);
}

export const useFeed = () => getContext<PathFeed>(FEED_KEY);
export const useOptions = () => getContext<ViewOptions>(OPTIONS_KEY);
