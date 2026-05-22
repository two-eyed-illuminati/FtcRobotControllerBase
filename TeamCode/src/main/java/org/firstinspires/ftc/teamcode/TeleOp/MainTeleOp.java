package org.firstinspires.ftc.teamcode.TeleOp;

import com.acmerobotics.roadrunner.Pose2d;
import com.acmerobotics.roadrunner.PoseVelocity2d;
import com.acmerobotics.roadrunner.Rotation2d;
import com.acmerobotics.roadrunner.Vector2d;
import com.qualcomm.hardware.lynx.LynxModule;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.teamcode.utils.Clamp;
import org.firstinspires.ftc.teamcode.utils.Robot;

import java.util.List;

@TeleOp(name="Main TeleOp", group="Main")
public class MainTeleOp extends OpMode {
    public static boolean FIELD_CENTRIC = true;
    public double currentMinOuttakeVel = 0.0;
    public double currentMaxOuttakeVel = 0.0;
    public ElapsedTime timeSinceWantedLedStateChange;
    public int wantedLedState = 0;

    @Override
    public void init(){
        Robot.initialize(hardwareMap, telemetry);
        timeSinceWantedLedStateChange = new ElapsedTime();
    }

    @Override
    public void loop(){
        PoseVelocity2d currDriveVel = Robot.drive.updatePoseEstimate();
        currDriveVel = Rotation2d.fromDouble(Robot.drive.localizer.getPose().heading.log()).times(currDriveVel);
        currDriveVel = new PoseVelocity2d(
                new Vector2d(
                        Math.abs(currDriveVel.linearVel.x) < 1.0 ? 0.0 : currDriveVel.linearVel.x,
                        Math.abs(currDriveVel.linearVel.y) < 1.0 ? 0.0 : currDriveVel.linearVel.y
                ),
                currDriveVel.angVel
        );

        Robot.telemetry.addData("Drive Heading (deg)", Math.toDegrees(Robot.drive.localizer.getPose().heading.toDouble()));
        Robot.telemetry.addData("Drive X (in)", Robot.drive.localizer.getPose().position.x);
        Robot.telemetry.addData("Drive Y (in)", Robot.drive.localizer.getPose().position.y);

        Robot.telemetry.addData("Drive Vel X (in/s)", currDriveVel.linearVel.x);
        Robot.telemetry.addData("Drive Vel Y (in/s)", currDriveVel.linearVel.y);

        Robot.telemetry.addData("Drive FL Power", Robot.drive.leftFront.getPower());
        Robot.telemetry.addData("Drive FR Power", Robot.drive.rightFront.getPower());
        Robot.telemetry.addData("Drive BL Power", Robot.drive.leftBack.getPower());
        Robot.telemetry.addData("Drive BR Power", Robot.drive.rightBack.getPower());

        Vector2d driveVector = new Vector2d(0, 0);
        if(FIELD_CENTRIC){
            double theta = Math.atan2(-gamepad1.left_stick_x, -gamepad1.left_stick_y);
            if(Robot.alliance == Robot.Alliance.BLUE){
                theta -= Math.toRadians(90);
            }
            else{
                theta += Math.toRadians(90);
            }
            double mag = Math.sqrt(
                    gamepad1.left_stick_y*gamepad1.left_stick_y+
                    gamepad1.left_stick_x*gamepad1.left_stick_x);

            double newTheta = theta - Robot.drive.localizer.getPose().heading.log();
            driveVector = new Vector2d(
                    mag*Math.cos(newTheta),
                    mag*Math.sin(newTheta)
            );
        }
        else{
            driveVector = new Vector2d(
                    -gamepad1.left_stick_y,
                    -gamepad1.left_stick_x
            );
        }
        double driveMultiplier = gamepad1.left_trigger > 0.8 ? 0.6 : 1.0;
        driveVector = driveVector.times(driveMultiplier);

        double rotation = (-gamepad1.right_stick_x) * driveMultiplier;
        Robot.drive.setDrivePowers(new PoseVelocity2d(
                driveVector,
                rotation
        ));

        Robot.telemetry.update();
    }
}
