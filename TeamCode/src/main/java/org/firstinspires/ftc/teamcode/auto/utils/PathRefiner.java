package org.firstinspires.ftc.teamcode.auto.utils;

import com.pedropathing.math.Pose;
import com.pedropathing.math.Vector2D;
import com.pedropathing.paths.Path;
import com.pedropathing.paths.PathSegment;
import com.pedropathing.paths.curves.Curve;
import com.pedropathing.paths.curves.bezier.BezierCurve;
import com.skeletonarmy.marrow.zones.Point;
import com.skeletonarmy.marrow.zones.PolygonZone;
import com.skeletonarmy.marrow.zones.Zone;

import org.firstinspires.ftc.teamcode.auto.utils.sim.PathSimulator;
import org.firstinspires.ftc.teamcode.auto.utils.sim.SimRobot;
import org.firstinspires.ftc.teamcode.utils.pedro.Constants;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Computer-only port of the motion playground's Weaver refinement (refinement.js).
 * Starts from the faster default Weaver route, tries seed heading plans, then pattern-searches
 * around the best one. Score is simulated arrival time + ROTATION_COST * turned radians,
 * and every kept path stays OBSTACLE_CLEARANCE away from obstacles and the walls.
 */
final class PathRefiner {
  private static final int TRIAL_LIMIT = 200;
  private static final double[] COARSE_STEP_SCALES = {1, 0.5};
  private static final double FINE_STEP_SCALE = 0.25;
  private static final double SHIFT_LIMIT = 6;
  private static final double ROTATION_COST = 0.01;
  private static final double GOAL_RADIUS = 1;
  private static final double HEADING_TOLERANCE = Math.toRadians(2);
  private static final double FIELD = 141.5;
  private static final double ROBOT_SIZE = 18;
  private static final double OBSTACLE_CLEARANCE = 4;
  private static final double PLAN_SPACING = 0.2;

  private static final class Settings {
    int curve;
    boolean followTangent;
    boolean reverse;
    double turn;
    double turnStart;
    double turnEnd;
    double freeze = 1;
    double[] offsets = new double[4];
    double[] shifts;

    Settings copy() {
      Settings copy = new Settings();
      copy.curve = curve;
      copy.followTangent = followTangent;
      copy.reverse = reverse;
      copy.turn = turn;
      copy.turnStart = turnStart;
      copy.turnEnd = turnEnd;
      copy.freeze = freeze;
      copy.offsets = offsets.clone();
      copy.shifts = shifts.clone();
      return copy;
    }

    String key() {
      return String.format(Locale.ROOT, "%d %b %b %.6f %.6f %.6f %.6f %s %s", curve, followTangent, reverse,
        turn, turnStart, turnEnd, freeze, Arrays.toString(offsets), Arrays.toString(shifts));
    }
  }

  private static final class Record {
    final RefinedPath path;
    final double score;
    final double time;
    final boolean physical;
    final boolean keepsBuffer;

    Record(RefinedPath path, double score, double time, boolean physical, boolean keepsBuffer) {
      this.path = path;
      this.score = score;
      this.time = time;
      this.physical = physical;
      this.keepsBuffer = keepsBuffer;
    }
  }

  private final Pose start;
  private final Pose end;
  private final List<Zone> obstacles;
  private final PathSimulator simulator = new PathSimulator(SimRobot.create(Constants.foresightConfig));
  private final Set<String> visited = new HashSet<>();
  private double[][][] curves;
  private Record best;
  private Settings center;
  private double centerScore = Double.POSITIVE_INFINITY;
  private String bestChange = "Default Weaver";
  private int tried;

  private PathRefiner(Pose start, Pose end, List<Zone> obstacles) {
    this.start = start;
    this.end = end;
    this.obstacles = obstacles;
  }

