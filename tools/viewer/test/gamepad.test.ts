import { describe, expect, it } from "vitest";
import { gamepadAt, gamepadsIn, isGamepadKey } from "../src/sim/gamepad";

describe("gamepadAt", () => {
  it("reads logged values and defaults the rest", () => {
    const pad = gamepadAt(
      {
        "gamepad1.left_stick_x": "-0.5",
        "gamepad1.right_trigger": "1.0E-4",
        "gamepad1.a": "true",
        "gamepad1.b": "false",
      },
      "gamepad1",
    );
    expect(pad.axes.left_stick_x).toBe(-0.5);
    expect(pad.axes.right_trigger).toBeCloseTo(1e-4);
    expect(pad.axes.left_stick_y).toBe(0);
    expect(pad.buttons.a).toBe(true);
    expect(pad.buttons.b).toBe(false);
    expect(pad.buttons.x).toBe(false);
  });
});

describe("gamepad keys", () => {
  it("finds gamepads and their keys", () => {
    const values = { "gamepad2.a": "true", Pose: "p", "gamepad1x.a": "1" };
    expect(gamepadsIn(values)).toEqual(["gamepad2"]);
    expect(isGamepadKey("gamepad1.left_stick_x")).toBe(true);
    expect(isGamepadKey("Pose")).toBe(false);
  });
});
