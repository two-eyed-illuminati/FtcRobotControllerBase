package org.firstinspires.ftc.teamcode.utils;

import com.bylazar.configurables.annotations.Configurable;
import com.bylazar.telemetry.JoinedTelemetry;
import com.bylazar.telemetry.PanelsTelemetry;
import com.pedropathing.follower.Follower;
import com.qualcomm.hardware.lynx.LynxModule;
import com.qualcomm.robotcore.hardware.Gamepad;
import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.utils.pedro.Constants;

import java.util.List;

// Allow configuration variables to be tuned without pushing code in Panels.
@Configurable
public class Robot{
  //Constants

  //Mechanisms, IMU, etc.
  public static Follower follower;
  public static Telemetry telemetry;
  //Stored Values
  public enum Alliance{
    BLUE, RED
  }
  public static Alliance alliance = Alliance.BLUE; //0 = blue, 1 = red
  public static boolean initialized = false;

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
    telemetry = new JoinedTelemetry( // Needs to be recreated for each OpMode.
            PanelsTelemetry.INSTANCE.getFtcTelemetry(), // Panels telemetry
            dsTelemetry // Driver Station telemetry
    );

    List<LynxModule> allHubs;
    allHubs = hardwareMap.getAll(LynxModule.class);
    for (LynxModule hub : allHubs) {
      hub.setBulkCachingMode(LynxModule.BulkCachingMode.MANUAL);
    }

    if(initialized) {
      //Do things like setting motor zero power behavior, direction, etc.
    }
  }
}
