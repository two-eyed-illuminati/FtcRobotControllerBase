import { getContext, setContext } from "svelte";
import { SvelteSet } from "svelte/reactivity";
import { DEFAULT_SETTINGS } from "../config";
import { parseProject, robotConfigOf, type ProjectDoc } from "../io";
import {
  parseSim,
  sampleAt,
  type SimData,
  type SimSample,
} from "../sim/simData";

export type FeedStatus = "loading" | "ok" | "error";

type FeedMessage = (
  | { ok: true; file: string; text: string; mtimeMs: number }
  | { ok: false; file: string; error: string }
) & { exportError: string | null; simText: string | null };

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
  sim = $state.raw<SimData | null>(null);
  /** Why there is no simulation, when the export wrote none or it failed. */
  simError = $state<string | null>(null);

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

    this.applySim(message.simText);
    this.doc = result.doc;
    this.warnings = result.warnings;
    this.error = null;
    this.status = "ok";
    this.updatedAt = message.mtimeMs;
    console.info(
      `[viewer] loaded ${message.file}: ${result.doc.lines.length} paths, ${message.text.length} bytes`,
    );
  }

  private applySim(text: string | null) {
    if (text === null) {
      this.sim = null;
      this.simError = "No simulation file was exported.";
      return;
    }
    const result = parseSim(text);
    if (!result.ok) {
      this.sim = null;
      this.simError = result.error;
      console.error(`[viewer] ${result.error}`);
      return;
    }
    this.sim = result.sim;
    this.simError = null;
    console.info(
      `[viewer] simulation: ${result.sim.samples.t.length} samples, ${result.sim.duration.toFixed(2)} s, ` +
        `end speed ${result.sim.summary.endSpeed.toFixed(2)} in/s, ${result.sim.warnings.length} warnings`,
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
  showTrace = $state(true);
  showFootprints = $state(false);
  footprintSpacing = $state(6);
  showSwept = $state(false);
  hiddenPathIds = new SvelteSet<string>();

  togglePath(id: string) {
    if (!this.hiddenPathIds.delete(id)) this.hiddenPathIds.add(id);
  }
}

export const PLAYBACK_RATES = [0.25, 0.5, 1, 2];

/** Plays the simulation back in real time, scaled by `rate`. */
export class Playback {
  time = $state(0);
  playing = $state(false);
  loop = $state(true);
  rate = $state(1);

  private frame = 0;
  private lastFrameAt = 0;
  duration: number;
  sample: SimSample | null;

  constructor(feed: PathFeed) {
    this.duration = $derived(feed.sim?.duration ?? 0);
    this.sample = $derived(feed.sim ? sampleAt(feed.sim, this.time) : null);
  }

  seek(seconds: number) {
    this.time = Math.min(this.duration, Math.max(0, seconds));
  }

  toggle() {
    if (this.playing) return this.pause();
    if (this.time >= this.duration) this.time = 0;
    this.playing = true;
    this.lastFrameAt = performance.now();
    this.frame = requestAnimationFrame(this.tick);
  }

  pause() {
    this.playing = false;
    cancelAnimationFrame(this.frame);
  }

  private tick = (now: number) => {
    const elapsed = ((now - this.lastFrameAt) / 1000) * this.rate;
    this.lastFrameAt = now;
    const next = this.time + elapsed;
    if (next < this.duration) this.time = next;
    else if (this.loop) this.time = next % Math.max(this.duration, 1e-6);
    else {
      this.time = this.duration;
      return this.pause();
    }
    this.frame = requestAnimationFrame(this.tick);
  };
}

const FEED_KEY = Symbol("feed");
const OPTIONS_KEY = Symbol("options");
const PLAYBACK_KEY = Symbol("playback");

export function provideViewer(
  feed: PathFeed,
  options: ViewOptions,
  playback: Playback,
) {
  setContext(FEED_KEY, feed);
  setContext(OPTIONS_KEY, options);
  setContext(PLAYBACK_KEY, playback);
}

export const useFeed = () => getContext<PathFeed>(FEED_KEY);
export const useOptions = () => getContext<ViewOptions>(OPTIONS_KEY);
export const usePlayback = () => getContext<Playback>(PLAYBACK_KEY);
