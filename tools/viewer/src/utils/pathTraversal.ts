import type {
  AtomicPath,
  BasePoint,
  CompoundPath,
  Heading,
  Path,
} from "../types";
import { curveOf, type Curve } from "../model/curve";
import { clamp } from "./math";

/** One drivable curve, resolved out of the path structure. */
export interface FlatSegment {
  /** The atomic path this came from. */
  line: AtomicPath;
  /** Position in the flattened order, counting leaves only. */
  index: number;
  /** Where the segment begins: the previous segment's end, or the path start. */
  start: BasePoint;
  /** Built on first access. */
  readonly curve: Curve;
  /**
   * The groups containing this segment, outermost first. Empty for a segment
   * at the top level.
   */
  ancestors: CompoundPath[];
}

/**
 * Resolve a path into the flat list of curves the robot actually drives.
 *
 * A segment's start point is implicit — it is wherever the previous segment
 * ended — and that rule was previously re-derived at every call site. Routing
 * them all through here means the rule lives in one place, which is what lets
 * the structure become a tree without touching the consumers.
 *
 * `curve` is lazy because several callers only need the path structure.
 */
export function flattenToAtomicSegments(
  startPoint: BasePoint,
  paths: Path[],
): FlatSegment[] {
  const segments: FlatSegment[] = [];
  let start: BasePoint = startPoint;

  const walk = (nodes: Path[], ancestors: CompoundPath[]) => {
    for (const node of nodes) {
      if (!node) continue;

      if (node.kind === "compound") {
        walk(node.segments, [...ancestors, node]);
        continue;
      }
      if (!node.endPoint) continue;

      const segmentStart = start;
      let curve: Curve | null = null;

      segments.push({
        line: node,
        index: segments.length,
        start: segmentStart,
        get curve() {
          return (curve ??= curveOf(segmentStart, node));
        },
        ancestors,
      });

      start = node.endPoint;
    }
  };

  walk(paths, []);
  return segments;
}

/**
 * The heading rule in force at arc-length fraction `s` of a segment, and how
 * far through that rule the robot is. A group's heading overrides its
 * children's and runs across the whole group, measured by distance as Pedro's
 * compound paths do.
 */
export function effectiveHeadingAt(
  segments: FlatSegment[],
  index: number,
  s: number,
): { heading: Heading; s: number } {
  const segment = segments[index];
  const override = segment.ancestors.find((group) => group.heading);
  if (!override?.heading) {
    return { heading: segment.line.heading, s };
  }

  const members = segments.filter((entry) =>
    entry.ancestors.includes(override),
  );
  const total = members.reduce((sum, entry) => sum + entry.curve.length, 0);
  if (total <= 1e-9) return { heading: override.heading, s: 0 };

  let travelled = 0;
  for (const member of members) {
    if (member === segment) break;
    travelled += member.curve.length;
  }

  return {
    heading: override.heading,
    s: clamp((travelled + s * segment.curve.length) / total, 0, 1),
  };
}

/** Every drivable segment, in the order they are driven. */
export function atomicSegments(paths: Path[]): AtomicPath[] {
  return paths.flatMap((path) =>
    path.kind === "compound" ? atomicSegments(path.segments) : [path],
  );
}
