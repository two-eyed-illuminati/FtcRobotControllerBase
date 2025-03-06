package org.firstinspires.ftc.teamcode.utils;

import com.acmerobotics.dashboard.config.Config;

//Allow configuration variables to be tuned without pushing code
//with FTC Dashboard (https://acmerobotics.github.io/ftc-dashboard/features#configuration-variables)
@Config
public class Robot{
  //Constants

  //Mechanisms, IMU, etc.
  public static boolean initialized = false;

  public static void initialize(){
    initialized = true;
  }
}
