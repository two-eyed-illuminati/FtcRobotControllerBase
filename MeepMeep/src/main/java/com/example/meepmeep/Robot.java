package com.example.meepmeep;

import com.acmerobotics.dashboard.telemetry.TelemetryPacket;
import com.acmerobotics.roadrunner.Action;
import com.acmerobotics.roadrunner.InstantAction;
import com.acmerobotics.roadrunner.Pose2d;
import com.acmerobotics.roadrunner.PoseVelocity2d;
import com.acmerobotics.roadrunner.Rotation2d;
import com.acmerobotics.roadrunner.SleepAction;
import com.acmerobotics.roadrunner.Vector2d;

public class Robot{
    public enum Alliance{
        BLUE, RED
    }
    public static Alliance alliance = Alliance.RED; //0 = blue, 1 = red
    public static boolean STOP_SHOOT_OUTTAKE_ACTION = false;
    public static boolean STOP_AIM_TURRET_ACTION = false;
    public static void beginIntake(){}
    public static void stopIntake(){}
    public static Action getLooseIntakeAction(){
        return new InstantAction(() -> {});
    }
    public static Action getReverseIntakeAction(){
        return new InstantAction(() -> {});
    }
    public static Action getAimOuttakeTurretAction(Pose2d endPose){
        return new InstantAction(() -> {});
    }
    public static Action getShootOuttakeAction(Pose2d endPose){
        return new InstantAction(() -> {});
    }
    public static Action getShootSequenceAction(){
        return new SleepAction(1.2);
    }
}
