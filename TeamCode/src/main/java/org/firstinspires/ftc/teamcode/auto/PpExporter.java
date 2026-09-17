package org.firstinspires.ftc.teamcode.auto;

import com.pedropathing.math.Pose;
import com.pedropathing.math.Vector2D;
import com.pedropathing.paths.Path;
import com.pedropathing.paths.PathSegment;
import com.pedropathing.paths.curves.Curve;
import com.pedropathing.paths.curves.Line;
import com.pedropathing.paths.curves.bezier.BezierCurve;

import java.util.ArrayList;
import java.util.List;

/**
 * Converts the geometry of Pedro paths into a Pedro Visualizer project.
 *
 * <p>Lines and Bézier curves are converted exactly. Other curve implementations are rejected until
 * an exact conversion is added.
 */
public final class PpExporter {
    private static final int HEADING_SAMPLES = 8;
    private static final double ANGLE_TOLERANCE = Math.toRadians(0.01);

    private final List<String> paths = new ArrayList<>();
    private Pose startPose;

    public void startAt(Pose pose) {
        startPose = pose;
    }

    public void add(Path path) {
        for (PathSegment segment : path.getSegments()) {
            paths.add(pathJson(segment, paths.size() + 1));
        }
    }

    public String build() {
        if (startPose == null) {
            throw new IllegalStateException("A start pose is required");
        }

        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"version\": \"1.5.0\",\n");
        json.append("  \"startPoint\": ").append(startPointJson(startPose)).append(",\n");
        json.append("  \"lines\": [");
        appendItems(json, paths, "    ");
        json.append("\n  ],\n");
        json.append("  \"shapes\": [],\n");
        json.append("  \"sequence\": [");
        for (int i = 0; i < paths.size(); i++) {
            if (i > 0) json.append(',');
            json.append("\n    {\"kind\":\"path\",\"lineId\":\"path-")
                    .append(i + 1)
                    .append("\"}");
        }
        json.append("\n  ],\n");
        json.append("  \"activePaths\": [");
        for (int i = 0; i < paths.size(); i++) {
            if (i > 0) json.append(',');
            json.append("\"path-").append(i + 1).append("\"");
        }
        json.append("]\n");
        json.append('}');
        return json.toString();
    }

    private static String pathJson(PathSegment segment, int index) {
        Curve curve = segment.curve;
        Vector2D end;
        List<Vector2D> controls = new ArrayList<>();

        if (curve instanceof Line) {
            end = curve.endPoint();
        } else if (curve instanceof BezierCurve) {
            List<Vector2D> points = ((BezierCurve) curve).getControlPoints();
            end = points.get(points.size() - 1);
            controls.addAll(points.subList(1, points.size() - 1));
        } else {
            throw new IllegalArgumentException(
                    "Unsupported Pedro curve type: " + curve.getClass().getName());
        }

        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"id\":\"path-").append(index).append("\",");
        json.append("\"color\":\"#4f8cff\",");
        json.append("\"kind\":\"atomic\",");
        json.append("\"endPoint\":").append(pointJson(end)).append(',');
        json.append("\"controlPoints\":[");
        for (int i = 0; i < controls.size(); i++) {
            if (i > 0) json.append(',');
            json.append(pointJson(controls.get(i)));
        }
        json.append("],");
        json.append("\"heading\":").append(headingJson(segment));
        json.append("}");
        return json.toString();
    }

    private static String headingJson(PathSegment segment) {
        double[] headings = sampledHeadings(segment);
        double start = headings[0];
        double end = headings[HEADING_SAMPLES];

        if (matchesConstant(headings, start)) {
            return "{\"type\":\"constant\",\"degrees\":" + degrees(start) + "}";
        }

        if (matchesLinear(headings, start, end)) {
            return "{\"type\":\"linear\",\"startDeg\":" + degrees(start)
                    + ",\"endDeg\":" + degrees(end) + "}";
        }

        if (matchesTangent(segment, headings, false)) {
            return "{\"type\":\"tangential\",\"reverse\":false}";
        }

        if (matchesTangent(segment, headings, true)) {
            return "{\"type\":\"tangential\",\"reverse\":true}";
        }

        StringBuilder json = new StringBuilder();
        json.append("{\"type\":\"piecewise\",\"piecewiseHeading\":{\"segments\":[");
        for (int i = 0; i < HEADING_SAMPLES; i++) {
            if (i > 0) json.append(',');
            json.append("{\"startProgress\":").append(number((double) i / HEADING_SAMPLES));
            json.append(",\"endProgress\":").append(number((double) (i + 1) / HEADING_SAMPLES));
            json.append(",\"interpolationType\":\"linear\",\"parameters\":{");
            json.append("\"startDeg\":").append(degrees(headings[i]));
            json.append(",\"endDeg\":").append(degrees(headings[i + 1]));
            json.append("}}");
        }
        json.append("]}}");
        return json.toString();
    }

    private static double[] sampledHeadings(PathSegment segment) {
        double[] headings = new double[HEADING_SAMPLES + 1];
        headings[0] = segment.heading(0);

        for (int i = 1; i <= HEADING_SAMPLES; i++) {
            double heading = segment.heading((double) i / HEADING_SAMPLES);
            headings[i] = headings[i - 1] + angleDifference(headings[i - 1], heading);
        }
        return headings;
    }

    private static boolean matchesConstant(double[] headings, double expected) {
        for (double heading : headings) {
            if (Math.abs(angleDifference(expected, heading)) > ANGLE_TOLERANCE) return false;
        }
        return true;
    }

    private static boolean matchesLinear(double[] headings, double start, double end) {
        for (int i = 0; i < headings.length; i++) {
            double expected = start + (end - start) * i / HEADING_SAMPLES;
            if (Math.abs(angleDifference(expected, headings[i])) > ANGLE_TOLERANCE) return false;
        }
        return true;
    }

    private static boolean matchesTangent(
            PathSegment segment, double[] headings, boolean reversed) {
        double offset = reversed ? Math.PI : 0;
        for (int i = 0; i < headings.length; i++) {
            double t = (double) i / HEADING_SAMPLES;
            double expected = segment.curve.tangent(t).theta() + offset;
            if (Math.abs(angleDifference(expected, headings[i])) > ANGLE_TOLERANCE) return false;
        }
        return true;
    }

    private static double angleDifference(double from, double to) {
        return Math.atan2(Math.sin(to - from), Math.cos(to - from));
    }

    private static String startPointJson(Pose pose) {
        return "{\"x\":" + number(pose.x())
                + ",\"y\":" + number(pose.y())
                + ",\"headingDeg\":" + degrees(pose.heading()) + "}";
    }

    private static String pointJson(Vector2D point) {
        return "{\"x\":" + number(point.x()) + ",\"y\":" + number(point.y()) + "}";
    }

    private static String degrees(double radians) {
        return number(Math.toDegrees(radians));
    }

    private static String number(double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("Cannot export a non-finite number");
        }
        return Double.toString(value);
    }

    private static void appendItems(StringBuilder destination, List<String> items, String indent) {
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) destination.append(',');
            destination.append('\n').append(indent).append(items.get(i));
        }
    }
}
