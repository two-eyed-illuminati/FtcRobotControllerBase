package org.firstinspires.ftc.teamcode.utils;

import com.acmerobotics.dashboard.FtcDashboard;
import com.acmerobotics.dashboard.telemetry.MultipleTelemetry;
import com.pedropathing.api.PoseFactory;
import com.pedropathing.follower.Follower;
import com.pedropathing.ivy.Scheduler;
import com.pedropathing.math.Pose;
import com.qualcomm.hardware.lynx.LynxModule;
import com.qualcomm.robotcore.hardware.Gamepad;
import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.utils.pedro.Constants;

import java.util.ArrayList;
import java.util.List;

//Game-agnostic robot lifecycle. The static methods are the API used by OpModes.
//A game subclass (Robot) registers itself and overrides the protected hooks below.
public class RobotBase{
  public static Follower follower;
  public static MultipleTelemetry telemetry;
  private static List<LynxModule> hubs = new ArrayList<>();
  private static HardwareMap hardwareMap;
  private static Telemetry dsTelemetry;
  private static RobotBase game = new RobotBase();

  public enum Alliance{
    BLUE, RED
  }
  public static Alliance alliance = Alliance.BLUE;
  public static Pose pose = new Pose(0, 0, 0);
  public static boolean initialized = false;

  protected static void register(RobotBase gameRobot){
    game = gameRobot;
  }

  public static void init(HardwareMap hardwareMap, Telemetry dsTelemetry){
    RobotBase.hardwareMap = hardwareMap;
    RobotBase.dsTelemetry = dsTelemetry;
    Scheduler.reset();

    telemetry = new MultipleTelemetry( //apparently needs to reinit each time opmode inits
            dsTelemetry, // Driver Station telemetry
            FtcDashboard.getInstance().getTelemetry() // Dashboard telemetry
    );

    follower = Constants.create(hardwareMap);

    hubs = hardwareMap.getAll(LynxModule.class);
    for (LynxModule hub : hubs) {
      hub.setBulkCachingMode(LynxModule.BulkCachingMode.MANUAL);
    }

    game.onInit(hardwareMap);

    if(initialized) {
      if(follower != null) {
        follower.setPose(pose);
      }
    }
  }

  public static void initLoop(Gamepad gamepad){
    if(gamepad.left_bumper && gamepad.right_bumper){
      initialized = false;
      init(hardwareMap, dsTelemetry);
    }

    boolean bluePressed = gamepad.xWasPressed();
    boolean redPressed = gamepad.bWasPressed();

    if(initialized){
      telemetry.addLine("Robot using previous initialization state");
      telemetry.addData("Pose", pose);
      telemetry.addLine("Press LB+RB to re-initialize");
    }
    else{
      telemetry.addLine("Robot successfully initialized");
      telemetry.addLine("Press X for BLUE alliance, B for RED alliance");
      if(bluePressed){
        alliance = Alliance.BLUE;
      }
      else if(redPressed){
        alliance = Alliance.RED;
      }
      Pose startPose = toField(game.startPose());
      if(follower != null){
        follower.setPose(startPose);
      }
    }

    telemetry.addData("Current Alliance", alliance);
    telemetry.update();
  }

  public static void loop(){
    initialized = true;
    clearBulkCache();
    if (follower != null) {
      follower.update();
      pose = follower.pose();
    }
  }

  public static void clearBulkCache(){
    for (LynxModule hub : hubs) {
      hub.clearBulkCache();
    }
  }

  public static Pose toField(Pose bluePose){
    return game.fieldPose(bluePose);
  }

  //Runs inside init() after the follower and hubs exist. Create game-specific hardware here.
  protected void onInit(HardwareMap hardwareMap){
  }

  protected Pose startPose(){
    return new Pose(0, 0, 0);
  }

  protected Pose fieldPose(Pose bluePose){
    PoseFactory factory = PoseFactory.radians();
    if(alliance == Alliance.RED){
      factory = factory.rotateAround(70.75, 70.75, Math.PI);
    }
    return factory.of(bluePose.x(), bluePose.y(), bluePose.heading());
  }
}
