import type { BasePoint } from "../types";

/**
 * The four corners of a robot rectangle centered at (x, y), in field
 * coordinates with y up.
 * @param heading - Degrees counter-clockwise from +x
 * @param width - Extent along the heading, as the robot sprite draws it
 * @param height - Extent across the heading
 * @returns Corners in counter-clockwise order: back-right, front-right,
 *   front-left, back-left
 */
export function getRobotCorners(
  x: number,
  y: number,
  heading: number,
  width: number,
  height: number,
): BasePoint[] {
  // Convert heading from degrees to radians
  const headingRad = (heading * Math.PI) / 180;

  // Half dimensions
  const hw = width / 2;
  const hh = height / 2;

  // Calculate rotation components
  const cos = Math.cos(headingRad);
  const sin = Math.sin(headingRad);

  const corners = [
    { dx: -hw, dy: -hh },
    { dx: hw, dy: -hh },
    { dx: hw, dy: hh },
    { dx: -hw, dy: hh },
  ];

  return corners.map((corner) => ({
    x: x + corner.dx * cos - corner.dy * sin,
    y: y + corner.dx * sin + corner.dy * cos,
  }));
}
