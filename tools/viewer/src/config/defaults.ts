import type { Settings } from "../types";

/**
 * Default robot dimensions
 */
export const DEFAULT_ROBOT_LENGTH = 16;
export const DEFAULT_ROBOT_WIDTH = 16;

/**
 * Default canvas drawing settings
 */
export const POINT_RADIUS = 1;
export const LINE_WIDTH = 0.57;
/**
 * The floor inside the perimeter walls, in inches; coordinates run from 0 to
 * this. Six 24" tiles make 144", and the walls cover the outer 1.25" of the
 * edge tiles.
 */
export const FIELD_SIZE = 141.5;
export const TILE_SIZE = 24;
/**
 * Field images show all six tiles at full size, so they extend this far past
 * the floor on every side and are cropped to it.
 */
export const FIELD_IMAGE_OVERHANG = (6 * TILE_SIZE - FIELD_SIZE) / 2;
/**
 * Available field maps
 */
export const AVAILABLE_FIELD_MAPS = [
  { value: "biobuzz.webp", label: "BIOBUZZ Field (2026-2027)" },
  { value: "decode.webp", label: "DECODE Field (2025-2026)" },
  { value: "intothedeep.webp", label: "INTO THE DEEP Field (2024-2025)" },
  { value: "centerstage.webp", label: "CENTERSTAGE (2023-2024)" },
];

export type ColorScheme = "light" | "dark";

/**
 * Fields drawn in both schemes. The key is the field's id, which is not a file
 * itself; fields not listed here use `/fields/<id>` in either scheme.
 */
export const FIELD_SCHEME_IMAGES: Record<
  string,
  Record<ColorScheme, string>
> = {
  "biobuzz.webp": { light: "biobuzz-light.webp", dark: "biobuzz-dark.webp" },
};

/**
 * Default settings
 */
export const DEFAULT_SETTINGS: Settings = {
  forwardVelocity: 75,
  strafeVelocity: 65,
  aVelocity: Math.PI,
  kFriction: 0.1,
  rLength: DEFAULT_ROBOT_LENGTH,
  rWidth: DEFAULT_ROBOT_WIDTH,
  safetyMargin: 1,
  maxAcceleration: 50,
  maxDeceleration: 50,
  fieldMap: "biobuzz.webp",
};
