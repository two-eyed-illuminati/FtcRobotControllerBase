package org.firstinspires.ftc.teamcode.auto;

import static com.pedropathing.api.Paths.curve;
import static com.pedropathing.api.Paths.line;

import com.pedropathing.follower.Follower;
import com.pedropathing.math.Pose;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;

import org.firstinspires.ftc.teamcode.auto.utils.AutoBuilder;
import org.firstinspires.ftc.teamcode.auto.utils.AutoOpMode;
import org.firstinspires.ftc.teamcode.utils.Robot;

@Autonomous(name = "Example Auto", group = "Auto")
public final class ExampleAuto extends AutoOpMode {
    public ExampleAuto() {
        super(ExampleAuto::createRoutine);
    }

    public static AutoBuilder createRoutine(Follower follower) {
        Pose start = Robot.START_POSE_CORNER;

        // Example of a "Tweak" - meant to be used for last-minute emergency tweaks to auto
        AutoBuilder.GoToCenter gtcConfig = new AutoBuilder.GoToCenter();
        gtcConfig.endPose = new Pose(72, 72, Math.PI/2);

        return new AutoBuilder(follower)
                .startAt(start)
                .goToCenter(gtcConfig);
    }
}
