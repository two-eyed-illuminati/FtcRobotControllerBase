package org.firstinspires.ftc.teamcode.utils.pedro.procedures;

import com.pedropathing.drivetrain.DrivePowers;
import com.pedropathing.drivetrain.Drivetrain;
import com.pedropathing.math.Pose;
import com.pedropathing.revhub.localizers.PinpointConfig;
import com.pedropathing.revhub.localizers.PinpointLocalizer;
import com.pedropathing.tuning.autotune.TuningOpMode;
import com.pedropathing.utils.Angle;
import com.qualcomm.hardware.gobilda.GoBildaPinpointDriver;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.qualcomm.robotcore.util.RobotLog;

import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.OptionalDouble;
import java.util.function.Function;

class PinpointSpinOffsets extends TuningOpMode<PinpointSpinOffsets.Result> {
  private static final String TAG = "PinpointSpinOffsets";
  private static final double MIN_STEP_RADIANS = Math.toRadians(2);
  private static final double MAX_SPIN_SECONDS = 60;
  private static final double SETTLE_SECONDS = 1;
  private static final int MIN_SAMPLES = 50;
  private static final double MAX_RELATIVE_RESIDUAL = 0.15;
  private static final double MAX_OVERSHOOT_RADIANS = Math.toRadians(30);
  private static final double MIN_COMPLETION = 0.95;

  static class Step {
    final double heading;
    final double forward;
    final double lateral;

    Step(double heading, double forward, double lateral) {
      this.heading = heading;
      this.forward = forward;
      this.lateral = lateral;
    }
  }

  static class Result {
    final double xPodOffset;
    final double yPodOffset;
    final double rmsResidual;
    final double relativeResidual;
    final int sampleCount;
    final double totalTurnRadians;
    final Pose finalPose;
    final List<String> warnings;

    Result(double xPodOffset, double yPodOffset, double rmsResidual, double relativeResidual, int sampleCount,
           double totalTurnRadians, Pose finalPose, List<String> warnings) {
      this.xPodOffset = xPodOffset;
      this.yPodOffset = yPodOffset;
      this.rmsResidual = rmsResidual;
      this.relativeResidual = relativeResidual;
      this.sampleCount = sampleCount;
      this.totalTurnRadians = totalTurnRadians;
      this.finalPose = finalPose;
      this.warnings = warnings;
    }

    List<Double> offsets() {
      return List.of(xPodOffset, yPodOffset);
    }
  }

  private final String name;
  private final PinpointTuner.PodType podType;
  private final OptionalDouble customPodScalar;
  private final boolean forwardPodReversed;
  private final boolean strafePodReversed;
  private final Function<HardwareMap, Drivetrain> drivetrainFactory;
  private final double turnPower;
  private final int turns;

  private double previousHeading;
  private double unwrappedHeading;

  public PinpointSpinOffsets(String name, PinpointTuner.PodType podType, OptionalDouble customPodScalar,
                             boolean forwardPodReversed, boolean strafePodReversed,
                             Function<HardwareMap, Drivetrain> drivetrainFactory, double turnPower, int turns) {
    super("Offsets Identification",
        "Automatically identifies the offsets for your Pinpoint localizer. \n"
            + "Clear the area around the robot. The robot will spin in place for " + turns + " whole turns. \n"
            + "Click stop at any time to stop the robot.",
        true);
    this.name = name;
    this.podType = podType;
    this.customPodScalar = customPodScalar;
    this.forwardPodReversed = forwardPodReversed;
    this.strafePodReversed = strafePodReversed;
    this.drivetrainFactory = drivetrainFactory;
    this.turnPower = turnPower;
    this.turns = turns;
  }

