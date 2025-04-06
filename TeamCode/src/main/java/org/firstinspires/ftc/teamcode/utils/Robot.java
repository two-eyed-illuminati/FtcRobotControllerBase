package org.firstinspires.ftc.teamcode.utils;

import com.acmerobotics.dashboard.FtcDashboard;
import com.acmerobotics.dashboard.config.Config;
import com.acmerobotics.dashboard.telemetry.MultipleTelemetry;
import com.qualcomm.hardware.limelightvision.Limelight3A;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.Servo;

import org.firstinspires.ftc.robotcore.external.Telemetry;

//Allow configuration variables to be tuned without pushing code
//with FTC Dashboard (https://acmerobotics.github.io/ftc-dashboard/features#configuration-variables)
@Config
public class Robot{
  //Constants
  public static double WRIST_MIN_ANGLE = -112.2; // Minimum angle for the wrist servo in degrees
  public static double WRIST_MAX_ANGLE = 162.9; // Maximum angle for the wrist servo in degrees
  public static double WRIST_MIN_SERVO_POS = 1; // Servo pos when wrist is at minimum angle
  public static double WRIST_MAX_SERVO_POS = 0; // Servo pos when wrist is at maximum angle
  public static double WRIST_VELOCITY = 1; // Maximum velocity in revolutions per second for the wrist servo
  public static double SLIDES_MIN_POS = 10; // Minimum position for the slides in inches
  public static double SLIDES_MAX_POS = 36.75; // Maximum position for the slides in inches
  public static double SLIDES_MIN_ENCODER_POS = 0; // Minimum encoder position for the slides in encoder pulses
  public static double SLIDES_MAX_ENCODER_POS = 2208.0; // Maximum encoder position for the slides in encoder pulses
  public static double SLIDES_VELOCITY = 384.5 * 435.0 / 60.0; // Maximum velocity in encoder pulses per second for the slides

  //Mechanisms, IMU, etc.
  public static ServoMechanism wrist;
  public static Limelight3A ll;
  public static DualMotorMechanism slides;
  public static MultipleTelemetry telemetry;
  public static boolean initialized = false;

  public static void initialize(HardwareMap hardwareMap, Telemetry dsTelemetry) {
    Servo wristServo = hardwareMap.get(Servo.class, "wrist");
    wrist = new ServoMechanism(
        wristServo,
        WRIST_MIN_ANGLE, WRIST_MAX_ANGLE,
        WRIST_MIN_SERVO_POS, WRIST_MAX_SERVO_POS,
        WRIST_VELOCITY // maxVel in revolutions per second
    );

    ll = hardwareMap.get(Limelight3A.class, "ll");
    ll.setPollRateHz(100);
    ll.pipelineSwitch(0);
    ll.start();

    DcMotorEx slidesMotor1 = hardwareMap.get(DcMotorEx.class, "liftLeft");
    DcMotorEx slidesMotor2 = hardwareMap.get(DcMotorEx.class, "liftRight");
    slides = new DualMotorMechanism(
        slidesMotor1, slidesMotor2,
        SLIDES_MIN_POS, SLIDES_MAX_POS,
        SLIDES_MIN_ENCODER_POS, SLIDES_MAX_ENCODER_POS,
        SLIDES_VELOCITY // maxVel in encoder pulses per second
    );

    telemetry = new MultipleTelemetry(
        dsTelemetry, // Driver Station telemetry
        FtcDashboard.getInstance().getTelemetry() // Dashboard telemetry
    );

    initialized = true;
  }
}
