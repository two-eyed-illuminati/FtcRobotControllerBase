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
        Pose start = pose(56, 8, 90);
        Pose approach = pose(56, 16, 0);

        return new AutoBuilder(follower)
                .startAt(start)
                .followPath(line(start, approach).linear(start, approach));
    }
}
