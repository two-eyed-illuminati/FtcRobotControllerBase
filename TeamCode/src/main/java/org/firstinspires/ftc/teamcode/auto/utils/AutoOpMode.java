package org.firstinspires.ftc.teamcode.auto.utils;

import com.pedropathing.follower.Follower;
import com.pedropathing.ivy.Command;
import com.pedropathing.ivy.Scheduler;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;

import org.firstinspires.ftc.teamcode.utils.Robot;

import java.util.function.Function;

public abstract class AutoOpMode extends OpMode {
    private final Function<Follower, AutoBuilder> routineFactory;

    private Follower follower;
    private Command routine;

    protected AutoOpMode(
            Function<Follower, AutoBuilder> routineFactory
    ) {
        this.routineFactory = routineFactory;
    }

    @Override
    public final void init() {
        Scheduler.reset();
        Robot.initialize(hardwareMap, telemetry);
        follower = Robot.follower;
    }

    @Override
    public final void init_loop() {
        Robot.requestAlliance(gamepad1);
    }

    @Override
    public final void start() {
        routine = routineFactory.apply(follower).build();
        Scheduler.schedule(routine);
    }

    @Override
    public final void loop() {
        Robot.clearBulkCache();
        follower.update();
        Robot.pose = follower.pose();
        Scheduler.execute();
    }

    @Override
    public final void stop() {
        Scheduler.reset();
    }
}
