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
import com.qualcomm.robotcore.util.RobotLog;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.robotcore.external.navigation.VoltageUnit;
import org.firstinspires.ftc.teamcode.utils.log.RecordingTelemetry;
import org.firstinspires.ftc.teamcode.utils.log.RunLog;
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
  private static String opModeName;
  private static RunLog runLog;
  private static final double VOLTAGE_REFRESH_SECONDS = 1.0;
  private static long lastLoopNanos;
  private static long lastVoltageNanos;
  private static double batteryVolts;
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

  public static void init(HardwareMap hardwareMap, Telemetry dsTelemetry, String opModeName){
    RobotBase.hardwareMap = hardwareMap;
    RobotBase.dsTelemetry = dsTelemetry;
    RobotBase.opModeName = opModeName;
    Scheduler.reset();

    closeRunLog();
    runLog = RunLog.open(opModeName);

    telemetry = new RecordingTelemetry( //apparently needs to reinit each time opmode inits
            runLog,
            dsTelemetry, // Driver Station telemetry
            FtcDashboard.getInstance().getTelemetry() // Dashboard telemetry
    );

    follower = Constants.create(hardwareMap);
    if(follower == null) {
      RobotLog.ii("RunLog", "no follower; only telemetry is logged");
    }

    hubs = hardwareMap.getAll(LynxModule.class);
    lastLoopNanos = 0;
    lastVoltageNanos = 0;
    RobotLog.ii("Robot", "hubs=%d, voltage refresh=%.1fs", hubs.size(), VOLTAGE_REFRESH_SECONDS);
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
      init(hardwareMap, dsTelemetry, opModeName);
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
      runLog.sample(follower);
    }
    addLoopTelemetry();
  }

  private static void addLoopTelemetry(){
    telemetry.addData("Pose", pose);
    long now = System.nanoTime();
    if(lastLoopNanos != 0){
      telemetry.addData("Loop time (ms)", "%.1f", (now - lastLoopNanos) / 1e6);
    }
    lastLoopNanos = now;

    if(lastVoltageNanos == 0 || (now - lastVoltageNanos) / 1e9 >= VOLTAGE_REFRESH_SECONDS){
      batteryVolts = readBatteryVolts();
      lastVoltageNanos = now;
    }
    telemetry.addData("Battery (V)", "%.2f", batteryVolts);
  }

  private static double readBatteryVolts(){
    double lowest = Double.POSITIVE_INFINITY;
    for (LynxModule hub : hubs) {
      lowest = Math.min(lowest, hub.getInputVoltage(VoltageUnit.VOLTS));
    }
    return lowest;
  }

  public static void closeRunLog(){
    if(runLog != null) {
      runLog.close();
      runLog = null;
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
