package org.firstinspires.ftc.teamcode.TeleOp;

import com.pedropathing.ivy.Scheduler;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;

import org.firstinspires.ftc.teamcode.utils.Robot;

public abstract class TeleOpMode extends OpMode {
  @Override
  public final void init() {
    Robot.init(hardwareMap, telemetry, getClass().getSimpleName());
  }

  @Override
  public final void init_loop() {
    Robot.initLoop(gamepad1);
  }

  @Override
  public final void loop() {
    Robot.loop();
    onLoop();
    Robot.telemetry.update();
    Scheduler.execute();
  }

  @Override
  public final void stop() {
    Scheduler.reset();
    Robot.closeRunLog();
  }

  protected void onLoop() {
  }
}
