package org.firstinspires.ftc.teamcode.auto.utils;

import com.pedropathing.follower.Follower;
import com.pedropathing.math.Pose;

import org.firstinspires.ftc.teamcode.utils.Robot;

import java.io.File;
import java.lang.reflect.Method;

// Runs on the computer before every APK build: ./gradlew :TeamCode:optimizeAutoPaths
// Arguments: <compiled classes dir> <output .properties>
public final class OptimizeAutoPaths {
  private static final String AUTO_PACKAGE = "org.firstinspires.ftc.teamcode.auto";

  private OptimizeAutoPaths() {}

  public static void main(String[] args) throws Exception {
    File classes = new File(args[0]);
    File output = new File(args[1]);
    System.out.printf("Optimizing auto paths in %s around %d obstacles%n",
      AUTO_PACKAGE, OptimizedPaths.obstacles.size());
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

  static RefinedPath optimize(Pose start, Pose end) {
    return PathRefiner.refine(start, end, OptimizedPaths.obstacles);
  }
}
