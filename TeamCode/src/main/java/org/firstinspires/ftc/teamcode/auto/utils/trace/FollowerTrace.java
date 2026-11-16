package org.firstinspires.ftc.teamcode.auto.utils.trace;

import com.pedropathing.follower.Follower;
import com.pedropathing.math.Pose;
import com.pedropathing.math.Velocity;
import com.pedropathing.paths.Path;
import com.qualcomm.robotcore.util.RobotLog;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Records what the real Follower did, one row per loop, and writes it as columnar JSON that the
 * path viewer loads as a run. Rows stay in memory during the run; the file is written in save().
 * Pull it with: adb pull /sdcard/FIRST/traces/latest.trace.json
 */
public final class FollowerTrace {
  private static final String TAG = "FollowerTrace";
  private static final String DIRECTORY = "/sdcard/FIRST/traces";
  private static final String LATEST = "latest.trace.json";
  private static final int MAX_SAMPLES = 60_000;
  private static final String[] COLUMNS = {
      "t", "x", "y", "heading", "vx", "vy", "omega", "closestT", "mode", "pathIndex", "group"};

  private final String name;
  private final long startNanos = System.nanoTime();
  private final List<double[]> samples = new ArrayList<>();
  private Path lastPath;
  private int group = -1;
  private boolean full;

  public FollowerTrace(String name) {
    this.name = name;
    RobotLog.ii(TAG, "recording %s to %s (max %d samples, columns %s)",
        name, DIRECTORY, MAX_SAMPLES, Arrays.toString(COLUMNS));
  }

  public void record(Follower follower) {
    if (samples.size() >= MAX_SAMPLES) {
      if (!full) RobotLog.ee(TAG, "%s reached %d samples; later loops are not recorded", name, MAX_SAMPLES);
      full = true;
      return;
    }
    Path path = follower.currentPath();
    if (path != null && path != lastPath) group++;
    lastPath = path;

    Pose pose = follower.pose();
    Velocity velocity = follower.velocity();
    boolean following = follower.following();
    samples.add(new double[] {
        (System.nanoTime() - startNanos) / 1e9, pose.x(), pose.y(), pose.heading(),
        velocity.vx, velocity.vy, velocity.omega,
        following ? follower.parametricCompletion() : 1.0,
        follower.mode().ordinal(), following ? follower.pathIndex() : -1, group});
  }

  public void save() {
    String json = toJson();
    try {
      File directory = new File(DIRECTORY);
      directory.mkdirs();
      byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
      File file = new File(directory, name + "-" + System.currentTimeMillis() + ".trace.json");
      write(file, bytes);
      write(new File(directory, LATEST), bytes);
      RobotLog.ii(TAG, "saved %d samples over %.2f s, %d path groups, %d bytes to %s",
          samples.size(), duration(), group + 1, bytes.length, file.getPath());
    } catch (IOException e) {
      RobotLog.ee(TAG, e, "could not save %s", name);
    }
  }

  private static void write(File file, byte[] bytes) throws IOException {
    try (FileOutputStream out = new FileOutputStream(file)) {
      out.write(bytes);
    }
  }

  private double duration() {
    return samples.isEmpty() ? 0 : samples.get(samples.size() - 1)[0];
  }

  /** Mode ordinals follow Follower.Mode: 0 FOLLOW, 1 HOLD, 2 MANUAL, 3 IDLE. */
  String toJson() {
    StringBuilder json = new StringBuilder();
    json.append("{\"kind\":\"trace\",\"version\":1,\"name\":\"").append(name).append('"');
    json.append(",\"modes\":[");
    Follower.Mode[] modes = Follower.Mode.values();
    for (int i = 0; i < modes.length; i++) {
      if (i > 0) json.append(',');
      json.append('"').append(modes[i].name()).append('"');
    }
    json.append("],\"samples\":{");
    for (int c = 0; c < COLUMNS.length; c++) {
      if (c > 0) json.append(',');
      json.append('"').append(COLUMNS[c]).append("\":[");
      for (int i = 0; i < samples.size(); i++) {
        if (i > 0) json.append(',');
        json.append(num(samples.get(i)[c]));
      }
      json.append(']');
    }
    return json.append("}}").toString();
  }

  private static String num(double value) {
    if (!Double.isFinite(value)) return "null";
    if (value == Math.rint(value) && Math.abs(value) < 1e9) return Long.toString((long) value);
    return String.format(Locale.ROOT, "%.5f", value);
  }
}