  static RefinedPath refine(Pose start, Pose end, List<Zone> obstacles) {
    if (Math.hypot(end.x() - start.x(), end.y() - start.y()) <= GOAL_RADIUS) {
      System.out.println("  " + start + " -> " + end + ": already at the goal, driving a line");
      return RefinedPath.line();
    }
    return new PathRefiner(start, end, obstacles).run();
  }

  private RefinedPath run() {
    Record forward = measure(RefinedPath.weaver(false));
    Record reverse = measure(RefinedPath.weaver(true));
    boolean reverseFaster = reverse.physical && !Double.isInfinite(reverse.time)
      && (!forward.physical || reverse.time < forward.time);
    Record baseline = reverseFaster ? reverse : forward;
    best = baseline.physical ? baseline : new Record(baseline.path, Double.POSITIVE_INFINITY,
      Double.POSITIVE_INFINITY, false, false);
    double[][] controls = controls(baseline.path.toPath(start, end, obstacles));
    double[][] freeControls = controls(RefinedPath.freeWeaverPath(start, end, obstacles));
    curves = new double[][][] {controls, freeControls};

    search(reverseFaster);

    System.out.printf(Locale.ROOT,
      "  %s -> %s: Weaver forward %.2f s, reverse %.2f s; best %.2f s (%s) after %d trials%n",
      start, end, forward.time, reverse.time, best.time, bestChange, tried);
    if (!best.physical) System.out.println("  WARNING: no trial avoided contact; keeping the default Weaver route");
    return best.path;
  }

  private void search(boolean baselineReverse) {
    Settings tangent = new Settings();
    tangent.followTangent = true;
    tangent.reverse = baselineReverse;
    tangent.shifts = flat(0);
    Settings fixed = tangent.copy();
    fixed.followTangent = false;
    fixed.reverse = false;
    double seedTurn = wrapAngle(end.heading() - start.heading());
    if (Math.abs(seedTurn) <= 0.05) seedTurn = 0;

    test(tangent, "Default heading");
    Settings hold = fixed.copy();
    hold.turnEnd = 1;
    test(hold, "Hold, then turn at goal");
    for (boolean reverse : new boolean[] {tangent.reverse, !tangent.reverse}) {
      for (double turnEnd : new double[] {0.15, 0.35}) {
        Settings trial = tangent.copy();
        trial.reverse = reverse;
        trial.turnEnd = turnEnd;
        test(trial, "Turn to tangent while driving");
      }
    }
    for (double turnEnd : new double[] {0, 0.15, 0.5}) {
      Settings trial = fixed.copy();
      trial.turn = seedTurn;
      trial.turnEnd = turnEnd;
      test(trial, turnEnd > 0 ? "Turn while driving" : "Turn, then drive");
    }
    Settings late = fixed.copy();
    late.turn = seedTurn;
    late.turnStart = 0.5;
    late.turnEnd = 1;
    test(late, "Drive, then turn");

    Settings free = fixed.copy();
    free.curve = 1;
    free.shifts = flat(1);
    for (double[] window : new double[][] {{0, 0}, {0, 0.15}, {0, 0.5}, {0, 1}, {0.5, 1}}) {
      Settings trial = free.copy();
      trial.turn = seedTurn;
      trial.turnStart = window[0];
      trial.turnEnd = window[1];
      test(trial, String.format(Locale.ROOT, "Free-heading curve, turn %.0f-%.0f%%", 100 * window[0], 100 * window[1]));
    }
    for (boolean reverse : new boolean[] {false, true}) {
      Settings trial = free.copy();
      trial.followTangent = true;
      trial.reverse = reverse;
      trial.freeze = 0.7;
      test(trial, "Free-heading curve, tangent then turn");
    }
    if (center == null) return;

    for (double scale : COARSE_STEP_SCALES) stepAround(scale);
    double before;
    do {
      before = centerScore;
      stepAround(FINE_STEP_SCALE);
    } while (centerScore < before - 1e-8 && tried < TRIAL_LIMIT);
  }

