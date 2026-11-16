package org.firstinspires.ftc.teamcode.auto.utils.sim;

import com.pedropathing.drivetrain.DrivePowers;
import com.pedropathing.math.Pose;
import com.pedropathing.paths.Path;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Collects simulation samples and events, and writes them as JSON (columnar samples). */
public final class SimResult {
  private final double dt;
  private final Map<String, Object> assumptions;
  private final List<String> warnings = new ArrayList<>();
  private final List<double[]> segments = new ArrayList<>();
  private final List<int[]> groups = new ArrayList<>();
  private final List<Event> events = new ArrayList<>();
  private final List<double[]> samples = new ArrayList<>();
  private double duration;
  private double endPositionError;
  private double endHeadingError;
  private double endSpeed;

  public SimResult(double dt, Map<String, Object> assumptions) {
    this.dt = dt;
    this.assumptions = assumptions;
  }

  public void addSegment(int group, int index, double length) {
    segments.add(new double[] {group, index, length});
  }

  public void addGroup(int group, int firstSegment, int segmentCount) {
    groups.add(new int[] {group, firstSegment, segmentCount});
  }

  public int groupOfSegment(int segment) {
    return (int) segments.get(segment)[0];
  }

  public void warn(String message) {
    warnings.add(message);
  }

  public void event(String type, double time, int group, int segment, double speed) {
    events.add(new Event(type, time, group, segment, speed));
  }

  public void sample(double time, Pose pose, double speed, double omega, double targetVelocity,
                     double closestT, boolean braking, int segment, int group, boolean holding,
                     DrivePowers powers) {
    samples.add(new double[] {
        time, pose.x(), pose.y(), pose.heading(), speed, omega, targetVelocity, closestT,
        braking ? 1 : 0, segment, group, holding ? 1 : 0,
        powers.forward(), powers.strafe(), powers.turn()});
  }

  public SimResult finish(double time, SimPhysics physics, List<Path> paths, Pose start) {
    duration = time;
    Pose end = paths.isEmpty() ? start : paths.get(paths.size() - 1).endPose();
    Pose actual = physics.pose();
    endPositionError = Math.hypot(end.x() - actual.x(), end.y() - actual.y());
    endHeadingError = Math.abs(Math.atan2(
        Math.sin(end.heading() - actual.heading()), Math.cos(end.heading() - actual.heading())));
    endSpeed = physics.worldSpeed();
    return this;
  }

  public double duration() {
    return duration;
  }

  public double endSpeed() {
    return endSpeed;
  }

  public double endPositionError() {
    return endPositionError;
  }

  public double endHeadingError() {
    return endHeadingError;
  }

  public int sampleCount() {
    return samples.size();
  }

  public List<String> warnings() {
    return warnings;
  }

  public double maxSpeed() {
    double max = 0;
    for (double[] sample : samples) max = Math.max(max, sample[4]);
    return max;
  }

  /** Lowest speed within [from, to] seconds, or the minimum over all samples if no sample fits. */
  public double minSpeedBetween(double from, double to) {
    double min = Double.POSITIVE_INFINITY;
    for (double[] sample : samples) {
      if (sample[0] >= from && sample[0] <= to) min = Math.min(min, sample[4]);
    }
    return min;
  }

  public double speedAt(double time) {
    int index = (int) Math.round(time / dt);
    return samples.get(Math.max(0, Math.min(samples.size() - 1, index)))[4];
  }

  public List<double[]> samples() {
    return samples;
  }

  public List<Event> events() {
    return events;
  }

