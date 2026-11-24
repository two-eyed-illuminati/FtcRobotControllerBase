import { parseSim, wrapAngle, type SimData, type SimParse } from "./simData";

/** The resampling step, equal to the simulation's control tick. */
export const TRACE_DT = 0.01;
const MAX_LOOP_GAP = 0.1;

const TRACE_COLUMNS = [
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
] as const;

type TraceColumn = (typeof TRACE_COLUMNS)[number];

/**
 * Reads a robot trace (see FollowerTrace.java) as a run. The loop times of a real robot differ from
 * tick to tick, so the samples are resampled to a fixed step. Time 0 is the first FOLLOW sample.
 */
export function parseTrace(raw: Record<string, unknown>): SimParse {
  const result = resampleTrace(raw);
  return result.ok ? { ok: true, sim: result.sim } : result;
}

type Resampled =
  { ok: true; sim: SimData; t0: number } | { ok: false; error: string };

function resampleTrace(raw: Record<string, unknown>): Resampled {
  const samples = raw.samples as Record<string, unknown> | undefined;
  if (!samples) return { ok: false, error: "The trace has no samples." };
  const columns = {} as Record<TraceColumn, number[]>;
  for (const name of TRACE_COLUMNS) {
    const column = samples[name];
    if (!Array.isArray(column)) {
      return { ok: false, error: `The trace is missing column "${name}".` };
    }
    columns[name] = column.map((value) => Number(value ?? 0));
  }
  const length = columns.t.length;
  if (length < 2) return { ok: false, error: "The trace has under 2 samples." };
  if (TRACE_COLUMNS.some((name) => columns[name].length !== length)) {
    return { ok: false, error: "The trace columns differ in length." };
  }

  const modes = (raw.modes ?? []) as string[];
  const follow = modes.indexOf("FOLLOW");
  const hold = modes.indexOf("HOLD");
  const warnings: string[] = [];
  let first = columns.mode.findIndex((mode) => mode === follow);
  if (first < 0) {
    warnings.push(
      "The trace never entered FOLLOW; time 0 is the first sample.",
    );
    first = 0;
  }
  if (first >= length - 1) {
    return { ok: false, error: "The trace has no samples after FOLLOW began." };
  }

  const t0 = columns.t[first];
  const time = columns.t.slice(first).map((value) => value - t0);
  const rows = time.length;
  const pick = (name: TraceColumn) => columns[name].slice(first);
  const [x, y, heading, omega, closestT, mode, pathIndex, group] = [
    "x",
    "y",
    "heading",
    "omega",
    "closestT",
    "mode",
    "pathIndex",
    "group",
  ].map((name) => pick(name as TraceColumn));
  const vy = pick("vy");
  const speed = pick("vx").map((vx, i) => Math.hypot(vx, vy[i]));

  const gaps = time.slice(1).map((value, i) => value - time[i]);
  const maxGap = Math.max(...gaps);
  if (maxGap > MAX_LOOP_GAP) {
    warnings.push(
      `The longest loop took ${(maxGap * 1000).toFixed(0)} ms; the run is interpolated across it.`,
    );
  }

  const duration = time[rows - 1];
  const count = Math.floor(duration / TRACE_DT) + 1;
  const grid = {
    t: [] as number[],
    x: [] as number[],
    y: [] as number[],
    heading: [] as number[],
    speed: [] as number[],
    omega: [] as number[],
    targetVelocity: [] as number[],
    closestT: [] as number[],
    braking: [] as number[],
    segment: [] as number[],
    group: [] as number[],
    holding: [] as number[],
    forwardPower: [] as number[],
    strafePower: [] as number[],
    turnPower: [] as number[],
  };
  let j = 0;
  for (let i = 0; i < count; i++) {
    const at = i * TRACE_DT;
    while (j < rows - 2 && time[j + 1] <= at) j++;
    const span = time[j + 1] - time[j];
    const f = span > 0 ? Math.min(1, Math.max(0, (at - time[j]) / span)) : 0;
    const blend = (values: number[]) =>
      values[j] + (values[j + 1] - values[j]) * f;
    grid.t.push(at);
    grid.x.push(blend(x));
    grid.y.push(blend(y));
    grid.heading.push(heading[j] + wrapAngle(heading[j + 1] - heading[j]) * f);
    grid.speed.push(blend(speed));
    grid.omega.push(blend(omega));
    grid.targetVelocity.push(0);
    grid.closestT.push(blend(closestT));
    grid.braking.push(0);
    grid.segment.push(Math.max(0, pathIndex[j]));
    grid.group.push(Math.max(0, group[j]));
    grid.holding.push(mode[j] === hold ? 1 : 0);
    grid.forwardPower.push(0);
    grid.strafePower.push(0);
    grid.turnPower.push(0);
  }

  const meanLoop = duration / (rows - 1);
  return {
    ok: true,
    t0,
    sim: {
      dt: TRACE_DT,
      duration: (count - 1) * TRACE_DT,
      summary: {
        maxSpeed: Math.max(...grid.speed),
        endSpeed: grid.speed[count - 1],
        endPositionError: 0,
        endHeadingError: 0,
      },
      assumptions: {
        source: "robot trace",
        name: String(raw.name ?? ""),
        rawSamples: rows,
        meanLoopMs: +(meanLoop * 1000).toFixed(2),
      },
      warnings,
      groups: [],
      segments: [],
      events: [],
      samples: grid,
    },
  };
}

