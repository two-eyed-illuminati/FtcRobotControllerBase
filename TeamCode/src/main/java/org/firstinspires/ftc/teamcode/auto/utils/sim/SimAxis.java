package org.firstinspires.ftc.teamcode.auto.utils.sim;

/**
 * One body axis of the simulated drivetrain.
 * With power p the velocity approaches p * terminalVelocity() with time constant timeConstant().
 * This is the linear power-to-speed relation that a tuned kV assumes.
 * Friction acts only while the axis slows down (target speed below current speed).
 * A power of exactly 0 floats the motors, so only friction acts.
 */
public final class SimAxis {
  public final double maxVelocity;
  public final double stallAcceleration;
  public final double friction;

  public SimAxis(double maxVelocity, double stallAcceleration, double friction) {
    this.maxVelocity = maxVelocity;
    this.stallAcceleration = stallAcceleration;
    this.friction = friction;
  }

  /** Axis that reaches terminalVelocity under full power, with the given drive time constant and friction. */
  public static SimAxis fit(double terminalVelocity, double timeConstant, double friction) {
    double maxVelocity = terminalVelocity + timeConstant * friction;
    return new SimAxis(maxVelocity, maxVelocity / timeConstant, friction);
  }

  public double terminalVelocity() {
    return maxVelocity - timeConstant() * friction;
  }

  public double step(double velocity, double power, double dt) {
    double target = power * terminalVelocity();
    double driven = power == 0 ? velocity : velocity + (target - velocity) / timeConstant() * dt;
    boolean slowing = power == 0 || Math.abs(target) < Math.abs(velocity);
    double loss = slowing ? Math.min(friction * dt, Math.abs(driven)) : 0;
    return driven - Math.signum(driven) * loss;
  }

  public double timeConstant() {
    return maxVelocity / stallAcceleration;
  }
}
