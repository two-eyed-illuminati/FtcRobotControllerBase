export const GAMEPAD_NAMES = ["gamepad1", "gamepad2"] as const;
export type GamepadName = (typeof GAMEPAD_NAMES)[number];

export const AXES = [
  "left_stick_x",
  "left_stick_y",
  "right_stick_x",
  "right_stick_y",
  "left_trigger",
  "right_trigger",
] as const;
export type Axis = (typeof AXES)[number];

export const BUTTONS = [
  "dpad_up",
  "dpad_down",
  "dpad_left",
  "dpad_right",
  "a",
  "b",
  "x",
  "y",
  "left_bumper",
  "right_bumper",
  "left_stick_button",
  "right_stick_button",
] as const;
export type Button = (typeof BUTTONS)[number];

export interface GamepadState {
  axes: Record<Axis, number>;
  buttons: Record<Button, boolean>;
}

/** Stick deflection. y is positive when the stick points down, as on the robot. */
export interface StickPoint {
  x: number;
  y: number;
}

export function isGamepadKey(key: string) {
  return GAMEPAD_NAMES.some((name) => key.startsWith(`${name}.`));
}

/** The gamepads that have any logged value in `values`. */
export function gamepadsIn(values: Record<string, string>) {
  return GAMEPAD_NAMES.filter((name) =>
    Object.keys(values).some((key) => key.startsWith(`${name}.`)),
  );
}

function axisValue(text: string | undefined, key: string) {
  if (text === undefined) return 0;
  const value = Number(text);
  if (Number.isFinite(value)) return value;
  console.warn(`[viewer] gamepad value ${key}=${text} is not a number`);
  return 0;
}

/** Values the robot has not logged yet read as released and centered. */
export function gamepadAt(
  values: Record<string, string>,
  name: GamepadName,
): GamepadState {
  const axes = {} as Record<Axis, number>;
  for (const axis of AXES) {
    axes[axis] = axisValue(values[`${name}.${axis}`], `${name}.${axis}`);
  }
  const buttons = {} as Record<Button, boolean>;
  for (const button of BUTTONS) {
    buttons[button] = values[`${name}.${button}`] === "true";
  }
  return { axes, buttons };
}