  @Override
  protected Result runTuningOpMode() throws InterruptedException {
    RobotLog.ii(TAG, "Config: turnPower=%.2f turns=%d podType=%s customTicksPerInch=%s forwardPodReversed=%b strafePodReversed=%b",
        turnPower, turns, podType, customPodScalar, forwardPodReversed, strafePodReversed);

    PinpointLocalizer localizer = new PinpointLocalizer(hardwareMap, zeroOffsetConfig());
    Drivetrain drivetrain = drivetrainFactory.apply(hardwareMap);
    if (customPodScalar.isPresent()) {
      localizer.reset();
    }
    localizer.setPose(Pose.zero());
    localizer.update();

    Thread.sleep(1000);
    waitForStart();
    localizer.setPose(Pose.zero());
    localizer.update();

    List<Step> steps = new ArrayList<>();
    try {
      spin(localizer, drivetrain, steps);
    } finally {
      drivetrain.stop(true);
    }
    settle(localizer);

    Result result = fit(steps, localizer.pose());
    logSummary(result);
    return result;
  }

  private PinpointConfig zeroOffsetConfig() {
    return new PinpointConfig(c -> {
      c.name.set(name);
      c.xPodDirection.set(forwardPodReversed ? GoBildaPinpointDriver.EncoderDirection.REVERSED : GoBildaPinpointDriver.EncoderDirection.FORWARD);
      c.yPodDirection.set(strafePodReversed ? GoBildaPinpointDriver.EncoderDirection.REVERSED : GoBildaPinpointDriver.EncoderDirection.FORWARD);
      if (customPodScalar.isPresent()) {
        c.encoderResolutionUnit.set(DistanceUnit.INCH);
        c.ticksPerUnit.set(OptionalDouble.of(customPodScalar.getAsDouble()));
        c.resetMode.set(PinpointLocalizer.ResetMode.RESET_AND_RECALIBRATE_IMU);
      } else {
        c.podType.set(podType == PinpointTuner.PodType.SWING_ARM ? GoBildaPinpointDriver.GoBildaOdometryPods.goBILDA_SWINGARM_POD : GoBildaPinpointDriver.GoBildaOdometryPods.goBILDA_4_BAR_POD);
      }
      c.xPodOffset.set(0.0);
      c.yPodOffset.set(0.0);
      c.globalDistanceUnit.set(DistanceUnit.INCH);
      c.offsetUnits.set(DistanceUnit.INCH);
    });
  }

  private void spin(PinpointLocalizer localizer, Drivetrain drivetrain, List<Step> steps) {
    double targetTurn = turns * 2 * Math.PI;
    Pose sampleStart = localizer.pose();
    double sampleStartHeading = 0;
    previousHeading = sampleStart.heading();
    unwrappedHeading = 0;

    ElapsedTime timer = new ElapsedTime();
    while (opModeIsActive() && Math.abs(unwrappedHeading) < targetTurn) {
      if (timer.seconds() > MAX_SPIN_SECONDS) {
        RobotLog.ww(TAG, "Spin timed out after %.0f s at %.1f deg; the robot may not be turning", MAX_SPIN_SECONDS, Math.toDegrees(unwrappedHeading));
        return;
      }
      drivetrain.drive(new DrivePowers(0, 0, turnPower), false);
      localizer.update();
      Pose pose = localizer.pose();
      trackHeading(pose);

      double headingStep = unwrappedHeading - sampleStartHeading;
      if (Math.abs(headingStep) >= MIN_STEP_RADIANS) {
        steps.add(toRobotFrameStep(sampleStart, pose, sampleStart.heading() + headingStep / 2, headingStep));
        sampleStart = pose;
        sampleStartHeading = unwrappedHeading;
      }

      telemetry.addData("turns", "%.2f / %d", Math.abs(unwrappedHeading) / (2 * Math.PI), turns);
      telemetry.addData("samples", steps.size());
      telemetry.addData("pose", pose);
      telemetry.update();
    }
  }

  private void settle(PinpointLocalizer localizer) {
    ElapsedTime timer = new ElapsedTime();
    while (timer.seconds() < SETTLE_SECONDS) {
      localizer.update();
      trackHeading(localizer.pose());
    }
  }

  private void trackHeading(Pose pose) {
    unwrappedHeading += Angle.normalizeSigned(pose.heading() - previousHeading);
    previousHeading = pose.heading();
  }

