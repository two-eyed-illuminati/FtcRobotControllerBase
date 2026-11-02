package org.firstinspires.ftc.teamcode.auto.utils.sim;

import static com.pedropathing.api.Paths.curve;
import static com.pedropathing.api.Paths.line;
import static com.pedropathing.api.Paths.path;

import com.pedropathing.math.Pose;
import com.pedropathing.paths.Path;

import org.firstinspires.ftc.teamcode.auto.utils.AutoBuilder;
import org.firstinspires.ftc.teamcode.auto.utils.ExportAutoPath;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

/**
 * Prints join and end speeds of the simulation for routines where the expected result is known.
 * With an argument, also exports each routine as a .pp and .sim.json into that directory.
 */
public final class SimCheck {
  private static final double JOIN_WINDOW = 0.3;

  private SimCheck() {}

  public static void main(String[] args) throws Exception {
    SimRobot robot = SimRobot.create();
    Pose a = new Pose(0, 0, 0);
    Pose b = new Pose(48, 0, 0);
    Pose c = new Pose(96, 0, 0);
    Pose d = new Pose(96, 48, 0);

    System.out.printf("%-34s %8s %16s %10s %10s %10s%n",
        "scenario", "time s", "join speeds", "min +-0.3s", "end speed", "end err in");
    Pose bend = new Pose(70, 0, 0);
    check(robot, args, "join-straight", "path(a,b) straight, 2 x 48 in", a, path(seg(a, b), seg(b, c)));
    check(robot, args, "join-bend", "path(a,b) with 90 deg bend", a, path(seg(a, b), seg(b, c), seg(c, d)));
    check(robot, args, "join-curve", "path(a,b) curved + line", a, path(
        curve(a, new Pose(40, 30, 0), bend).linear(a, bend), seg(bend, c)));
    check(robot, args, "separate", "two separate paths", a, seg(a, b), seg(b, c));
    check(robot, args, "single-line", "one 96 in line (no join)", a, seg(a, c));
  }

  private static Path seg(Pose from, Pose to) {
    return line(from, to).linear(from, to);
  }

  private static void check(SimRobot robot, String[] args, String id, String name, Pose start,
                            Path... pathArray) throws Exception {
    List<Path> paths = Arrays.asList(pathArray);
    if (args.length > 0) {
      AutoBuilder auto = new AutoBuilder().startAt(start);
      for (Path path : paths) auto.followPath(path);
      ExportAutoPath.export(auto, Paths.get(args[0], id + ".pp"));
    }
    SimResult result = new PathSimulator(robot).simulate(start, paths);
    StringBuilder joins = new StringBuilder();
    double minNearJoin = Double.POSITIVE_INFINITY;
    for (SimResult.Event event : result.events()) {
      boolean handoff = event.type.equals("pathStart") && event.time > 0;
      if (!event.type.equals("join") && !handoff) continue;
      if (joins.length() > 0) joins.append(" / ");
      joins.append(String.format("%.2f", event.speed));
      minNearJoin = Math.min(minNearJoin,
          result.minSpeedBetween(event.time - JOIN_WINDOW, event.time + JOIN_WINDOW));
    }
    System.out.printf("%-34s %8.2f %16s %10s %10.2f %10.2f%n", name, result.duration(),
        joins.length() == 0 ? "-" : joins,
        Double.isInfinite(minNearJoin) ? "-" : String.format("%.2f", minNearJoin),
        result.endSpeed(), result.endPositionError());
    for (String warning : result.warnings()) System.out.println("  warning: " + warning);
  }
}
