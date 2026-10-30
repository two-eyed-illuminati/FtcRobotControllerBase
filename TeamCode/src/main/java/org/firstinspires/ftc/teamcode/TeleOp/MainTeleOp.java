package org.firstinspires.ftc.teamcode.TeleOp;

import com.pedropathing.follower.ManualDrive;
import com.pedropathing.math.Pose;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.teamcode.utils.Robot;

@TeleOp(name = "Main TeleOp", group = "Main")
public class MainTeleOp extends TeleOpMode {
  @Override
  protected void onLoop() {
    double driverForward = Robot.toField(new Pose(0, 0, Robot.DRIVER_FORWARD_HEADING)).heading();
    double heading = Robot.follower.pose().heading();

    Robot.follower.manual(ManualDrive.fieldCentric(
      -gamepad1.left_stick_y, gamepad1.left_stick_x, gamepad1.right_stick_x,
      heading, -driverForward));
  }
}
