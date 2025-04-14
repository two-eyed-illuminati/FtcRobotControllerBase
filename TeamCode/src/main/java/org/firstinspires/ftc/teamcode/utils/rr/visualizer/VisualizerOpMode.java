package org.firstinspires.ftc.teamcode.utils.rr.visualizer;

import com.acmerobotics.dashboard.canvas.Canvas;
import com.acmerobotics.dashboard.telemetry.TelemetryPacket;
import com.acmerobotics.roadrunner.Action;
import com.acmerobotics.roadrunner.NullAction;
import com.acmerobotics.roadrunner.ParallelAction;
import com.acmerobotics.roadrunner.RaceAction;
import com.acmerobotics.roadrunner.SequentialAction;
import com.acmerobotics.roadrunner.SleepAction;

import org.firstinspires.ftc.teamcode.teleop.test;
import org.firstinspires.ftc.teamcode.utils.rr.MecanumDrive.FollowTrajectoryAction;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class VisualizerOpMode extends TestOpMode {
    DashboardInstance dashboard;
    private boolean b;
    private Canvas c;
    private Action action;

    public VisualizerOpMode() {
        super("RoadRunnerPathVisualizer");
    }

    @Override
    protected void init() {
        dashboard = DashboardInstance.getInstance();
        b = true;
        Canvas c = new Canvas();
        action = cleanActions(test.testing());
        action.preview(c);
    }

    // Removes all actions which aren't
    // SequentialAction, ParallelAction, RaceAction, SleepAction, NullAction, or FollowTrajectoryAction
    public Action cleanActions(Action action){
        Class actionClass = action.getClass();
        if(actionClass.equals(SequentialAction.class) ||
                actionClass.equals(ParallelAction.class) ||
                actionClass.equals(RaceAction.class)
        ) {
            try {
                Field f = actionClass.getDeclaredField("actions");
                f.setAccessible(true);
                List<Action> actions = (List<Action>) f.get(action);
                return (Action) actionClass.getConstructor(List.class)
                        .newInstance(actions.stream().map(this::cleanActions).collect(Collectors.toList()));
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
        if(actionClass.equals(SleepAction.class) ||
                actionClass.equals(NullAction.class) ||
                actionClass.equals(FollowTrajectoryAction.class)
        ) {
            return action;
        }
        return new NullAction();
    }

    @Override
    protected void loop() throws InterruptedException {
        action = cleanActions(action);

        if(b) {
            TelemetryPacket p = new TelemetryPacket();
            p.fieldOverlay().getOperations().addAll(c.getOperations());
            b = action.run(p);
            dashboard.sendTelemetryPacket(p);
        }
    }
}