  private static Step toRobotFrameStep(Pose from, Pose to, double midHeading, double headingStep) {
    double dx = to.x() - from.x();
    double dy = to.y() - from.y();
    double forward = dx * Math.cos(midHeading) + dy * Math.sin(midHeading);
    double lateral = -dx * Math.sin(midHeading) + dy * Math.cos(midHeading);
    return new Step(headingStep, forward, lateral);
  }

  private Result fit(List<Step> steps, Pose finalPose) {
    double headingSquared = 0, headingForward = 0, headingLateral = 0;
    for (Step step : steps) {
      headingSquared += step.heading * step.heading;
      headingForward += step.heading * step.forward;
      headingLateral += step.heading * step.lateral;
    }
    double forwardSlope = headingSquared > 0 ? headingForward / headingSquared : 0;
    double lateralSlope = headingSquared > 0 ? headingLateral / headingSquared : 0;

    double residualSquared = 0, stepSquared = 0;
    for (Step step : steps) {
      double forwardError = step.forward - forwardSlope * step.heading;
      double lateralError = step.lateral - lateralSlope * step.heading;
      residualSquared += forwardError * forwardError + lateralError * lateralError;
      stepSquared += step.forward * step.forward + step.lateral * step.lateral;
    }
    int terms = 2 * steps.size();
    double rms = terms > 0 ? Math.sqrt(residualSquared / terms) : 0;
    double relative = stepSquared > 0 ? Math.sqrt(residualSquared / stepSquared) : 0;

    List<String> warnings = warnings(steps.size(), relative);
    return new Result(-forwardSlope, lateralSlope, rms, relative, steps.size(), unwrappedHeading, finalPose, warnings);
  }

  private List<String> warnings(int sampleCount, double relativeResidual) {
    List<String> warnings = new ArrayList<>();
    double targetTurn = turns * 2 * Math.PI;
    double turned = Math.abs(unwrappedHeading);

    if (unwrappedHeading < 0) {
      warnings.add("Heading decreased while turning with positive power. Check drivetrain motor directions.");
    }
    if (turned < targetTurn * MIN_COMPLETION) {
      warnings.add(String.format(Locale.US, "Turned only %.0f deg of the %.0f deg target. The spin was stopped early or the robot stalled.",
          Math.toDegrees(turned), Math.toDegrees(targetTurn)));
    } else if (turned > targetTurn + MAX_OVERSHOOT_RADIANS) {
      warnings.add(String.format(Locale.US, "Overshot the %.0f deg target by %.0f deg. Lower the turn power.",
          Math.toDegrees(targetTurn), Math.toDegrees(turned - targetTurn)));
    }
    if (sampleCount < MIN_SAMPLES) {
      warnings.add("Only " + sampleCount + " samples were recorded (minimum " + MIN_SAMPLES + "). The fit is unreliable.");
    }
    if (relativeResidual > MAX_RELATIVE_RESIDUAL) {
      warnings.add(String.format(Locale.US, "Fit residual is %.0f%% of the step size (maximum %.0f%%). Check pod direction, wheel slip and pod mounting.",
          relativeResidual * 100, MAX_RELATIVE_RESIDUAL * 100));
    }
    return warnings;
  }

  private void logSummary(Result result) {
    RobotLog.ii(TAG, "Summary: xPodOffset=%.4f yPodOffset=%.4f rmsResidual=%.4f in relativeResidual=%.3f samples=%d totalTurn=%.1f deg finalPose=%s",
        result.xPodOffset, result.yPodOffset, result.rmsResidual, result.relativeResidual, result.sampleCount,
        Math.toDegrees(result.totalTurnRadians), result.finalPose);
    for (String warning : result.warnings) {
      RobotLog.ww(TAG, "Warning: %s", warning);
    }
    if (result.warnings.isEmpty()) {
      RobotLog.ii(TAG, "Fit within expected ranges");
    }
  }
}