  private void stepAround(double scale) {
    for (int sign : new int[] {-1, 1}) {
      Settings freeze = center.copy();
      freeze.freeze = clamp(center.freeze + sign * 0.15 * scale, 0.3, 1);
      test(freeze, "Freeze heading earlier/later");
      Settings turnStart = center.copy();
      turnStart.turnStart = clamp(center.turnStart + sign * 0.1 * scale, 0, center.turnEnd);
      test(turnStart, "Adjust turn start");
      Settings turnEnd = center.copy();
      turnEnd.turnEnd = clamp(center.turnEnd + sign * 0.15 * scale, center.turnStart, 1);
      test(turnEnd, "Adjust turn finish");
    }
    for (int i = 1; center.followTangent && i < center.offsets.length; i++) {
      for (int sign : new int[] {-1, 1}) {
        Settings trial = center.copy();
        trial.offsets[i] = clamp(trial.offsets[i] + sign * 0.35 * scale, -Math.PI, Math.PI);
        test(trial, "Heading point " + i);
      }
    }
    for (int i = 0; i < center.shifts.length && tried < TRIAL_LIMIT; i++) {
      for (int sign : new int[] {-1, 1}) {
        Settings trial = center.copy();
        trial.shifts[i] = clamp(trial.shifts[i] + sign * 3 * scale, -SHIFT_LIMIT, SHIFT_LIMIT);
        test(trial, curves[center.curve].length == 1 ? "Bend curve" : "Shift join " + (i + 1));
      }
    }
  }

  private void test(Settings settings, String change) {
    if (tried >= TRIAL_LIMIT || !visited.add(settings.key())) return;
    tried++;
    Record record = measure(new RefinedPath(RefinedPath.Kind.REFINED, settings.reverse, settings.followTangent,
      settings.turn, settings.turnStart, settings.turnEnd, settings.freeze, settings.offsets.clone(),
      shiftedGeometry(curves[settings.curve], settings.shifts)));
    boolean valid = record.keepsBuffer && !Double.isInfinite(record.time);
    if (valid && record.score < centerScore - 1e-8) {
      center = settings;
      centerScore = record.score;
    }
    if (valid && record.score < best.score - 1e-8) {
      best = record;
      bestChange = change;
    }
  }

  private Record measure(RefinedPath refined) {
    Path path = refined.toPath(start, end, obstacles);
    List<double[]> trace = simulator.simulate(start, Collections.singletonList(path)).samples();

    int lastOutside = -1;
    for (int i = 0; i < trace.size(); i++) {
      if (!arrived(trace.get(i))) lastOutside = i;
    }
    double time = lastOutside < trace.size() - 1 ? trace.get(lastOutside + 1)[0] : Double.POSITIVE_INFINITY;

    boolean physical = !trace.isEmpty();
    for (int i = 0; physical && i < trace.size(); i++) {
      physical = clear(trace.get(i)[1], trace.get(i)[2], trace.get(i)[3], 0);
    }
    boolean keepsBuffer = physical;
    for (int i = 0; keepsBuffer && i < trace.size(); i++) {
      keepsBuffer = clear(trace.get(i)[1], trace.get(i)[2], trace.get(i)[3], OBSTACLE_CLEARANCE);
    }
    for (int s = 0; keepsBuffer && s < path.getSegments().size(); s++) {
      PathSegment segment = path.getSegments().get(s);
      int steps = Math.max(32, (int) Math.ceil(segment.curve.length() / PLAN_SPACING));
      for (int i = 0; keepsBuffer && i <= steps; i++) {
        Pose point = segment.get((double) i / steps);
        keepsBuffer = clear(point.x(), point.y(), point.heading(), OBSTACLE_CLEARANCE);
      }
    }

    double rotation = 0;
    for (int i = 1; i < trace.size() && trace.get(i - 1)[0] < time; i++) {
      rotation += Math.abs(wrapAngle(trace.get(i)[3] - trace.get(i - 1)[3]));
    }
    return new Record(refined, time + ROTATION_COST * rotation, time, physical, keepsBuffer);
  }

