package com.example.meepmeep;

import com.acmerobotics.roadrunner.Action;
import com.acmerobotics.roadrunner.InstantAction;
import com.acmerobotics.roadrunner.ParallelAction;
import com.acmerobotics.roadrunner.Pose2d;
import com.acmerobotics.roadrunner.SequentialAction;
import com.acmerobotics.roadrunner.SleepAction;
import com.acmerobotics.roadrunner.TrajectoryActionBuilder;
import com.acmerobotics.roadrunner.TranslationalVelConstraint;
import com.acmerobotics.roadrunner.Vector2d;
import com.acmerobotics.roadrunner.VelConstraint;

import java.util.ArrayList;

public class AutoBuilder {
    ArrayList<String> actions;
    ArrayList<Action> actionObjs;
    TrajectoryActionBuilder currentTab;
    public AutoBuilder(TrajectoryActionBuilder tab){
        this.actions = new ArrayList<>();
        this.actionObjs = new ArrayList<>();
        currentTab = tab;
    }
    public static Pose2d pose2dMapped(Pose2d pose){
        if(Robot.alliance == Robot.Alliance.BLUE){
            return pose;
        }
        else{
            return new Pose2d(pose.position.x, -pose.position.y, -pose.heading.log());
        }
    }

    public static double SPIKE_CLOSE_SHOOT_HEADING = Math.toRadians(-90);
    public static double SPIKE_CLOSE_SHOOT_TANGENT_ANGLE = Math.toRadians(-225);
    public static double SPIKE_1_CLOSE_SHOOT_TANGENT_ANGLE = Math.toRadians(90);
    public static double SPIKE_CLOSE_SHOOT_X = -12.3370432609;
    public static double SPIKE_CLOSE_SHOOT_Y = -15.9996985274;
    public static double PRELOAD_CLOSE_SHOOT_HEADING = Math.toRadians(-126.5);
    public static double PRELOAD_CLOSE_SHOOT_X = -25.3370432609;
    public static double PRELOAD_CLOSE_SHOOT_Y = -22.9996985274;
    public AutoBuilder goToCloseShoot(String type, String tangentType){
        Pose2d endPose = (
                actions.isEmpty() ?
                        new Pose2d(PRELOAD_CLOSE_SHOOT_X, PRELOAD_CLOSE_SHOOT_Y, PRELOAD_CLOSE_SHOOT_HEADING) :
                        new Pose2d(SPIKE_CLOSE_SHOOT_X, SPIKE_CLOSE_SHOOT_Y, SPIKE_CLOSE_SHOOT_HEADING)
        );
        currentTab = currentTab.afterTime(0,
                new ParallelAction(
                        new InstantAction(() -> {
                            Robot.stopIntake();
                        }),
                        Robot.getAimOuttakeTurretAction(pose2dMapped(endPose)),
                        Robot.getShootOuttakeAction(pose2dMapped(endPose))
                )
        );
        if(type.equals("spline")){
            currentTab = currentTab.splineToSplineHeading(
                    endPose,
                    tangentType.equals("1") ? SPIKE_1_CLOSE_SHOOT_TANGENT_ANGLE : SPIKE_CLOSE_SHOOT_TANGENT_ANGLE
            );
        }
        else if(type.equals("strafe")){
            currentTab = currentTab.strafeToLinearHeading(
                    endPose.position,
                    endPose.heading
            );
        }
        currentTab = currentTab.afterTime(0, new InstantAction(() -> {
            Robot.STOP_SHOOT_OUTTAKE_ACTION = true;
            Robot.STOP_AIM_TURRET_ACTION = true;}));
        actionObjs.add(currentTab.build());
        currentTab = currentTab.fresh();
        actions.add("GoToShoot");
        return this;
    }
    public static double FAR_SHOOT_TANGENT_ANGLE = Math.toRadians(90);
    public static double FAR_SHOOT_HEADING = Math.toRadians(-90);
    public static double FAR_SHOOT_X = 50.3370432609;
    public static double FAR_SHOOT_Y = -12.9996985274;
    public AutoBuilder goToFarShoot(String type){
        Pose2d endPose = (
                new Pose2d(FAR_SHOOT_X, FAR_SHOOT_Y, FAR_SHOOT_HEADING)
        );
        currentTab = currentTab.afterTime(0,
                new ParallelAction(
                        new InstantAction(() -> {
                            Robot.stopIntake();
                        }),
                        Robot.getAimOuttakeTurretAction(pose2dMapped(endPose)),
                        Robot.getShootOuttakeAction(pose2dMapped(endPose))
                )
        );
        if(type.equals("spline")){
            currentTab = currentTab.splineToSplineHeading(
                    endPose,
                    FAR_SHOOT_TANGENT_ANGLE
            );
        }
        else if(type.equals("strafe")){
            currentTab = currentTab.strafeToLinearHeading(
                    endPose.position,
                    endPose.heading
            );
        }
        currentTab = currentTab.afterTime(0, new InstantAction(() -> {Robot.STOP_SHOOT_OUTTAKE_ACTION = true;}));
        actionObjs.add(currentTab.build());
        currentTab = currentTab.fresh();
        actions.add("GoToShoot");
        return this;
    }
    public AutoBuilder shoot(){
        actionObjs.add(Robot.getShootSequenceAction());
        actions.add("Shoot");
        return this;
    }

