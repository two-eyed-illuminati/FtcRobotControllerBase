package com.example.meepmeep;

import com.acmerobotics.roadrunner.IdentityPoseMap;
import com.acmerobotics.roadrunner.Pose2d;
import com.acmerobotics.roadrunner.Pose2dDual;
import com.acmerobotics.roadrunner.PoseMap;
import com.acmerobotics.roadrunner.TrajectoryActionBuilder;
import com.noahbres.meepmeep.roadrunner.DefaultBotBuilder;
import com.noahbres.meepmeep.roadrunner.entity.RoadRunnerBotEntity;

public class MeepMeepCurrent {
    public static void main(String[] args) {
        com.noahbres.meepmeep.MeepMeep meepMeep = new com.noahbres.meepmeep.MeepMeep(800);

        RoadRunnerBotEntity myBot = new DefaultBotBuilder(meepMeep)
                // Robot constraints
                .setConstraints(60, 50, Math.toRadians(180), Math.toRadians(180), 14)
                .setDimensions(15, 18)
                .build();

//        double START_X = 63.0;
//        double START_Y = -10.0;
//        double START_HEADING = -90.0;
        double START_X = -49.0;
        double START_Y = -50.5;
        double START_HEADING = -126.5;

        Pose2d startPose = new Pose2d(START_X, START_Y, Math.toRadians(START_HEADING));

        PoseMap poseMap = Robot.alliance == Robot.Alliance.BLUE ? new IdentityPoseMap() :
                pose -> new Pose2dDual<>(pose.position.x, pose.position.y.unaryMinus(), pose.heading.inverse());

        TrajectoryActionBuilder tab = myBot.getDrive().actionBuilder(startPose);
        TrajectoryActionBuilder tabMapped = new TrajectoryActionBuilder(
                tab.getTurnActionFactory(),
                tab.getTrajectoryActionFactory(),
                tab.getTrajectoryBuilderParams(),
                startPose,
                tab.getBeginEndVel(),
                tab.getBaseTurnConstraints(),
                tab.getBaseVelConstraint(),
                tab.getBaseAccelConstraint(),
                poseMap
        );
        AutoBuilder autoBuilder = new AutoBuilder(tabMapped);

        autoBuilder
                .goToCloseShoot("strafe", "")
                .shoot();
        autoBuilder
                .goToSpike2()
                .intakeSpike2()
                .backUpAfterSpike2()
                .goToCloseShoot("spline", "")
                .shoot();
        autoBuilder
                .goToGateHit("right")
                .intakeFromGate()
                .goToCloseShoot("strafe", "")
                .shoot();
        autoBuilder
                .goToGateHit("right")
                .intakeFromGate()
                .goToCloseShoot("strafe", "")
                .shoot();
        autoBuilder
                .goToSpike1("")
                .intakeSpike1()
                .backUpAfterSpike1()
                .goToCloseShoot("spline", "1")
                .shoot();
        autoBuilder
                .leaveZone();

        myBot.runAction(autoBuilder.build());

        meepMeep.setBackground(com.noahbres.meepmeep.MeepMeep.Background.FIELD_DECODE_JUICE_DARK) // You can change this to match your season
                .setDarkMode(true)
                .setBackgroundAlpha(0.95f)
                .addEntity(myBot)
                .start();
    }
}