/**
 * Reads a streamed robot log (see RunLog.java): a header line, one array per loop and one
 * object per telemetry change. A cut-off last line, as after a crash, is skipped with a warning.
 */
export function parseRunLog(text: string): SimParse {
  const lines = text.split("\n");
  while (lines.length && lines[lines.length - 1].trim() === "") lines.pop();
  if (lines.length === 0) return { ok: false, error: "The log is empty." };

  let header: Record<string, unknown>;
  try {
    header = JSON.parse(lines[0]);
  } catch {
    return { ok: false, error: "The log header on line 1 is not valid JSON." };
  }
  const names = header.columns;
  if (
    !Array.isArray(names) ||
    TRACE_COLUMNS.some((name) => !names.includes(name))
  ) {
    return { ok: false, error: "The log header is missing sample columns." };
  }

  const warnings: string[] = [];
  const rows: unknown[][] = [];
  const telemetry: { t: number; tel: Record<string, unknown> }[] = [];
  const endsWithNewline = text.endsWith("\n");
  for (let i = 1; i < lines.length; i++) {
    let line: unknown;
    try {
      line = JSON.parse(lines[i]);
    } catch {
      if (i === lines.length - 1 && !endsWithNewline) {
        warnings.push("The log ends mid-line; the last 0.5 s may be missing.");
        break;
      }
      return {
        ok: false,
        error: `Line ${i + 1} of the log is not valid JSON.`,
      };
    }
    if (Array.isArray(line)) rows.push(line);
    else if (line && typeof line === "object" && "tel" in line) {
      telemetry.push(line as { t: number; tel: Record<string, unknown> });
    } else {
      return {
        ok: false,
        error: `Line ${i + 1} of the log is not a row or telemetry.`,
      };
    }
  }

  const raw = {
    kind: "trace",
    name: header.name,
    modes: header.modes,
    samples: Object.fromEntries(
      TRACE_COLUMNS.map((name) => {
        const column = names.indexOf(name);
        return [name, rows.map((row) => row[column])];
      }),
    ),
  };
  const result = resampleTrace(raw);
  if (!result.ok) return result;

  const tracks: Record<string, { t: number[]; v: string[] }> = {};
  for (const { t, tel } of telemetry) {
    for (const [key, value] of Object.entries(tel)) {
      const track = (tracks[key] ??= { t: [], v: [] });
      track.t.push(Number(t) - result.t0);
      track.v.push(String(value));
    }
  }

  const { sim } = result;
  sim.warnings.push(...warnings);
  sim.telemetry = tracks;
  sim.assumptions.source = "robot log";
  sim.assumptions.telemetryLines = telemetry.length;
  return { ok: true, sim };
}

/** A `.sim.json` from the simulator, a `.trace.json` or a `.run.jsonl` from the robot. */
export function parseRun(text: string): SimParse {
  if (text.startsWith('{"kind":"run"')) return parseRunLog(text);
  let raw: Record<string, unknown>;
  try {
    raw = JSON.parse(text);
  } catch {
    return { ok: false, error: "The file is not valid JSON." };
  }
  return raw.kind === "trace" ? parseTrace(raw) : parseSim(text);
}
