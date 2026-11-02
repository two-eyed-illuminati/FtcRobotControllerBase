package org.firstinspires.ftc.teamcode.auto.utils.sim;

import com.pedropathing.algorithm.Foresight;
import com.pedropathing.drivetrain.DrivePowers;
import com.pedropathing.follower.Follower;
import com.pedropathing.math.Pose;
import com.pedropathing.paths.Path;
import com.pedropathing.paths.PathSegment;

import java.util.ArrayList;
import java.util.List;

/**
 * Runs the real Follower and Foresight against simulated physics.
 * Paths run in order, like Ivy's follow(): the next path starts as soon as
 * follower.atParametricEnd() is true, while the robot is still moving.
 */
public final class PathSimulator {
  public static final double CONTROL_DT = 0.01;
  private static final double MAX_PATH_SECONDS = 20;
  private static final double MAX_SETTLE_SECONDS = 3;
  private static final double SETTLED_SPEED = 0.25;
  private static final double SETTLED_TURN_RATE = 0.05;

  private final SimRobot robot;

  public PathSimulator(SimRobot robot) {
    this.robot = robot;
  }

  public SimResult simulate(Pose start, List<Path> paths) {
    SimResult result = new SimResult(CONTROL_DT, robot.assumptions);

    SimPhysics physics = new SimPhysics(SimRobot.FORWARD, SimRobot.STRAFE, SimRobot.TURN);
    SimDrivetrain drivetrain = new SimDrivetrain();
    Foresight foresight = new Foresight(robot.config);
    Follower follower = new Follower(new SimLocalizer(physics), drivetrain, foresight);
    follower.setPose(start);

    int firstSegment = 0;
    List<Integer> firstSegments = new ArrayList<>();
    for (int group = 0; group < paths.size(); group++) {
      firstSegments.add(firstSegment);
      List<PathSegment> segments = paths.get(group).getSegments();
      for (int i = 0; i < segments.size(); i++) {
        result.addSegment(group, i, segments.get(i).curve.length());
      }
      result.addGroup(group, firstSegment, segments.size());
      firstSegment += segments.size();
    }

    State state = new State(result, physics, drivetrain, follower, foresight);
    for (int group = 0; group < paths.size(); group++) {
      follower.follow(paths.get(group));
      state.beginGroup(group, firstSegments.get(group), paths.get(group).getSegments().size());
      double groupStart = state.time();
      while (!follower.atParametricEnd()) {
        if (state.time() - groupStart > MAX_PATH_SECONDS) {
          result.warn("Path " + group + " did not finish within " + MAX_PATH_SECONDS
              + " s of simulated time; the simulation stopped there.");
          return result.finish(state.time(), physics, paths, start);
        }
        state.tick();
      }
      result.event("pathEnd", state.time(), group, state.lastSegment(), physics.worldSpeed());
    }

    double settleStart = state.time();
    while (!state.settled() && state.time() - settleStart < MAX_SETTLE_SECONDS) state.tick();
    if (!state.settled()) {
      result.warn("The robot was still moving " + MAX_SETTLE_SECONDS
          + " s after the last path ended.");
    }
    result.event("stop", state.time(), paths.size() - 1, state.lastSegment(), physics.worldSpeed());
    return result.finish(state.time(), physics, paths, start);
  }

  private static final class State {
    private final SimResult result;
    private final SimPhysics physics;
    private final SimDrivetrain drivetrain;
    private final Follower follower;
    private final Foresight foresight;
    private int ticks;
    private int group;
    private int firstSegment;
    private int segmentCount;
    private int lastSegment = -1;

    State(SimResult result, SimPhysics physics, SimDrivetrain drivetrain,
          Follower follower, Foresight foresight) {
      this.result = result;
      this.physics = physics;
      this.drivetrain = drivetrain;
      this.follower = follower;
      this.foresight = foresight;
    }

    double time() {
      return ticks * CONTROL_DT;
    }

    int lastSegment() {
      return lastSegment;
    }

    void beginGroup(int group, int firstSegment, int segmentCount) {
      this.group = group;
      this.firstSegment = firstSegment;
      this.segmentCount = segmentCount;
    }

    boolean settled() {
      return physics.worldSpeed() < SETTLED_SPEED && Math.abs(physics.omega()) < SETTLED_TURN_RATE;
    }

    void tick() {
      follower.update(CONTROL_DT);
      boolean following = follower.following();
      int segment = following
          ? firstSegment + Math.max(0, Math.min(segmentCount - 1, follower.pathIndex()))
          : Math.max(lastSegment, firstSegment);
      if (segment != lastSegment) {
        boolean sameGroup = lastSegment >= 0 && result.groupOfSegment(segment) == result.groupOfSegment(lastSegment);
        result.event(sameGroup ? "join" : "pathStart", time(), group, segment, physics.worldSpeed());
      }
      lastSegment = segment;

      DrivePowers applied = drivetrain.applied();
      double closestT = following ? follower.parametricCompletion() : 1.0;
      result.sample(time(), physics.pose(), physics.worldSpeed(), physics.omega(),
          foresight.targetVelocity(), closestT, foresight.isBraking(), segment, group,
          follower.holding(), applied);

      physics.advance(applied, CONTROL_DT);
      ticks++;
    }
  }
}