  public String toJson() {
    StringBuilder json = new StringBuilder();
    json.append("{\"ok\":true,\"version\":1,\"dt\":").append(num(dt));
    json.append(",\"duration\":").append(num(duration));
    json.append(",\"summary\":{\"maxSpeed\":").append(num(maxSpeed()))
        .append(",\"endSpeed\":").append(num(endSpeed))
        .append(",\"endPositionError\":").append(num(endPositionError))
        .append(",\"endHeadingError\":").append(num(endHeadingError)).append('}');
    json.append(",\"assumptions\":{");
    boolean first = true;
    for (Map.Entry<String, Object> entry : assumptions.entrySet()) {
      if (!first) json.append(',');
      first = false;
      json.append(quote(entry.getKey())).append(':').append(value(entry.getValue()));
    }
    json.append("},\"warnings\":[");
    for (int i = 0; i < warnings.size(); i++) {
      if (i > 0) json.append(',');
      json.append(quote(warnings.get(i)));
    }
    json.append("],\"groups\":[");
    for (int i = 0; i < groups.size(); i++) {
      int[] group = groups.get(i);
      if (i > 0) json.append(',');
      json.append("{\"group\":").append(group[0]).append(",\"firstSegment\":").append(group[1])
          .append(",\"segmentCount\":").append(group[2]).append('}');
    }
    json.append("],\"segments\":[");
    for (int i = 0; i < segments.size(); i++) {
      double[] segment = segments.get(i);
      if (i > 0) json.append(',');
      json.append("{\"group\":").append((int) segment[0]).append(",\"index\":")
          .append((int) segment[1]).append(",\"length\":").append(num(segment[2])).append('}');
    }
    json.append("],\"events\":[");
    for (int i = 0; i < events.size(); i++) {
      if (i > 0) json.append(',');
      json.append(events.get(i).toJson());
    }
    json.append(']');
    appendColumns(json);
    return json.append('}').toString();
  }

  private static final String[] COLUMNS = {
      "t", "x", "y", "heading", "speed", "omega", "targetVelocity", "closestT",
      "braking", "segment", "group", "holding", "forwardPower", "strafePower", "turnPower"};

  private void appendColumns(StringBuilder json) {
    json.append(",\"samples\":{");
    for (int c = 0; c < COLUMNS.length; c++) {
      if (c > 0) json.append(',');
      json.append(quote(COLUMNS[c])).append(":[");
      for (int i = 0; i < samples.size(); i++) {
        if (i > 0) json.append(',');
        json.append(num(samples.get(i)[c]));
      }
      json.append(']');
    }
    json.append('}');
  }

  public static final class Event {
    public final String type;
    public final double time;
    public final int group;
    public final int segment;
    public final double speed;

    Event(String type, double time, int group, int segment, double speed) {
      this.type = type;
      this.time = time;
      this.group = group;
      this.segment = segment;
      this.speed = speed;
    }

    String toJson() {
      return "{\"type\":" + quote(type) + ",\"t\":" + num(time) + ",\"group\":" + group
          + ",\"segment\":" + segment + ",\"speed\":" + num(speed) + "}";
    }
  }

  public static String failureJson(String error) {
    return "{\"ok\":false,\"error\":" + quote(error) + "}";
  }

  private static String value(Object value) {
    if (value instanceof Number) return num(((Number) value).doubleValue());
    if (value instanceof Boolean) return value.toString();
    return quote(String.valueOf(value));
  }

  private static String num(double value) {
    if (!Double.isFinite(value)) return "null";
    if (value == Math.rint(value) && Math.abs(value) < 1e9) return Long.toString((long) value);
    String text = String.format(Locale.ROOT, "%.5f", value).replaceAll("0+$", "");
    return text.endsWith(".") ? text.substring(0, text.length() - 1) : text;
  }

  private static String quote(String text) {
    StringBuilder out = new StringBuilder("\"");
    for (char c : text.toCharArray()) {
      switch (c) {
        case '"': out.append("\\\""); break;
        case '\\': out.append("\\\\"); break;
        case '\n': out.append("\\n"); break;
        case '\r': out.append("\\r"); break;
        case '\t': out.append("\\t"); break;
        default:
          if (c < 0x20) out.append(String.format("\\u%04x", (int) c));
          else out.append(c);
      }
    }
    return out.append('"').toString();
  }
}