    public static double TO_SPIKE_INITIAL_TANGENT_ANGLE = Math.toRadians(0.0);
    public static double TO_SPIKE_1_INITIAL_TANGENT_ANGLE = Math.toRadians(-90.0);
    public static double TO_SPIKE_1_FROM_PRELOAD_INITIAL_TANGENT_ANGLE = Math.toRadians(-40.0);
    public static double SPIKE_HEADING = Math.toRadians(-90.0);
    public static double SPIKE_START_Y = -29.1017;
    public static double SPIKE_1_X = -12.3457;
    public static double INTAKE_SPEED = 45.0;
    public AutoBuilder goToSpike1(String type){
        VelConstraint constraint = (robotPose, _path, _disp) -> {
            if(Math.abs(robotPose.position.x.value()-SPIKE_1_X) < 5.0 && Math.abs(robotPose.position.y.value()-SPIKE_START_Y) < 7.0){
                return INTAKE_SPEED;
            }
            return 60;
        };
        currentTab = currentTab.afterTime(0, Robot.getReverseIntakeAction());
        currentTab = currentTab.setTangent(type.equals("preload") ? TO_SPIKE_1_FROM_PRELOAD_INITIAL_TANGENT_ANGLE : TO_SPIKE_1_INITIAL_TANGENT_ANGLE).splineToSplineHeading(
                new Pose2d(SPIKE_1_X, SPIKE_START_Y, SPIKE_HEADING),
                SPIKE_HEADING,
                constraint
        );
        actions.add("GoToSpike1");
        return this;
    }

    public static double SPIKE_2_X = 12.3457;
    public AutoBuilder goToSpike2(){
        VelConstraint constraint = (robotPose, _path, _disp) -> {
            if(Math.abs(robotPose.position.x.value()-SPIKE_2_X) < 5.0 && Math.abs(robotPose.position.y.value()-SPIKE_START_Y) < 7.0){
                return INTAKE_SPEED;
            }
            return 60;
        };
        currentTab = currentTab.setTangent(TO_SPIKE_INITIAL_TANGENT_ANGLE).splineToSplineHeading(
                new Pose2d(SPIKE_2_X, SPIKE_START_Y, SPIKE_HEADING),
                SPIKE_HEADING,
                constraint
        );
        actions.add("GoToSpike2");
        return this;
    }

    public static double SPIKE_3_X = 34.3457;
    public AutoBuilder goToSpike3(){
        VelConstraint constraint = (robotPose, _path, _disp) -> {
            if(Math.abs(robotPose.position.x.value()-SPIKE_3_X) < 5.0 && Math.abs(robotPose.position.y.value()-SPIKE_START_Y) < 7.0){
                return INTAKE_SPEED;
            }
            return 60;
        };
        currentTab = currentTab.setTangent(TO_SPIKE_INITIAL_TANGENT_ANGLE).splineToSplineHeading(
                new Pose2d(SPIKE_3_X, SPIKE_START_Y, SPIKE_HEADING),
                SPIKE_HEADING,
                constraint
        );
        actions.add("GoToSpike3");
        return this;
    }


    public static double SPIKE_RAMP_END_Y = -54.1282;
    public static double SPIKE_TUNNEL_END_Y = -62.1282;
    public AutoBuilder intakeSpike1(){
        currentTab = currentTab.afterTime(0, () -> {
            Robot.beginIntake();
        });
        currentTab = currentTab.splineToConstantHeading(
                new Vector2d(SPIKE_1_X, SPIKE_RAMP_END_Y),
                SPIKE_HEADING,
                new TranslationalVelConstraint(INTAKE_SPEED)
        );
        actions.add("IntakeSpike1");
        return this;
    }
    public static double SPIKE_BACKUP_Y = -52.1282;
    public static double SPIKE_BACKUP_TANGENT_ANGLE = Math.toRadians(90.0);
    public AutoBuilder backUpAfterSpike1(){
        currentTab = currentTab.splineToConstantHeading(
                new Vector2d(SPIKE_1_X, SPIKE_BACKUP_Y),
                SPIKE_BACKUP_TANGENT_ANGLE
        );
        actions.add("BackUpAfterIntakeSpike1");
        return this;
    }
    public static double SPIKE_2_END_X = 13.8457;
    public static double SPIKE_2_INTAKE_SPEED = 35.0;
    public AutoBuilder intakeSpike2(){
        currentTab = currentTab.afterTime(0, () -> {
            Robot.beginIntake();
        });
        currentTab = currentTab.splineToConstantHeading(
                new Vector2d(SPIKE_2_END_X, SPIKE_TUNNEL_END_Y),
                SPIKE_HEADING,
                new TranslationalVelConstraint(SPIKE_2_INTAKE_SPEED)
        );
        actions.add("IntakeSpike2");
        return this;
    }
    public AutoBuilder backUpAfterSpike2(){
        currentTab = currentTab.splineToConstantHeading(
                new Vector2d(SPIKE_2_END_X, SPIKE_BACKUP_Y),
                SPIKE_BACKUP_TANGENT_ANGLE
        );
        actions.add("BackUpAfterIntakeSpike2");
        return this;
    }
    public AutoBuilder intakeSpike3(){
        currentTab = currentTab.afterTime(0, () -> {
            Robot.beginIntake();
        });
        currentTab = currentTab.splineToConstantHeading(
                new Vector2d(SPIKE_3_X, SPIKE_TUNNEL_END_Y),
                SPIKE_HEADING,
                new TranslationalVelConstraint(INTAKE_SPEED)
        );
        actions.add("IntakeSpike3");
        return this;
    }
    public AutoBuilder backUpAfterSpike3(){
        currentTab = currentTab.splineToConstantHeading(
                new Vector2d(SPIKE_3_X, SPIKE_BACKUP_Y),
                SPIKE_BACKUP_TANGENT_ANGLE
        );
        actions.add("BackUpAfterIntakeSpike3");
        return this;
    }

