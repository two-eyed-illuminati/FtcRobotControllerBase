package org.firstinspires.ftc.teamcode.auto.utils.sim;

import com.pedropathing.drivetrain.DrivePowers;
import com.pedropathing.drivetrain.Drivetrain;

import java.util.HashMap;
import java.util.Map;

/**
 * Mecanum drivetrain without hardware. The wheel mixing, maxScaling and interpolateVelocity
 * match com.pedropathing.revhub.drivetrains.Mecanum (3.0.1), which needs a HardwareMap.
 */
public final class SimDrivetrain implements Drivetrain {
  private DrivePowers applied = DrivePowers.zero();

  @Override
  public void drive(DrivePowers powers, boolean manual) {
    double[] wheels = wheelPowers(powers);
    double maxPower = 1.0;
    for (double wheel : wheels) maxPower = Math.max(maxPower, Math.abs(wheel));
    double fl = wheels[0] / maxPower;
    double fr = wheels[1] / maxPower;
    double bl = wheels[2] / maxPower;
    double br = wheels[3] / maxPower;
    applied = new DrivePowers(
        (fl + fr + bl + br) / 4,
        (-fl + fr + bl - br) / 4,
        (-fl + fr - bl + br) / 4);
  }

  public DrivePowers applied() {
    return applied;
  }

  private static double[] wheelPowers(DrivePowers powers) {
    double forward = powers.forward();
    double strafe = powers.strafe();
    double turn = powers.turn();
    return new double[] {
        forward - strafe - turn,
        forward + strafe + turn,
        forward + strafe - turn,
        forward - strafe + turn};
  }

  @Override
  public double maxScaling(DrivePowers current, DrivePowers delta) {
    double lambda = 1.0;
    double[] currentPowers = wheelPowers(current);
    double[] deltaPowers = wheelPowers(delta);
    for (int i = 0; i < 4; i++) {
      double a = currentPowers[i];
      double b = deltaPowers[i];
      if (Math.abs(b) < 1e-9) continue;
      double t1 = (1.0 - a) / b;
      double t2 = (-1.0 - a) / b;
      if (t1 >= 0.0 && t1 < lambda) lambda = t1;
      if (t2 >= 0.0 && t2 < lambda) lambda = t2;
    }
    return Math.max(0.0, Math.min(1.0, lambda));
  }

  @Override
  public void stop() {
    applied = DrivePowers.zero();
  }

  @Override
  public void stop(boolean brake) {
    applied = DrivePowers.zero();
  }

  @Override
  public Map<String, Object> debug() {
    return new HashMap<>();
  }

  @Override
  public double interpolateVelocity(double xRadius, double yRadius, double theta) {
    return 1.0 / (Math.abs(Math.cos(theta)) / xRadius + Math.abs(Math.sin(theta)) / yRadius);
  }
}
