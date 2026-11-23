package org.firstinspires.ftc.teamcode.auto.utils;

import com.pedropathing.api.Paths;
import com.pedropathing.math.Pose;
import com.pedropathing.math.Vector2D;
import com.pedropathing.paths.AtomicPath;
import com.pedropathing.paths.CompoundPath;
import com.pedropathing.paths.Path;
import com.pedropathing.paths.curves.Curve;
import com.pedropathing.paths.curves.Line;
import com.pedropathing.paths.curves.bezier.BezierCurve;
import com.skeletonarmy.marrow.weaver.HeadingPolicy;
import com.skeletonarmy.marrow.weaver.PedroPathingConverter;
import com.skeletonarmy.marrow.weaver.Weaver;
import com.skeletonarmy.marrow.zones.Zone;

import java.util.List;
import java.util.Locale;

/**
 * A path found by PathRefiner, stored as text so the hub rebuilds the same Pedro path.
 * LINE and WEAVER paths are rebuilt from the poses; REFINED paths carry their own geometry and heading plan.
 */
public final class RefinedPath {
  public enum Kind { LINE, WEAVER, REFINED }

  private static final int TIME_SAMPLES = 200;
  private static final double PROFILE_VELOCITY = 60;
  private static final double PROFILE_ACCELERATION = 50;
  private static final double PROFILE_DECELERATION = 40;

  final Kind kind;
  final boolean reverse;
  final boolean followTangent;
  final double turn;
  final double turnStart;
  final double turnEnd;
  final double freeze;
  final double[] offsets;
  /** Bezier control points per leg, as x0, y0, x1, y1, ...; two points make a line. */
  final double[][] legs;

  RefinedPath(Kind kind, boolean reverse, boolean followTangent, double turn, double turnStart, double turnEnd,
              double freeze, double[] offsets, double[][] legs) {
    this.kind = kind;
    this.reverse = reverse;
    this.followTangent = followTangent;
    this.turn = turn;
    this.turnStart = turnStart;
    this.turnEnd = turnEnd;
    this.freeze = freeze;
    this.offsets = offsets;
    this.legs = legs;
  }

  static RefinedPath line() {
    return new RefinedPath(Kind.LINE, false, false, 0, 0, 0, 1, new double[4], new double[0][]);
  }

  static RefinedPath weaver(boolean reverse) {
    return new RefinedPath(Kind.WEAVER, reverse, false, 0, 0, 0, 1, new double[4], new double[0][]);
  }

  public Path toPath(Pose start, Pose end, List<Zone> obstacles) {
    switch (kind) {
      case LINE:
        return Paths.line(start, end).linear(start, end);
      case WEAVER:
        return reverse
          ? weaverPath(start, end.withHeading(end.heading() + Math.PI), obstacles).reverseTangent()
          : weaverPath(start, end, obstacles);
      default:
        return refinedPath(start, end);
    }
  }

  /** Default Weaver route that arrives at end's heading. */
  static Path weaverPath(Pose start, Pose end, List<Zone> obstacles) {
    return PedroPathingConverter.toPath(Weaver.builder()
      .start(PedroPathingConverter.toPoint(start))
      .end(PedroPathingConverter.toPoint(end), HeadingPolicy.LOCKED)
      .obstacles(obstacles)
      .build());
  }

  /** Default Weaver route that may arrive at any heading. */
  static Path freeWeaverPath(Pose start, Pose end, List<Zone> obstacles) {
    return PedroPathingConverter.toPath(Weaver.builder()
      .start(PedroPathingConverter.toPoint(start))
      .end(PedroPathingConverter.toPoint(end), HeadingPolicy.FREE)
      .obstacles(obstacles)
      .build());
  }

  /**
   * Heading blends from the start heading toward a target (a fixed turn, or the path tangent plus offsets)
   * on a time share between turnStart and turnEnd, then holds from progress {@code freeze}
   * and finishes the turn to the end heading.
   */
  private Path refinedPath(Pose start, Pose end) {
    Curve[] curves = new Curve[legs.length];
    for (int i = 0; i < legs.length; i++) curves[i] = curve(legs[i]);
    double totalLength = 0;
    for (Curve curve : curves) totalLength += curve.length();
    double[] timeShare = timeShareByProgress(totalLength);
    double[] tangent = tangentByProgress(curves, totalLength);
    double heading = start.heading();
    double endHeading = end.heading();
    double startTurn = wrapAngle(tangent[0] + (reverse ? Math.PI : 0) - heading);

    Path[] paths = new Path[curves.length];
    double traversed = 0;
    for (int i = 0; i < curves.length; i++) {
      Curve curve = curves[i];
      double before = traversed;
      double length = totalLength;
      paths[i] = new AtomicPath(curve).heading((ignored, t) -> {
        double progress = (before + curve.length() * curve.pathCompletion(t)) / length;
        double frozen = Math.min(freeze, progress);
        double weight = smoothStep((lookup(timeShare, frozen) - turnStart) / Math.max(1e-6, turnEnd - turnStart));
        double knot = frozen * 3;
        int left = Math.min(2, (int) knot);
        double offset = offsets[left] + smoothStep(knot - left) * (offsets[left + 1] - offsets[left]);
        double target = followTangent ? startTurn + lookup(tangent, frozen) - tangent[0] + offset : turn;
        double profile = heading + weight * target;
        if (progress < freeze) return profile;
        double finish = freeze >= 1 ? 1 : smoothStep((progress - freeze) / (1 - freeze));
        return profile + finish * wrapAngle(endHeading - profile);
      });
      traversed += curve.length();
    }
    return paths.length == 1 ? paths[0] : new CompoundPath(paths);
  }