    public static double GATE_X_LEFT = -4.0;
    public static double GATE_X_RIGHT = 8.0;
    public static double GATE_Y_BEFORE_HIT = -47.0;
    public static double GATE_Y_HIT = -57.0;
    public static double GATE_HIT_TIME = 0.0;
    public static double GATE_HIT_SPEED = 25.0;
    public AutoBuilder goToGateHit(String side){
        if(!actions.isEmpty() && (actions.get(actions.size()-1).equals("GoToShoot") || actions.get(actions.size()-1).equals("Shoot"))){
            currentTab = currentTab.setTangent(Math.toRadians(0));
        }
        double GATE_X = side.equals("left") ? GATE_X_LEFT : GATE_X_RIGHT;
        VelConstraint constraint = (robotPose, _path, _disp) -> {
            if(Math.abs(robotPose.position.x.value()-GATE_X) < 5.0 && Math.abs(robotPose.position.y.value()-GATE_Y_BEFORE_HIT) < 10.0){
                return GATE_HIT_SPEED;
            }
            return 60;
        };
        currentTab = currentTab.splineToConstantHeading(
                new Vector2d(GATE_X, GATE_Y_BEFORE_HIT),
                SPIKE_HEADING,
                constraint
        ).splineToConstantHeading(
                new Vector2d(GATE_X, GATE_Y_HIT),
                SPIKE_HEADING,
                new TranslationalVelConstraint(GATE_HIT_SPEED)
        );
        if(GATE_HIT_TIME > 0){
            currentTab = currentTab.waitSeconds(GATE_HIT_TIME);
        }
        actions.add("GoToGateHit");
        return this;
    }
    public static double GATE_INTAKE_X = 20.0;
    public static double GATE_INTAKE_Y = -60;
    public static double GATE_INTAKE_HEADING = Math.toRadians(-150);
    public static double GATE_INTAKE_TIME = 0.5;
    public AutoBuilder intakeFromGate(){
        currentTab = currentTab.afterTime(0, () -> {
            Robot.beginIntake();
        });
        if(!actions.isEmpty() && actions.get(actions.size()-1).equals("GoToGateHit")){
            currentTab = currentTab.setTangent(Math.toRadians(20));
        }
        currentTab = currentTab.splineToLinearHeading(
                new Pose2d(GATE_INTAKE_X, GATE_INTAKE_Y, GATE_INTAKE_HEADING),
                Math.toRadians(-90)
        );
        currentTab = currentTab.stopAndAdd(new SleepAction(GATE_INTAKE_TIME)).setTangent(Math.toRadians(90));
        actions.add("IntakeFromGate");
        return this;
    }
    public static double LOOSE_INTAKE_END_X = 40;
    public static double LOOSE_INTAKE_END_Y = -60;
    public AutoBuilder looseIntake(){
        actionObjs.add(Robot.getLooseIntakeAction());
        actionObjs.add(currentTab.build());
        currentTab = currentTab.fresh().strafeTo(new Vector2d(LOOSE_INTAKE_END_X, LOOSE_INTAKE_END_Y)).fresh();
        actions.add("LooseIntake");
        return this;
    }
    public static double OUTSIDE_ZONE_Y = -40.0;
    public AutoBuilder leaveZone(){
        currentTab = currentTab.setTangent(Math.toRadians(-90)).lineToY(OUTSIDE_ZONE_Y);
        actions.add("LeaveZone");
        return this;
    }
    public Action build(){
        actionObjs.add(currentTab.build());
        return new SequentialAction(actionObjs);
    }
}