// Exported type definitions for use in Svelte and TS modules

export interface BasePoint {
  x: number;
  y: number;
  locked?: boolean;
}

export type PiecewiseHeadingInterpolationType =
  "linear" | "constant" | "tangential" | "facing-point";

export interface PiecewiseHeadingSegment {
  startProgress: number;
  endProgress: number;
  interpolationType: PiecewiseHeadingInterpolationType;
  reversed?: boolean;
  /** Take the start angle from where the previous segment ends. Linear and constant only. */
  continueFromPrevious?: boolean;
  parameters?: {
    startDeg?: number;
    endDeg?: number;
    degrees?: number;
    point?: BasePoint;
  };
}

export interface PiecewiseHeadingInterpolation {
  segments: PiecewiseHeadingSegment[];
}

export interface FieldPoint extends BasePoint {
  color?: string;
  radius?: number;
  opacity?: number;
}

export type HeadingType = "linear" | "constant" | "tangential" | "piecewise";

export type Heading =
  | { type: "linear"; startDeg: number; endDeg: number }
  | { type: "constant"; degrees: number }
  | { type: "tangential"; reverse: boolean }
  | { type: "piecewise"; piecewiseHeading: PiecewiseHeadingInterpolation };

export type Point = BasePoint & { name?: string };

export interface StartPose extends BasePoint {
  name?: string;
  headingDeg: number;
}

export type ControlPoint = BasePoint;

export interface WaitSegment {
  name?: string;
  durationMs: number;
  position?: "before" | "after";
}

export type Path = {
  id: string;
  color: string;
  name?: string;
  locked?: boolean;
  waitBefore?: WaitSegment;
  waitAfter?: WaitSegment;
  waitBeforeMs?: number;
  waitAfterMs?: number;
  waitBeforeName?: string;
  waitAfterName?: string;
} & (Atomic | Compound);

export interface Atomic {
  kind: "atomic";
  endPoint: Point;
  controlPoints: ControlPoint[];
  /** Interior poses for Pedro's Paths.through(); never emitted as controls. */
  throughPoints?: BasePoint[];
  heading: Heading;
}

export type AtomicPath = Extract<Path, Atomic>;

export interface Compound {
  kind: "compound";
  segments: Path[];
  heading?: Heading;
}

export type CompoundPath = Extract<Path, Compound>;

export type SequenceWaitItem = {
  kind: "wait";
  id: string;
  name: string;
  durationMs: number;
  locked?: boolean;
};

export type SequencePathItem = {
  kind: "path";
  lineId: string;
};

export type SequenceItem = SequencePathItem | SequenceWaitItem;

/** The settings that change how a path drives. */
export interface RobotConfig {
  forwardVelocity: number; // inches/sec
  strafeVelocity: number; // inches/sec
  aVelocity: number;
  kFriction: number;
  rLength: number; // inches along the heading
  rWidth: number; // inches side to side
  safetyMargin: number;
  maxAcceleration: number; // inches/sec²
  maxDeceleration?: number; // inches/sec²
}

export interface Settings extends RobotConfig {
  fieldMap: string;
}

export interface Shape {
  id: string;
  name?: string;
  vertices: BasePoint[];
  color: string;
  fillColor: string;
}
