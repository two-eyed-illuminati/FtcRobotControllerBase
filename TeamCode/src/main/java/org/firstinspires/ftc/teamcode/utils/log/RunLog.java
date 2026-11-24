package org.firstinspires.ftc.teamcode.utils.log;

import com.pedropathing.follower.Follower;
import com.pedropathing.math.Pose;
import com.pedropathing.math.Velocity;
import com.pedropathing.paths.Path;
import com.qualcomm.robotcore.util.RobotLog;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;

/**
 * Streams one run to a line-based file that the path viewer opens: a header line, one row per
 * loop, and one line each time telemetry changes. Output is flushed every FLUSH_SECONDS, so a
 * crash loses at most that much. Pull it with: adb pull /sdcard/FIRST/traces
 */
public final class RunLog {
  private static final String TAG = "RunLog";
  private static final String DIRECTORY = "/sdcard/FIRST/traces";
  private static final String EXTENSION = ".run.jsonl";
  private static final double FLUSH_SECONDS = 0.5;
  private static final int MAX_LOGS = 50;
  private static final String[] COLUMNS = {
      "t", "x", "y", "heading", "vx", "vy", "omega", "closestT", "mode", "pathIndex", "group"};

  private final String name;
  private final File file;
  private final long startNanos = System.nanoTime();
  private BufferedWriter out;
  private Path lastPath;
  private int group = -1;
  private int rows;
  private int telemetryLines;
  private double lastFlushSeconds;

  public static RunLog open(String name) {
    return new RunLog(name);
  }

  private RunLog(String name) {
    this.name = name;
    File directory = new File(DIRECTORY);
    directory.mkdirs();
    int pruned = prune(directory);
    file = new File(directory, name + "-" + System.currentTimeMillis() + EXTENSION);
    try {
      out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8));
      append(header(), 0);
      RobotLog.ii(TAG, "recording %s to %s (flush %.1f s, keeping %d logs, pruned %d, columns %s)",
          name, file.getPath(), FLUSH_SECONDS, MAX_LOGS, pruned, Arrays.toString(COLUMNS));
    } catch (IOException e) {
      RobotLog.ee(TAG, e, "could not open %s; this run is not logged", file.getPath());
      out = null;
    }
  }

  public void sample(Follower follower) {
    if (out == null) return;
    Path path = follower.currentPath();
    if (path != null && path != lastPath) group++;
    lastPath = path;

    Pose pose = follower.pose();
    Velocity velocity = follower.velocity();
    boolean following = follower.following();
    double t = seconds();
    double[] row = {
        t, pose.x(), pose.y(), pose.heading(),
        velocity.vx, velocity.vy, velocity.omega,
        following ? follower.parametricCompletion() : 1.0,
        follower.mode().ordinal(), following ? follower.pathIndex() : -1, group};
    StringBuilder line = new StringBuilder("[");
    for (int i = 0; i < row.length; i++) {
      if (i > 0) line.append(',');
      line.append(number(row[i]));
    }
    append(line.append(']').toString(), t);
    rows++;
  }

  public void telemetry(Map<String, String> changed) {
    if (out == null) return;
    double t = seconds();
    StringBuilder line = new StringBuilder("{\"t\":").append(number(t)).append(",\"tel\":{");
    boolean first = true;
    for (Map.Entry<String, String> entry : changed.entrySet()) {
      if (!first) line.append(',');
      first = false;
      line.append(quote(entry.getKey())).append(':').append(quote(entry.getValue()));
    }
    append(line.append("}}").toString(), t);
    telemetryLines++;
  }

  public void close() {
    if (out == null) return;
    try {
      out.close();
    } catch (IOException e) {
      RobotLog.ee(TAG, e, "could not close %s", file.getPath());
    }
    out = null;
    if (rows == 0 && telemetryLines == 0) {
      RobotLog.ii(TAG, "%s logged nothing; deleted %s", name, file.getPath());
      file.delete();
      return;
    }
    RobotLog.ii(TAG, "saved %d rows and %d telemetry lines over %.2f s, %d path groups, %d bytes to %s",
        rows, telemetryLines, seconds(), group + 1, file.length(), file.getPath());
  }

  private String header() {
    StringBuilder json = new StringBuilder("{\"kind\":\"run\",\"version\":2,\"name\":")
        .append(quote(name)).append(",\"startedAt\":").append(System.currentTimeMillis())
        .append(",\"columns\":[");
    for (int i = 0; i < COLUMNS.length; i++) {
      if (i > 0) json.append(',');
      json.append(quote(COLUMNS[i]));
    }
    json.append("],\"modes\":[");
    Follower.Mode[] modes = Follower.Mode.values();
    for (int i = 0; i < modes.length; i++) {
      if (i > 0) json.append(',');
      json.append(quote(modes[i].name()));
    }
    return json.append("]}").toString();
  }

  private void append(String line, double seconds) {
    try {
      out.write(line);
      out.write('\n');
      if (seconds - lastFlushSeconds >= FLUSH_SECONDS) {
        out.flush();
        lastFlushSeconds = seconds;
      }
    } catch (IOException e) {
      RobotLog.ee(TAG, e, "could not write %s; the rest of this run is not logged", file.getPath());
      out = null;
    }
  }

  private double seconds() {
    return (System.nanoTime() - startNanos) / 1e9;
  }

  private static int prune(File directory) {
    File[] logs = directory.listFiles((dir, fileName) -> fileName.endsWith(EXTENSION));
    if (logs == null) return 0;
    Arrays.sort(logs, Comparator.comparingLong(File::lastModified).reversed());
    int pruned = 0;
    for (int i = MAX_LOGS - 1; i < logs.length; i++) {
      if (logs[i].delete()) pruned++;
    }
    return pruned;
  }

  private static String number(double value) {
    if (!Double.isFinite(value)) return "null";
    if (value == Math.rint(value) && Math.abs(value) < 1e9) return Long.toString((long) value);
    return String.format(Locale.ROOT, "%.5f", value);
  }

  private static String quote(String text) {
    StringBuilder json = new StringBuilder("\"");
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      if (c == '"' || c == '\\') json.append('\\').append(c);
      else if (c < 0x20) json.append(String.format(Locale.ROOT, "\\u%04x", (int) c));
      else json.append(c);
    }
    return json.append('"').toString();
  }
}