  private boolean arrived(double[] sample) {
    return Math.hypot(sample[1] - end.x(), sample[2] - end.y()) <= GOAL_RADIUS
      && Math.abs(wrapAngle(sample[3] - end.heading())) <= HEADING_TOLERANCE;
  }

  /** True when the robot footprint is inside the field and more than {@code clearance} from every obstacle. */
  private boolean clear(double x, double y, double heading, double clearance) {
    double extent = ROBOT_SIZE / 2 * (Math.abs(Math.cos(heading)) + Math.abs(Math.sin(heading)));
    if (x < extent || x > FIELD - extent || y < extent || y > FIELD - extent) return false;
    if (obstacles.isEmpty()) return true;
    PolygonZone robot = new PolygonZone(new Point(x, y), ROBOT_SIZE, ROBOT_SIZE, heading);
    for (Zone obstacle : obstacles) {
      double distance = robot.distanceTo(obstacle);
      if (clearance == 0 ? distance == 0 : distance < clearance) return false;
    }
    return true;
  }

  private double[] flat(int curve) {
    return new double[Math.max(1, curves[curve].length - 1)];
  }

  /** Moves the joins between legs (or bends a single leg) sideways by {@code shifts} inches. */
  private static double[][] shiftedGeometry(double[][] controls, double[] shifts) {
    double[][] legs = new double[controls.length][];
    for (int i = 0; i < controls.length; i++) legs[i] = controls[i].clone();
    boolean shifted = false;
    for (double shift : shifts) shifted |= shift != 0;
    if (!shifted) return legs;

    if (legs.length == 1) {
      double[] leg = legs[0];
      double x = leg[0], y = leg[1], dx = leg[leg.length - 2] - x, dy = leg[leg.length - 1] - y;
      double length = Math.hypot(dx, dy);
      if (leg.length == 4) {
        leg = new double[] {x, y, x + dx / 3, y + dy / 3, x + 2 * dx / 3, y + 2 * dy / 3, x + dx, y + dy};
        legs[0] = leg;
      }
      for (int p : new int[] {2, 4}) {
        leg[p] += -dy * shifts[0] / length;
        leg[p + 1] += dx * shifts[0] / length;
      }
      return legs;
    }
    for (int i = 0; i < shifts.length; i++) {
      if (shifts[i] == 0) continue;
      double[] prior = legs[i], next = legs[i + 1], edge = controls[i];
      int join = edge.length - 2;
      double angle = Math.atan2(edge[join + 1] - edge[join - 1], edge[join] - edge[join - 2]);
      double moveX = -Math.sin(angle) * shifts[i], moveY = Math.cos(angle) * shifts[i];
      prior[join] += moveX;
      prior[join + 1] += moveY;
      next[0] += moveX;
      next[1] += moveY;
      if (prior.length > 4) {
        prior[join - 2] += moveX;
        prior[join - 1] += moveY;
      }
      if (next.length > 4) {
        next[2] += moveX;
        next[3] += moveY;
      }
    }
    return legs;
  }

  private static double[][] controls(Path path) {
    List<PathSegment> segments = path.getSegments();
    double[][] legs = new double[segments.size()][];
    for (int i = 0; i < legs.length; i++) {
      Curve curve = segments.get(i).curve;
      List<Vector2D> points = curve instanceof BezierCurve
        ? ((BezierCurve) curve).getControlPoints() : Arrays.asList(curve.startPoint(), curve.endPoint());
      legs[i] = new double[2 * points.size()];
      for (int j = 0; j < points.size(); j++) {
        legs[i][2 * j] = points.get(j).x();
        legs[i][2 * j + 1] = points.get(j).y();
      }
    }
    return legs;
  }

  private static double wrapAngle(double angle) {
    return RefinedPath.wrapAngle(angle);
  }

  private static double clamp(double value, double min, double max) {
    return Math.max(min, Math.min(max, value));
  }
}