  /** Format: "line", "weaver forward|reverse", or "refined tangent reverse turn turnStart turnEnd freeze o0 o1 o2 o3 legs". */
  public String encode() {
    switch (kind) {
      case LINE:
        return "line";
      case WEAVER:
        return "weaver " + (reverse ? "reverse" : "forward");
      default:
        StringBuilder text = new StringBuilder(String.format(Locale.ROOT, "refined %b %b %.4f %.4f %.4f %.4f",
          followTangent, reverse, turn, turnStart, turnEnd, freeze));
        for (double offset : offsets) text.append(String.format(Locale.ROOT, " %.4f", offset));
        text.append(' ');
        for (int i = 0; i < legs.length; i++) {
          if (i > 0) text.append(';');
          for (int j = 0; j < legs[i].length; j++) {
            if (j > 0) text.append(',');
            text.append(String.format(Locale.ROOT, "%.3f", legs[i][j]));
          }
        }
        return text.toString();
    }
  }

  public static RefinedPath decode(String text) {
    String[] parts = text.trim().split(" ");
    switch (parts[0]) {
      case "line":
        return line();
      case "weaver":
        return weaver(parts[1].equals("reverse"));
      case "refined":
        double[] offsets = new double[4];
        for (int i = 0; i < 4; i++) offsets[i] = Double.parseDouble(parts[7 + i]);
        String[] encodedLegs = parts[11].split(";");
        double[][] legs = new double[encodedLegs.length][];
        for (int i = 0; i < legs.length; i++) {
          String[] values = encodedLegs[i].split(",");
          legs[i] = new double[values.length];
          for (int j = 0; j < values.length; j++) legs[i][j] = Double.parseDouble(values[j]);
        }
        return new RefinedPath(Kind.REFINED, Boolean.parseBoolean(parts[2]), Boolean.parseBoolean(parts[1]),
          Double.parseDouble(parts[3]), Double.parseDouble(parts[4]), Double.parseDouble(parts[5]),
          Double.parseDouble(parts[6]), offsets, legs);
      default:
        throw new IllegalArgumentException("Unknown refined path: " + text);
    }
  }

  private static Curve curve(double[] leg) {
    Vector2D[] points = new Vector2D[leg.length / 2];
    for (int i = 0; i < points.length; i++) points[i] = Vector2D.cartesian(leg[2 * i], leg[2 * i + 1]);
    return points.length == 2 ? new Line(points[0], points[1]) : new BezierCurve(points);
  }

  /** Share of a rest-to-rest trapezoid drive's time spent before each 1/TIME_SAMPLES of the path. */
  private static double[] timeShareByProgress(double length) {
    double[] share = new double[TIME_SAMPLES + 1];
    double step = length / TIME_SAMPLES;
    for (int i = 1; i <= TIME_SAMPLES; i++) {
      double s = (i - 0.5) * step;
      double speed = Math.min(PROFILE_VELOCITY,
        Math.min(Math.sqrt(2 * PROFILE_ACCELERATION * s), Math.sqrt(2 * PROFILE_DECELERATION * (length - s))));
      share[i] = share[i - 1] + step / Math.max(1e-6, speed);
    }
    for (int i = 1; i <= TIME_SAMPLES; i++) share[i] /= share[TIME_SAMPLES];
    return share;
  }

  /** Unwrapped tangent angle at each 1/TIME_SAMPLES of the path. */
  private static double[] tangentByProgress(Curve[] curves, double length) {
    double[] angle = new double[TIME_SAMPLES + 1];
    int curve = 0;
    double before = 0;
    for (int i = 0; i <= TIME_SAMPLES; i++) {
      double distance = length * i / TIME_SAMPLES;
      while (curve < curves.length - 1 && distance > before + curves[curve].length()) {
        before += curves[curve].length();
        curve++;
      }
      double completion = Math.max(0, Math.min(1, (distance - before) / curves[curve].length()));
      double raw = curves[curve].tangent(curves[curve].parameter(completion)).theta();
      angle[i] = i == 0 ? raw : angle[i - 1] + wrapAngle(raw - angle[i - 1]);
    }
    return angle;
  }

  private static double lookup(double[] table, double progress) {
    double index = Math.max(0, Math.min(1, progress)) * (table.length - 1);
    int left = Math.min(table.length - 2, (int) index);
    return table[left] + (index - left) * (table[left + 1] - table[left]);
  }

  static double wrapAngle(double angle) {
    return Math.IEEEremainder(angle, 2 * Math.PI);
  }

  private static double smoothStep(double t) {
    double value = Math.max(0, Math.min(1, t));
    return value * value * (3 - 2 * value);
  }
}
