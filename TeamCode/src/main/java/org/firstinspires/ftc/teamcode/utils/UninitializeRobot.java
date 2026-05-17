package org.firstinspires.ftc.teamcode.utils;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;

@Autonomous(name="Uninitialize Robot", group="Utils")
public class UninitializeRobot extends OpMode {
    @Override
    public void init() {
        Robot.initializeOpMode(hardwareMap, telemetry);
        Robot.initialized = false;
        Robot.telemetry.addLine("Robot successfully uninitialized");
        Robot.telemetry.update();
    }

    @Override
    public void loop() {
        telemetry.addLine("Robot successfully uninitialized; you may stop this OpMode");
        telemetry.update();
    }
}
