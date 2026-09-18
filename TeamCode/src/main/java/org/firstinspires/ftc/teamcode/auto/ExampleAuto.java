package org.firstinspires.ftc.teamcode.auto;

import com.pedropathing.follower.Follower;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;

import org.firstinspires.ftc.teamcode.utils.Robot;

@Autonomous(name = "Example Auto", group = "Auto")
public final class ExampleAuto extends AutoOpMode {
    public ExampleAuto() {
        super(Robot.Alliance.BLUE, ExampleAuto::createRoutine);
    }

    public static ExampleAutoBuilder createRoutine(Follower follower) {
        return new ExampleAutoBuilder(follower)
                .startAt(pose(24, 32, 0))
                .goToMark(ExampleAutoBuilder.Mark.LEFT)
                .parkAt(pose(84, 48, 90));
    }
}
