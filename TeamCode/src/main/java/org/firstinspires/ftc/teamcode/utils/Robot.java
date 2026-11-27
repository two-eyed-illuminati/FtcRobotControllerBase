package org.firstinspires.ftc.teamcode.utils;

import com.acmerobotics.dashboard.config.Config;
import com.pedropathing.math.Pose;
import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.robotcore.external.Telemetry;

//Allow configuration variables to be tuned without pushing code
//with FTC Dashboard (https://acmerobotics.github.io/ftc-dashboard/features#configuration-variables)
@Config
public class Robot extends RobotBase{
  //Constants
  public static double DRIVER_FORWARD_HEADING = Math.PI;
  public static Pose DEFAULT_START_POSE = new Pose(110, 132, 3 * Math.PI / 2);
  
  //Mechanisms
  //ex: public static DcMotorEx shooter;

  public static void init(HardwareMap hardwareMap, Telemetry dsTelemetry){
    register(new Robot());
    RobotBase.init(hardwareMap, dsTelemetry);
  }

  @Override
  protected void onInit(HardwareMap hardwareMap){
    if (!initialized){
      //ex: shooter = hardwareMap.get(DcMotorEx.class, "shooter");
    }
    //ex: shooter.setDirection(DcMotorSimple.Direction.REVERSE);
  }

  @Override
  protected Pose startPose(){
    return DEFAULT_START_POSE;
  }
}
