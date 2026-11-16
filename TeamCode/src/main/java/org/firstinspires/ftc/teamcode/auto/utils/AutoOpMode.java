package org.firstinspires.ftc.teamcode.auto.utils;

import com.pedropathing.follower.Follower;
import com.pedropathing.ivy.Command;
import com.pedropathing.ivy.Scheduler;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.util.RobotLog;

import org.firstinspires.ftc.teamcode.auto.utils.trace.FollowerTrace;
import org.firstinspires.ftc.teamcode.utils.Robot;

import java.util.function.Function;

public abstract class AutoOpMode extends OpMode {
    private final Function<Follower, AutoBuilder> routineFactory;

    private Command routine;
    private FollowerTrace trace;

    protected AutoOpMode(
            Function<Follower, AutoBuilder> routineFactory
    ) {
        this.routineFactory = routineFactory;
    }

    @Override
    public final void init() {
        Robot.init(hardwareMap, telemetry);
    }

    @Override
    public final void init_loop() {
        Robot.initLoop(gamepad1);
    }

    @Override
    public final void start() {
        routine = routineFactory.apply(Robot.follower).build();
        Scheduler.schedule(routine);
        if (Robot.follower == null) {
            RobotLog.ii("FollowerTrace", "no follower, trace disabled");
        } else {
            trace = new FollowerTrace(getClass().getSimpleName());
        }
    }

    @Override
    public final void loop() {
        Robot.loop();
        if (trace != null) trace.record(Robot.follower);
        Scheduler.execute();
    }

    @Override
    public final void stop() {
        Scheduler.reset();
        if (trace != null) {
            trace.save();
            trace = null;
        }
    }
}
