package org.firstinspires.ftc.teamcode.auto.utils.sim;

import com.pedropathing.localization.Localizer;
import com.pedropathing.localization.MotionState;
import com.pedropathing.math.Pose;

/** Reports the simulated robot's true state; the physics advances outside of update(). */
public final class SimLocalizer implements Localizer {
  private final SimPhysics physics;

  public SimLocalizer(SimPhysics physics) {
    this.physics = physics;
  }

  @Override
  public void setPose(Pose pose) {
    physics.setPose(pose);
  }

  @Override
  public MotionState state() {
    return MotionState.ofTwist(physics.pose(), physics.twist());
  }

  @Override
  public void update() {}

  @Override
  public void reset() {
    physics.setPose(Pose.zero());
  }
}
