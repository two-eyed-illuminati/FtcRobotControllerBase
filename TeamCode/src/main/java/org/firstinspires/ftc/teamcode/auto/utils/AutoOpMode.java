package org.firstinspires.ftc.teamcode.auto.utils;

import com.pedropathing.follower.Follower;
import com.pedropathing.ivy.Command;
import com.pedropathing.ivy.Scheduler;
import com.pedropathing.math.Pose;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;

import org.firstinspires.ftc.teamcode.utils.Robot;

import java.util.function.Function;

public abstract class AutoOpMode extends OpMode {
    private final Robot.Alliance alliance;
    private final Function<Follower, AutoBuilder> routineFactory;

    private Follower follower;
    private Command routine;

    protected AutoOpMode(
            Robot.Alliance alliance,
            Function<Follower, AutoBuilder> routineFactory
    ) {
        this.alliance = alliance;
        this.routineFactory = routineFactory;
    }

    @Override
    public final void init() {
        Scheduler.reset();
        Robot.alliance = alliance;
        Robot.initialize(hardwareMap, telemetry);
        follower = Robot.follower;
        routine = routineFactory.apply(follower).build();
    }

    @Override
    public final void start() {
        Scheduler.schedule(routine);
    }

    @Override
    public final void loop() {
        follower.update();
        Scheduler.execute();
    }

    @Override
    public final void stop() {
        Scheduler.reset();
    }

    protected static Pose pose(double x, double y, double headingDegrees) {
        return new Pose(x, y, Math.toRadians(headingDegrees));
    }
}
