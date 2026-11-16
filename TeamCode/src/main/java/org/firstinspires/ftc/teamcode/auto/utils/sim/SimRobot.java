package org.firstinspires.ftc.teamcode.auto.utils.sim;

import com.pedropathing.algorithm.ForesightConfig;
import com.pedropathing.controllers.Controller;
import com.pedropathing.math.Matrix;
import com.pedropathing.math.Vector2D;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The robot the simulation runs. The ForesightConfig is the tuned one from the biobuzz project
 * (biobuzz/.../utils/pedro/Constants.java, commit 5184a2e). The physics is fitted to that config:
 * - top speed and natural deceleration are the config values;
 * - the drive time constant follows from the tuner's gain formula kP = tau * alpha^2 / K;
 * - the turn axis has no tuned top speed, so TURN_TERMINAL_VELOCITY and TURN_FRICTION are assumed.
 */
public final class SimRobot {
  static final double FORWARD_TERMINAL_VELOCITY = 72.37704404328076;
  static final double STRAFE_TERMINAL_VELOCITY = 54.82465388665514;
  static final double FORWARD_NATURAL_DECELERATION = 28.496999108568463;
  static final double STRAFE_NATURAL_DECELERATION = 82.80357292419318;
  static final double FORWARD_PRIMARY_KP = 0.46143747753891484;
  static final double FORWARD_SECONDARY_KP = 0.17048881811414734;
  static final double STRAFE_PRIMARY_KP = 0.7421916068070539;
  static final double STRAFE_SECONDARY_KP = 0.2742199669902264;
  static final double HEADING_KP = 7.394465419100555;
  static final double TRANSLATIONAL_SWITCH_DISTANCE = 2.5;
  static final double TRANSLATIONAL_ALPHA_LARGE = 10.2;
  static final double HEADING_ALPHA = 18.25;
  static final double COAST_KV = 0.013828078266724485;
  static final double BRAKE_KV = 0.011753866526715812;
  static final double[] HEADING_BRAKING = {0.057710307439896885, 0.007305738388892705};
  static final double[] FORWARD_BRAKING = {0.10604301225944404, 0.0017051540772801824};
  static final double[] STRAFE_BRAKING = {0.07235600480491211, 0.0015567358791152273};

  static final double TURN_TERMINAL_VELOCITY = 4.5;
  static final double TURN_FRICTION = 5;
  static final double TUNER_BRAKING_POWER = 0.001;
  static final double BRAKING_CHECK_VELOCITY = 30;

  public static SimAxis FORWARD = SimAxis.fit(FORWARD_TERMINAL_VELOCITY,
      timeConstant(FORWARD_PRIMARY_KP, FORWARD_TERMINAL_VELOCITY), FORWARD_NATURAL_DECELERATION);
  public static SimAxis STRAFE = SimAxis.fit(STRAFE_TERMINAL_VELOCITY,
      timeConstant(STRAFE_PRIMARY_KP, STRAFE_TERMINAL_VELOCITY), STRAFE_NATURAL_DECELERATION);
  public static SimAxis TURN = SimAxis.fit(TURN_TERMINAL_VELOCITY,
      HEADING_KP * TURN_TERMINAL_VELOCITY / (HEADING_ALPHA * HEADING_ALPHA), TURN_FRICTION);

  public final ForesightConfig config;
  public final Map<String, Object> assumptions = new LinkedHashMap<>();

  private SimRobot(ForesightConfig config) {
    this.config = config;
  }

  public static SimRobot create() {
    Controller forwardPrimary = Controller.proportional(FORWARD_PRIMARY_KP);
    Controller forwardSecondary = Controller.proportional(FORWARD_SECONDARY_KP);
    Controller strafePrimary = Controller.proportional(STRAFE_PRIMARY_KP);
    Controller strafeSecondary = Controller.proportional(STRAFE_SECONDARY_KP);

    ForesightConfig config = new ForesightConfig(c -> {
      c.forwardTranslational.set(
          Controller.piecewise(forwardSecondary).put(TRANSLATIONAL_SWITCH_DISTANCE, forwardPrimary));
      c.strafeTranslational.set(
          Controller.piecewise(strafeSecondary).put(TRANSLATIONAL_SWITCH_DISTANCE, strafePrimary));
      c.coast.set(Controller.proportionalFeedforward(COAST_KV));
      c.brake.set(Controller.proportionalFeedforward(BRAKE_KV));
      c.headingFeedback.set(Controller.proportional(HEADING_KP));
      c.headingBrakeCoefficients.set(Vector2D.cartesian(HEADING_BRAKING[0], HEADING_BRAKING[1]));
      c.linearBrakeCoefficients.set(Matrix.diag(FORWARD_BRAKING[0], STRAFE_BRAKING[0]));
      c.quadraticBrakeCoefficients.set(Matrix.diag(FORWARD_BRAKING[1], STRAFE_BRAKING[1]));
      c.maxAchievableForwardVelocity.set(FORWARD_TERMINAL_VELOCITY);
      c.maxAchievableStrafeVelocity.set(STRAFE_TERMINAL_VELOCITY);
      c.naturalForwardDeceleration.set(FORWARD_NATURAL_DECELERATION);
      c.naturalStrafeDeceleration.set(STRAFE_NATURAL_DECELERATION);
    });

    SimRobot robot = new SimRobot(config);
    robot.describe();
    return robot;
  }

  private void describe() {
    assumptions.put("source", "biobuzz tuned ForesightConfig (Constants.java, commit 5184a2e)");
    assumptions.put("physicsModel", "v approaches power * topSpeed with the drive time constant; friction only while slowing; power 0 floats");
    describeAxis("forward", FORWARD, FORWARD_BRAKING);
    describeAxis("strafe", STRAFE, STRAFE_BRAKING);
    assumptions.put("turnMaxVelocity (assumed)", TURN.maxVelocity);
    assumptions.put("turnStallAcceleration", TURN.stallAcceleration);
    assumptions.put("turnFriction (assumed)", TURN.friction);
    assumptions.put("coastKV", COAST_KV);
    assumptions.put("brakeKV", BRAKE_KV);
    assumptions.put("headingKP", HEADING_KP);
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
