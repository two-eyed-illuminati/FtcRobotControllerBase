package org.firstinspires.ftc.teamcode.utils.rr.visualizer;

import androidx.annotation.NonNull;

import com.acmerobotics.dashboard.canvas.Canvas;
import com.acmerobotics.dashboard.telemetry.TelemetryPacket;
import com.acmerobotics.roadrunner.Action;
import com.acmerobotics.roadrunner.Actions;
import com.acmerobotics.roadrunner.Pose2d;
import com.acmerobotics.roadrunner.Pose2dDual;
import com.acmerobotics.roadrunner.Time;
import com.acmerobotics.roadrunner.TimeTrajectory;

import org.firstinspires.ftc.teamcode.utils.rr.Drawing;

import java.util.LinkedList;
import java.util.List;

public class SimulateTrajectoryAction implements Action {
    public final TimeTrajectory timeTrajectory;
    private double beginTs = -1;

    private final double[] xPoints, yPoints;
    private final LinkedList<Pose2d> poseHistory = new LinkedList<>();

    public SimulateTrajectoryAction(TimeTrajectory t) {
        timeTrajectory = t;

        List<Double> disps = com.acmerobotics.roadrunner.Math.range(
                0, t.path.length(),
                Math.max(2, (int) Math.ceil(t.path.length() / 2)));
        xPoints = new double[disps.size()];
        yPoints = new double[disps.size()];
        for (int i = 0; i < disps.size(); i++) {
            Pose2d p = t.path.get(disps.get(i), 1).value();
            xPoints[i] = p.position.x;
            yPoints[i] = p.position.y;
        }
    }

    @Override
    public boolean run(@NonNull TelemetryPacket p) {
        double t;
        if (beginTs < 0) {
            beginTs = Actions.now();
            t = 0;
        } else {
            t = Actions.now() - beginTs;
        }

        if (t >= timeTrajectory.duration) {
            return false;
        }

        Pose2dDual<Time> txWorldTarget = timeTrajectory.get(t);
        poseHistory.add(txWorldTarget.value());

        p.put("x", txWorldTarget.value().position.x);
        p.put("y", txWorldTarget.value().position.y);
        p.put("heading (deg)", Math.toDegrees(txWorldTarget.value().heading.toDouble()));

        // only draw when active; only one drive action should be active at a time
        Canvas c = p.fieldOverlay();
        drawPoseHistory(c);

        c.setStroke("#4CAF50");
        Drawing.drawRobot(c, txWorldTarget.value());

        c.setStroke("#4CAF50FF");
        c.setStrokeWidth(1);
        c.strokePolyline(xPoints, yPoints);

        return true;
    }

    @Override
    public void preview(Canvas c) {
        c.setStroke("#4CAF507A");
        c.setStrokeWidth(1);
        c.strokePolyline(xPoints, yPoints);
    }

    private void drawPoseHistory(Canvas c) {
        double[] xPoints = new double[poseHistory.size()];
        double[] yPoints = new double[poseHistory.size()];

        int i = 0;
        for (Pose2d t : poseHistory) {
            xPoints[i] = t.position.x;
            yPoints[i] = t.position.y;

            i++;
        }

        c.setStrokeWidth(1);
        c.setStroke("#3F51B5");
        c.strokePolyline(xPoints, yPoints);
    }
}