package org.firstinspires.ftc.teamcode.utils;

import com.acmerobotics.dashboard.FtcDashboard;
import com.acmerobotics.dashboard.config.Config;
import com.acmerobotics.dashboard.telemetry.MultipleTelemetry;
import com.acmerobotics.roadrunner.Pose2d;
import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.utils.rr.MecanumDrive;

//Allow configuration variables to be tuned without pushing code
//with FTC Dashboard (https://acmerobotics.github.io/ftc-dashboard/features#configuration-variables)
@Config
public class Robot{
  //Constants

  //Mechanisms, IMU, etc.
  public static MecanumDrive drive;
  public static MultipleTelemetry telemetry;
  public static boolean initialized = false;

  public static void initialize(HardwareMap hardwareMap, Telemetry dsTelemetry){
    drive = new MecanumDrive(hardwareMap, new Pose2d(0, 0, 0));
    telemetry = new MultipleTelemetry(
        dsTelemetry, // Driver Station telemetry
        FtcDashboard.getInstance().getTelemetry() // Dashboard telemetry
    );

    initialized = true;
  }
}