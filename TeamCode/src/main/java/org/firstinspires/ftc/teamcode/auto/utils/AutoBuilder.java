package org.firstinspires.ftc.teamcode.auto.utils;

import static com.pedropathing.api.Paths.curve;
import static com.pedropathing.api.Paths.line;
import static com.pedropathing.ivy.groups.Groups.sequential;
import static com.pedropathing.ivy.pedro.PedroCommands.follow;

import com.pedropathing.follower.Follower;
import com.pedropathing.ivy.Command;
import com.pedropathing.math.Pose;
import com.pedropathing.paths.Path;

import org.firstinspires.ftc.teamcode.utils.Robot;

import java.util.ArrayList;
import java.util.List;

public class AutoBuilder {
  public Follower follower;
  public List<Command> commands = new ArrayList<>();
  public PpExporter pp = new PpExporter();
  public Pose currentPose;

  public AutoBuilder() {
    this(null);
  }

  public AutoBuilder(Follower follower) {
    this.follower = follower;
  }

  public AutoBuilder startAt(Pose bluePose) {
    currentPose = Robot.toField(bluePose);
    pp.startAt(currentPose);
    if (follower != null) follower.setPose(currentPose);
    return this;
  }

  public AutoBuilder followPath(Path path) {
    commands.add(drive(path));
    return this;
  }

  public AutoBuilder then(Command command) {
    commands.add(command);
    return this;
  }

  public Command build() {
    return sequential(commands.toArray(new Command[0]));
  }

  public String buildPp() {
    return pp.build();
  }

  public Command drive(Path path) {
    Pose end = path.endPose();
    pp.add(path);
    currentPose = end;
    return follower == null ? Command.NOOP : follow(follower, path);
  }

  public enum Shape { LINE, CURVE }

  public static Pose CENTER = new Pose(72.0, 72.0, 0);

  private static double angleDistance(double a, double b) {
    return Math.abs(Math.atan2(Math.sin(a - b), Math.cos(a - b)));
  }

  public static class GoToCenter {
    public Pose endPose = CENTER;
  }

  public AutoBuilder goToCenter() {
    GoToCenter config = new GoToCenter();
    Pose facing180 = CENTER.withHeading(Math.PI);
    if (angleDistance(currentPose.heading(), Robot.toField(facing180).heading())
        < angleDistance(currentPose.heading(), Robot.toField(CENTER).heading())) {
      config.endPose = facing180;
    }
    return goToCenter(config);
  }

  public AutoBuilder goToCenter(GoToCenter config) {
    Pose end = Robot.toField(config.endPose);
    commands.add(drive(line(currentPose, end).linear(currentPose, end)));
    return this;
  }
}
