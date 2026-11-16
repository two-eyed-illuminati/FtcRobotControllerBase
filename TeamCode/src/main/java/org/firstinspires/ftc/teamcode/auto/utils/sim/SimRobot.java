package org.firstinspires.ftc.teamcode.auto.utils.sim;

import com.pedropathing.algorithm.ForesightConfig;
import com.pedropathing.controllers.Controller;
import com.pedropathing.math.Matrix;
import com.pedropathing.math.Vector2D;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The robot the simulation runs. The physics is fitted to a ForesightConfig:
 * - top speed and natural deceleration are the config values;
 * - the drive time constant follows from the tuner's gain formula kP = tau * alpha^2 / K;
 * - the turn axis has no tuned top speed, so TURN_TERMINAL_VELOCITY and TURN_FRICTION are assumed.
 * create(config) fits to the given config. create() uses the built-in default config, which is the tuned one
 * from the biobuzz project (biobuzz/.../utils/pedro/Constants.java, commit 5184a2e).
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

  public static SimRobot create() {
    return new SimRobot(defaultConfig(),
        "built-in default: biobuzz tuned ForesightConfig (Constants.java, commit 5184a2e)");
  }

  private static ForesightConfig defaultConfig() {
    Controller forwardPrimary = Controller.proportional(FORWARD_PRIMARY_KP);
    Controller forwardSecondary = Controller.proportional(FORWARD_SECONDARY_KP);
    Controller strafePrimary = Controller.proportional(STRAFE_PRIMARY_KP);
    Controller strafeSecondary = Controller.proportional(STRAFE_SECONDARY_KP);

    return new ForesightConfig(c -> {
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
