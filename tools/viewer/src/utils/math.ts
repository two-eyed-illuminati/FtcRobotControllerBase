export function clamp(value: number, min: number, max: number): number {
  return Math.min(max, Math.max(min, value));
}

export function normalizeAngleDegrees(angle: number): number {
  if (!Number.isFinite(angle)) return 0;
  let normalized = angle % 360;
  if (normalized > 180) normalized -= 360;
  if (normalized <= -180) normalized += 360;
  return normalized;
}

/**
 * Calculates the smallest difference between two angles.
 * Returns a value between -180 and 180.
 */
export function getAngularDifference(start: number, end: number): number {
  const normalizedStart = (start + 360) % 360;
  const normalizedEnd = (end + 360) % 360;
  let diff = normalizedEnd - normalizedStart;

  if (diff > 180) diff -= 360;
  else if (diff < -180) diff += 360;

  return diff;
}

/**
 * Calculates the shortest rotation from startAngle to endAngle based on a percentage.
 * @param startAngle
 * @param endAngle
 * @param percentage
 * @returns
 */
export function shortestRotation(
  startAngle: number,
  endAngle: number,
  percentage: number,
) {
  // Use the helper to find the shortest signed difference
  const diff = getAngularDifference(startAngle, endAngle);
  // Apply difference to the ORIGINAL startAngle to preserve winding/continuity
  return startAngle + diff * percentage;
}

export function reversedRotation(
  startAngle: number,
  endAngle: number,
  percentage: number,
) {
  const shortest = getAngularDifference(startAngle, endAngle);
  const longWay = shortest >= 0 ? shortest - 360 : shortest + 360;
  return normalizeAngleDegrees(startAngle + longWay * percentage);
}

export function interpolateAngleDegrees(
  startAngle: number,
  endAngle: number,
  percentage: number,
  reversed = false,
) {
  return reversed
    ? reversedRotation(startAngle, endAngle, percentage)
    : shortestRotation(startAngle, endAngle, percentage);
}

export function radiansToDegrees(radians: number) {
  return radians * (180 / Math.PI);
}

export function lerp(ratio: number, start: number, end: number) {
  return start + (end - start) * ratio;
}

export function lerp2d(
  ratio: number,
  start: { x: number; y: number },
  end: { x: number; y: number },
) {
  return {
    x: lerp(ratio, start.x, end.x),
    y: lerp(ratio, start.y, end.y),
  };
}

export function getCurvePoint(
  t: number,
  points: { x: number; y: number }[],
): { x: number; y: number } {
  if (points.length === 1) return points[0];
  const newpoints = [];
  for (let i = 0, j = 1; j < points.length; i++, j++) {
    newpoints[i] = lerp2d(t, points[i], points[j]);
  }
  return getCurvePoint(t, newpoints);
}
