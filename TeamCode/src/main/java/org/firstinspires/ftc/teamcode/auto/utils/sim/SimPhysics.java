package org.firstinspires.ftc.teamcode.auto.utils.sim;

import com.pedropathing.drivetrain.DrivePowers;
import com.pedropathing.math.Pose;
import com.pedropathing.math.Twist;

/** Integrates the robot pose from drive powers, in 1 ms sub-steps with the powers held. */
public final class SimPhysics {
  public static final double SUB_STEP = 0.001;

  private final SimAxis forward;
  private final SimAxis strafe;
  private final SimAxis turn;
  private double x;
  private double y;
  private double heading;
  private double vx;
  private double vy;
  private double omega;

  public SimPhysics(SimAxis forward, SimAxis strafe, SimAxis turn) {
    this.forward = forward;
    this.strafe = strafe;
    this.turn = turn;
  }

  public void setPose(Pose pose) {
    x = pose.x();
    y = pose.y();
    heading = wrap(pose.heading());
  }

  public void advance(DrivePowers powers, double dt) {
    int steps = (int) Math.round(dt / SUB_STEP);
    for (int i = 0; i < steps; i++) {
      vx = forward.step(vx, powers.forward(), SUB_STEP);
      vy = strafe.step(vy, powers.strafe(), SUB_STEP);
      omega = turn.step(omega, powers.turn(), SUB_STEP);
      double midHeading = heading + omega * SUB_STEP / 2;
      x += (vx * Math.cos(midHeading) - vy * Math.sin(midHeading)) * SUB_STEP;
      y += (vx * Math.sin(midHeading) + vy * Math.cos(midHeading)) * SUB_STEP;
      heading = wrap(heading + omega * SUB_STEP);
    }
  }

  public Pose pose() {
    return new Pose(x, y, heading);
  }

  public Twist twist() {
    return new Twist(vx, vy, omega);
  }

  public double worldSpeed() {
    return Math.hypot(vx, vy);
  }

  public double omega() {
    return omega;
  }

  private static double wrap(double angle) {
    return Math.atan2(Math.sin(angle), Math.cos(angle));
  }
}
