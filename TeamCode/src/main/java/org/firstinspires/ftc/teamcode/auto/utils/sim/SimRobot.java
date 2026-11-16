package org.firstinspires.ftc.teamcode.auto.utils.sim;

import com.pedropathing.algorithm.ForesightConfig;
import com.pedropathing.controllers.Controller;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The robot the simulation runs. The physics is fitted to a ForesightConfig:
 * - top speed and natural deceleration are the config values;
 * - the drive time constant follows from the tuner's gain formula kP = tau * alpha^2 / K;
 * - the turn axis has no tuned top speed, so TURN_TERMINAL_VELOCITY and TURN_FRICTION are assumed.
 * create(config) fits to the given config; pass Constants.foresightConfig to simulate the robot the follower drives.
 */
public final class SimRobot {
  static final double TRANSLATIONAL_ALPHA_LARGE = 10.2;
  static final double HEADING_ALPHA = 18.25;

  static final double TURN_TERMINAL_VELOCITY = 4.5;
  static final double TURN_FRICTION = 5;
  static final double TUNER_BRAKING_POWER = 0.001;
  static final double BRAKING_CHECK_VELOCITY = 30;

  static final double PROPORTIONAL_PROBE_ERROR = 10;

  public final ForesightConfig config;
  public final SimAxis forward;
  public final SimAxis strafe;
  public final SimAxis turn;
  public final Map<String, Object> assumptions = new LinkedHashMap<>();

  private final double coastKv;
  private final double brakeKv;
  private final double headingKp;
  private final double[] forwardBraking;
  private final double[] strafeBraking;

  private SimRobot(ForesightConfig config, String source) {
    this.config = config;
    double forwardTop = config.maxAchievableForwardVelocity.get();
    double strafeTop = config.maxAchievableStrafeVelocity.get();
    double forwardKp = proportionalGain(config.forwardTranslational.get(), PROPORTIONAL_PROBE_ERROR);
    double strafeKp = proportionalGain(config.strafeTranslational.get(), PROPORTIONAL_PROBE_ERROR);
    headingKp = proportionalGain(config.headingFeedback.get(), 1);
    coastKv = config.coast.get().calculate(1, 0);
    brakeKv = config.brake.get().calculate(1, 0);
    forwardBraking = brakingCoefficients(config, 0);
    strafeBraking = brakingCoefficients(config, 1);

    forward = SimAxis.fit(forwardTop, timeConstant(forwardKp, forwardTop),
        config.naturalForwardDeceleration.get());
    strafe = SimAxis.fit(strafeTop, timeConstant(strafeKp, strafeTop),
        config.naturalStrafeDeceleration.get());
    turn = SimAxis.fit(TURN_TERMINAL_VELOCITY,
        headingKp * TURN_TERMINAL_VELOCITY / (HEADING_ALPHA * HEADING_ALPHA), TURN_FRICTION);
    describe(source);
  }

  public static SimRobot create(ForesightConfig config) {
    return new SimRobot(config, "supplied ForesightConfig");
  }

  private static double proportionalGain(Controller controller, double error) {
    return controller.calculate(0, error) / error;
  }

  private static double[] brakingCoefficients(ForesightConfig config, int axis) {
    return new double[] {
        config.linearBrakeCoefficients.get().get(axis, axis),
        config.quadraticBrakeCoefficients.get().get(axis, axis)};
  }

  private void describe(String source) {
    assumptions.put("source", source);
    assumptions.put("physicsModel", "v approaches power * topSpeed with the drive time constant; friction only while slowing; power 0 floats");
    describeAxis("forward", forward, forwardBraking);
    describeAxis("strafe", strafe, strafeBraking);
    assumptions.put("turnMaxVelocity (assumed)", turn.maxVelocity);
    assumptions.put("turnStallAcceleration", turn.stallAcceleration);
    assumptions.put("turnFriction (assumed)", turn.friction);
    assumptions.put("coastKV", coastKv);
    assumptions.put("brakeKV", brakeKv);
    assumptions.put("headingKP", headingKp);
    assumptions.put("maxBrakingPower", config.maxBrakingPower.get());
    assumptions.put("brakeAggression", config.brakeAggression.get());
    assumptions.put("brakeAtEnd", config.brakeAtEnd.get());
    assumptions.put("pathSkip", config.pathSkip.get());
    assumptions.put("parametricTConstraint", config.parametricTConstraint.get());
  }

  private void describeAxis(String name, SimAxis axis, double[] braking) {
    double predicted = braking[0] * BRAKING_CHECK_VELOCITY + braking[1] * BRAKING_CHECK_VELOCITY * BRAKING_CHECK_VELOCITY;
    assumptions.put(name + "MaxVelocity", axis.maxVelocity);
    assumptions.put(name + "StallAcceleration", axis.stallAcceleration);
    assumptions.put(name + "Friction", axis.friction);
    assumptions.put(name + "TopSpeed", terminalVelocity(axis));
    assumptions.put(name + "StopFrom30Sim", stoppingDistance(axis, BRAKING_CHECK_VELOCITY));
    assumptions.put(name + "StopFrom30Config", predicted);
  }

  private static double timeConstant(double primaryKp, double terminalVelocity) {
    return primaryKp * terminalVelocity / (TRANSLATIONAL_ALPHA_LARGE * TRANSLATIONAL_ALPHA_LARGE);
  }

  static double terminalVelocity(SimAxis axis) {
    double velocity = 0;
    for (int i = 0; i < 20_000; i++) velocity = axis.step(velocity, 1.0, SimPhysics.SUB_STEP);
    return velocity;
  }

  /** Distance covered while braking at the tuner's braking power, the quantity the braking coefficients predict. */
  static double stoppingDistance(SimAxis axis, double velocity) {
    double position = 0;
    while (Math.abs(velocity) > 0.25) {
      velocity = axis.step(velocity, -TUNER_BRAKING_POWER, SimPhysics.SUB_STEP);
      position += velocity * SimPhysics.SUB_STEP;
    }
    return position;
  }
}
