package org.firstinspires.ftc.teamcode.teleop;

import com.acmerobotics.roadrunner.Action;
import com.acmerobotics.roadrunner.Pose2d;

import org.firstinspires.ftc.teamcode.utils.Robot;

public class test {
    public static Action testing(){
        return Robot.drive.actionBuilder(new Pose2d(-72+8, 24+17.5/2, 0)).
                splineToLinearHeading(new Pose2d(-48, 48, Math.toRadians(135)), Math.toRadians(90)).build();
    }
}
