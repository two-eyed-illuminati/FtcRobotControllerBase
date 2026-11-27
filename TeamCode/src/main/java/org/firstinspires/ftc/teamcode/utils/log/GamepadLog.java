package org.firstinspires.ftc.teamcode.utils.log;

import com.qualcomm.robotcore.hardware.Gamepad;

/**
 * Records every input of a Logitech F310 to the RunLog under "<name>.<field>". Skips the PS4
 * aliases (cross, circle, share, ...) that mirror a, b, back, ..., the PS4-only touchpad, the
 * guide button the F310 lacks, and the per-packet timestamp, which would change every loop.
 * Unchanged values are not written again.
 */
public final class GamepadLog {
  private GamepadLog() {
  }

  public static void record(RecordingTelemetry telemetry, String name, Gamepad gamepad) {
    String p = name + ".";
    telemetry.record(p + "left_stick_x", gamepad.left_stick_x);
    telemetry.record(p + "left_stick_y", gamepad.left_stick_y);
    telemetry.record(p + "right_stick_x", gamepad.right_stick_x);
    telemetry.record(p + "right_stick_y", gamepad.right_stick_y);
    telemetry.record(p + "left_trigger", gamepad.left_trigger);
    telemetry.record(p + "right_trigger", gamepad.right_trigger);

    telemetry.record(p + "dpad_up", gamepad.dpad_up);
    telemetry.record(p + "dpad_down", gamepad.dpad_down);
    telemetry.record(p + "dpad_left", gamepad.dpad_left);
    telemetry.record(p + "dpad_right", gamepad.dpad_right);

    telemetry.record(p + "a", gamepad.a);
    telemetry.record(p + "b", gamepad.b);
    telemetry.record(p + "x", gamepad.x);
    telemetry.record(p + "y", gamepad.y);

    telemetry.record(p + "left_bumper", gamepad.left_bumper);
    telemetry.record(p + "right_bumper", gamepad.right_bumper);
    telemetry.record(p + "left_stick_button", gamepad.left_stick_button);
    telemetry.record(p + "right_stick_button", gamepad.right_stick_button);

    telemetry.record(p + "start", gamepad.start);
    telemetry.record(p + "back", gamepad.back);
  }
}
