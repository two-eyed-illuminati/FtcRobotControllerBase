package com.example.meepmeep;

import com.acmerobotics.roadrunner.Pose2d;
import com.acmerobotics.roadrunner.SequentialAction;
import com.acmerobotics.roadrunner.SleepAction;
import com.acmerobotics.roadrunner.TrajectoryActionBuilder;
import com.acmerobotics.roadrunner.TranslationalVelConstraint;
import com.acmerobotics.roadrunner.Vector2d;
import com.noahbres.meepmeep.roadrunner.DefaultBotBuilder;
import com.noahbres.meepmeep.roadrunner.entity.RoadRunnerBotEntity;

public class MeepMeepSpline {
    // --- Copy of your constants ---
    public static double START_X = -57.0586;
    public static double START_Y = -43.8964;
    public static double START_HEADING = -126.5;
    public static double PRELOAD_SHOOT_X = -45.3370432609;
    public static double PRELOAD_SHOOT_Y = -24.9996985274;
    public static double PRELOAD_SHOOT_HEADING = -128.71;
    public static double SPIKE_SHOOT_X = -20.3370432609;
    public static double SPIKE_SHOOT_Y = -24.9996985274;
    public static double SPIKE_SHOOT_HEADING = -90;
    public static double SPIKE_START_Y = -22.1017;
    public static double SPIKE_RAMP_END_Y = -52.1282;
    public static double SPIKE_TUNNEL_END_Y = -58.1282;
    public static double SPIKE_HEADING = -90.0;
    public static double SPIKE_1_X = -12.3457;
    public static double SPIKE_2_X = 11.3457;
    public static double SPIKE_2_END_X = 12.8457;
    public static double SPIKE_3_X = 34.3457;

    // --- Adapted helper methods (Removed Robot hardware calls) ---
    static TrajectoryActionBuilder trajToShoot(TrajectoryActionBuilder builder, boolean preload) {
        if(preload){
            Pose2d endRobotPose = new Pose2d(PRELOAD_SHOOT_X, PRELOAD_SHOOT_Y, Math.toRadians(PRELOAD_SHOOT_HEADING));
            return builder.strafeToConstantHeading(
                    endRobotPose.position
            );
        }
        else{
            Pose2d endRobotPose = new Pose2d(SPIKE_SHOOT_X, SPIKE_SHOOT_Y, Math.toRadians(SPIKE_SHOOT_HEADING));
            return builder.splineToSplineHeading(
                    endRobotPose,
                    Math.toRadians(-190)
            );
        }
    }

    static TrajectoryActionBuilder intakeFromSpike(TrajectoryActionBuilder builder, int spike) {
        double currSpikeX = spike == 1 ? SPIKE_1_X : (spike == 2 ? SPIKE_2_X : SPIKE_3_X);
        double endSpikeX = spike == 2 ? SPIKE_2_END_X : currSpikeX;
        double endSpikeY = spike <= 1 ? SPIKE_RAMP_END_Y : SPIKE_TUNNEL_END_Y;
        return builder.splineToSplineHeading(
                        new Pose2d(endSpikeX, endSpikeY, Math.toRadians(SPIKE_HEADING)),
                        Math.toRadians(-SPIKE_HEADING),
                        new TranslationalVelConstraint(25.0)
                )
                .splineToSplineHeading(
                        new Pose2d(endSpikeX, endSpikeY+5, Math.toRadians((SPIKE_HEADING*4 + SPIKE_SHOOT_HEADING) / 5.0)),
                        Math.toRadians(-SPIKE_HEADING),
                        new TranslationalVelConstraint(25.0)
                );
    }

    public static void main(String[] args) {
        com.noahbres.meepmeep.MeepMeep meepMeep = new com.noahbres.meepmeep.MeepMeep(800);

        RoadRunnerBotEntity myBot = new DefaultBotBuilder(meepMeep)
                // Robot constraints
                .setConstraints(60, 60, Math.toRadians(180), Math.toRadians(180), 15)
                .build();

        Pose2d startPose = new Pose2d(START_X, START_Y, Math.toRadians(START_HEADING));

        // --- Recreating your Action Logic ---

        // 1. Preload Shoot Path
        TrajectoryActionBuilder preloadShootTab = trajToShoot(myBot.getDrive().actionBuilder(startPose), true);

        // 2. To Spike 1
        TrajectoryActionBuilder toSpike1Tab = preloadShootTab.fresh()
                .setTangent(Math.toRadians(20))
                .splineToSplineHeading(
                        new Pose2d(SPIKE_1_X, SPIKE_START_Y, Math.toRadians(SPIKE_HEADING)),
                        Math.toRadians(SPIKE_HEADING)
                );

        TrajectoryActionBuilder toSpike1IntakeAndShootTab = trajToShoot(intakeFromSpike(toSpike1Tab, 1), false);

        // 3. To Spike 2
        TrajectoryActionBuilder toSpike2Tab = toSpike1IntakeAndShootTab.fresh()
                .setTangent(Math.toRadians(0))
                .splineToSplineHeading(
                        new Pose2d(SPIKE_2_X, SPIKE_START_Y, Math.toRadians(SPIKE_HEADING)),
                        Math.toRadians(SPIKE_HEADING)
                );

        TrajectoryActionBuilder toSpike2IntakeAndShootTab = trajToShoot(intakeFromSpike(toSpike2Tab, 2), false);

        // 4. Leave
        TrajectoryActionBuilder leaveLaunchZoneTab = toSpike2IntakeAndShootTab.fresh().strafeToLinearHeading(
                new Vector2d(SPIKE_SHOOT_X + 20, SPIKE_SHOOT_Y),
                0
        );

        // Run it all in sequence
        myBot.runAction(new SequentialAction(
                preloadShootTab.build(),
                new SleepAction(3.5), // Placeholder for doPreloadShoot
                toSpike1IntakeAndShootTab.build(),
                new SleepAction(3.5), // Placeholder for doSpike1Shoot
                toSpike2IntakeAndShootTab.build(),
                new SleepAction(3.5), // Placeholder for doSpike2Shoot
                leaveLaunchZoneTab.build()
        ));

        meepMeep.setBackground(com.noahbres.meepmeep.MeepMeep.Background.FIELD_DECODE_JUICE_DARK) // You can change this to match your season
                .setDarkMode(true)
                .setBackgroundAlpha(0.95f)
                .addEntity(myBot)
                .start();
    }
}