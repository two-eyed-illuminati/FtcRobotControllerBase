package org.firstinspires.ftc.teamcode.utils;

import com.acmerobotics.dashboard.FtcDashboard;
import com.acmerobotics.dashboard.config.Config;
import com.acmerobotics.dashboard.telemetry.MultipleTelemetry;
import com.pedropathing.follower.Follower;
import com.qualcomm.hardware.lynx.LynxModule;
import com.qualcomm.robotcore.hardware.Gamepad;
import com.qualcomm.robotcore.hardware.HardwareMap;

import com.pedropathing.math.Pose;
import com.pedropathing.api.PoseFactory;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.utils.pedro.Constants;

import java.util.List;

//Allow configuration variables to be tuned without pushing code
//with FTC Dashboard (https://acmerobotics.github.io/ftc-dashboard/features#configuration-variables)
@Config
public class Robot{
  //Constants
  public static Pose START_POSE_CENTER = new Pose(72, 72, 0);

  //Mechanisms, IMU, etc.
  public static Follower follower;
  public static MultipleTelemetry telemetry;
  
  //Stored Values
  public enum Alliance{
    BLUE, RED
  }
  public static Alliance alliance = Alliance.BLUE; //0 = blue, 1 = red
  public static Pose pose = new Pose(0, 0, 0);
  public static boolean initialized = false;

  public static Pose toField(Pose bluePose){
    PoseFactory factory = PoseFactory.radians();
    if(alliance == Alliance.RED){
      factory = factory.mirrorX(72);
    }
    return factory.of(bluePose.x(), bluePose.y(), bluePose.heading());
  }

  public static void requestAlliance(Gamepad gamepad){
    telemetry.addLine("Press X for BLUE alliance, B for RED alliance");

    if(gamepad.x){
      alliance = Alliance.BLUE;
    }
    else if(gamepad.b){
      alliance = Alliance.RED;
    }

    telemetry.addData("Current Alliance", alliance);
    telemetry.update();
  }

  public static void initialize(HardwareMap hardwareMap, Telemetry dsTelemetry){
    follower = Constants.create(hardwareMap);
    initializeOpMode(hardwareMap, dsTelemetry);

    if(!initialized) {
      telemetry.addLine("Robot successfully initialized");
    }
    else{
      telemetry.addLine("Robot using previous initialization state");
      telemetry.addLine("If you would like to uninitialize, run the \"Uninitialize Robot\" OpMode");
    }

    initialized = true;
    telemetry.update();
  }

  public static void initializeOpMode(HardwareMap hardwareMap, Telemetry dsTelemetry){
    telemetry = new MultipleTelemetry( //apparently needs to reinit each time opmode inits
            dsTelemetry, // Driver Station telemetry
            FtcDashboard.getInstance().getTelemetry() // Dashboard telemetry
    );

    List<LynxModule> allHubs;
    allHubs = hardwareMap.getAll(LynxModule.class);
    for (LynxModule hub : allHubs) {
      hub.setBulkCachingMode(LynxModule.BulkCachingMode.MANUAL);
    }

    if(initialized) {
      //Do things like setting motor zero power behavior, direction, etc.
      follower.setPose(pose);
    }
  }
}
