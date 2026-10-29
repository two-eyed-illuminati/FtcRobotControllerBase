package org.firstinspires.ftc.teamcode.TeleOp;

import com.pedropathing.follower.Follower;
import com.pedropathing.ivy.Scheduler;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;

import org.firstinspires.ftc.teamcode.utils.Robot;

public abstract class TeleOpMode extends OpMode {
  protected Follower follower;

  @Override
  public final void init() {
    Scheduler.reset();
    Robot.initialize(hardwareMap, telemetry);
    follower = Robot.follower;
  }

  @Override
  public final void init_loop() {
    Robot.requestAlliance(gamepad1);
  }

  @Override
  public final void loop() {
    Robot.clearBulkCache();
    onLoop();
    follower.update();
    Robot.pose = follower.pose();
    Scheduler.execute();
  }

  @Override
  public final void stop() {
    Scheduler.reset();
  }

  protected void onLoop() {
  }
}
