package org.firstinspires.ftc.teamcode.auto.utils;

import com.pedropathing.follower.Follower;
import com.pedropathing.math.Pose;

import org.firstinspires.ftc.teamcode.auto.utils.sim.PathSimulator;
import org.firstinspires.ftc.teamcode.auto.utils.sim.SimRobot;
import org.firstinspires.ftc.teamcode.utils.Robot;
import org.firstinspires.ftc.teamcode.utils.pedro.Constants;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Collections;

// Runs on the computer before every APK build: ./gradlew :TeamCode:optimizeAutoPaths
// Arguments: <compiled classes dir> <output .properties>
public final class OptimizeAutoPaths {
  private static final String AUTO_PACKAGE = "org.firstinspires.ftc.teamcode.auto";
  private static final double FIRST_STEP = 16;
  private static final double LAST_STEP = 0.5;

  private OptimizeAutoPaths() {}

  public static void main(String[] args) throws Exception {
    File classes = new File(args[0]);
    File output = new File(args[1]);
    System.out.printf("Optimizing auto paths in %s, compass steps %.1f to %.1f in%n",
      AUTO_PACKAGE, FIRST_STEP, LAST_STEP);
    OptimizedPaths.optimizer = OptimizeAutoPaths::optimize;

    File autoDir = new File(classes, AUTO_PACKAGE.replace('.', '/'));
    for (File file : autoDir.listFiles((dir, name) -> name.endsWith(".class") && !name.contains("$"))) {
      Class<?> auto = Class.forName(AUTO_PACKAGE + "." + file.getName().replace(".class", ""));
      Method createRoutine;
      try {
        createRoutine = auto.getMethod("createRoutine", Follower.class);
      } catch (NoSuchMethodException e) {
        continue;
      }
      for (Robot.Alliance alliance : Robot.Alliance.values()) {
        System.out.println(auto.getSimpleName() + " " + alliance + ":");
        Robot.alliance = alliance;
        createRoutine.invoke(null, (Object) null);
      }
    }
    OptimizedPaths.save(output);
  }

  /** Compass search over the two inner control points of a cubic, scored by simulated drive time. */
  static double[] optimize(Pose start, Pose end) {
    double[] controls = {
      start.x() + (end.x() - start.x()) / 3, start.y() + (end.y() - start.y()) / 3,
      start.x() + (end.x() - start.x()) * 2 / 3, start.y() + (end.y() - start.y()) * 2 / 3
    };
    double straight = driveTime(start, end, controls);
    double best = straight;
    int simulations = 1;
    for (double step = FIRST_STEP; step >= LAST_STEP; step /= 2) {
      boolean improved = true;
      while (improved) {
        improved = false;
        for (int i = 0; i < controls.length; i++) {
          for (double delta : new double[] {step, -step}) {
            controls[i] += delta;
            double time = driveTime(start, end, controls);
            simulations++;
            if (time < best) {
              best = time;
              improved = true;
            } else {
              controls[i] -= delta;
            }
          }
        }
      }
    }
    System.out.printf("  %s -> %s: straight %.2f s, optimized %.2f s, %d simulations%n",
      start, end, straight, best, simulations);
    return controls;
  }

  private static double driveTime(Pose start, Pose end, double[] controls) {
    return new PathSimulator(SimRobot.create(Constants.foresightConfig))
      .simulate(start, Collections.singletonList(OptimizedPaths.cubic(start, end, controls)))
      .duration();
  }
}
