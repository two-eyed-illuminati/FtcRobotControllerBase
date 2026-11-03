export const SAMPLE_COLUMNS = [
  "t",
  "x",
  "y",
  "heading",
  "speed",
  "omega",
  "targetVelocity",
  "closestT",
  "braking",
  "segment",
  "group",
  "holding",
  "forwardPower",
  "strafePower",
  "turnPower",
] as const;

export type SampleColumn = (typeof SAMPLE_COLUMNS)[number];

export interface SimEvent {
  type: "pathStart" | "join" | "pathEnd" | "stop";
  t: number;
  group: number;
  segment: number;
  speed: number;
}

export interface SimData {
  /** Seconds between samples. */
  dt: number;
  duration: number;
  summary: {
    maxSpeed: number;
    endSpeed: number;
    endPositionError: number;
    endHeadingError: number;
  };
  assumptions: Record<string, string | number | boolean>;
  warnings: string[];
  groups: { group: number; firstSegment: number; segmentCount: number }[];
  segments: { group: number; index: number; length: number }[];
  events: SimEvent[];
  samples: Record<SampleColumn, number[]>;
}

export interface SimSample {
  t: number;
  /** Inches, field coordinates. */
  x: number;
  y: number;
  /** Radians. */
  heading: number;
  /** In/s. */
  speed: number;
  omega: number;
  targetVelocity: number;
  closestT: number;
  braking: boolean;
  segment: number;
  group: number;
  holding: boolean;
}

export type SimParse =
  { ok: true; sim: SimData } | { ok: false; error: string };

export function parseSim(text: string): SimParse {
  let raw: Record<string, unknown>;
  try {
    raw = JSON.parse(text);
  } catch {
    return { ok: false, error: "The simulation file is not valid JSON." };
  }
  if (raw.ok === false) {
    return { ok: false, error: String(raw.error ?? "Simulation failed.") };
  }

  const samples = raw.samples as Record<string, unknown> | undefined;
  if (!samples) return { ok: false, error: "The simulation has no samples." };
  const columns = {} as Record<SampleColumn, number[]>;
  for (const name of SAMPLE_COLUMNS) {
    const column = samples[name];
    if (!Array.isArray(column)) {
      return {
        ok: false,
        error: `The simulation is missing column "${name}".`,
      };
    }
    columns[name] = column.map((value) => Number(value ?? 0));
  }
  const length = columns.t.length;
  if (length === 0) return { ok: false, error: "The simulation is empty." };
  if (SAMPLE_COLUMNS.some((name) => columns[name].length !== length)) {
    return { ok: false, error: "The simulation columns differ in length." };
  }

  return {
    ok: true,
    sim: {
      dt: Number(raw.dt),
      duration: Number(raw.duration),
      summary: raw.summary as SimData["summary"],
      assumptions: (raw.assumptions ?? {}) as SimData["assumptions"],
      warnings: (raw.warnings ?? []) as string[],
      groups: (raw.groups ?? []) as SimData["groups"],
      segments: (raw.segments ?? []) as SimData["segments"],
      events: (raw.events ?? []) as SimEvent[],
      samples: columns,
    },
  };
}

export function wrapAngle(angle: number) {
  return Math.atan2(Math.sin(angle), Math.cos(angle));
}

/** The state at `seconds`; continuous values are interpolated, flags come from the earlier sample. */
export function sampleAt(sim: SimData, seconds: number): SimSample {
  const { samples } = sim;
  const last = samples.t.length - 1;
  const position = Math.min(last, Math.max(0, seconds / sim.dt));
  const index = Math.min(last, Math.floor(position));
  const next = Math.min(last, index + 1);
  const fraction = position - index;
  const blend = (name: SampleColumn) =>
    samples[name][index] +
    (samples[name][next] - samples[name][index]) * fraction;

  return {
    t: seconds,
    x: blend("x"),
    y: blend("y"),
    heading:
      samples.heading[index] +
      wrapAngle(samples.heading[next] - samples.heading[index]) * fraction,
    speed: blend("speed"),
    omega: blend("omega"),
    targetVelocity: blend("targetVelocity"),
    closestT: blend("closestT"),
    braking: samples.braking[index] === 1,
    segment: samples.segment[index],
    group: samples.group[index],
    holding: samples.holding[index] === 1,
  };
}
