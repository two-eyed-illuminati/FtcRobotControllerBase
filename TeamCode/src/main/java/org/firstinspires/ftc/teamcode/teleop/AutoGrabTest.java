package org.firstinspires.ftc.teamcode.teleop;

import com.acmerobotics.dashboard.FtcDashboard;
import com.acmerobotics.dashboard.telemetry.MultipleTelemetry;
import com.qualcomm.hardware.limelightvision.LLResult;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.teamcode.utils.Robot;

import dev.frozenmilk.dairy.core.util.supplier.logical.EnhancedBooleanSupplier;
import dev.frozenmilk.dairy.pasteurized.Pasteurized;

@TeleOp
public class AutoGrabTest extends OpMode {
    EnhancedBooleanSupplier startAutoGrab;
    double CAMERA_HEIGHT = 5.0; // Difference in height between camera and samples
    boolean findingGrabPose = false;

    @Override
    public void init(){
        if(!Robot.initialized){
            Robot.initialize(hardwareMap, telemetry);
        }
        startAutoGrab = Pasteurized.gamepad1().leftTrigger().
                conditionalBindState().greaterThan(0.8).bind();
    }
    @Override
    public void loop(){
        if(startAutoGrab.onTrue()) findingGrabPose = true;
        if(!findingGrabPose) return;

        LLResult result = Robot.ll.getLatestResult();
        double[] pythonOutputs = result.getPythonOutput();
        if (pythonOutputs[0] != 0) {
            double tx = result.getTx(); // How far left or right the target is (degrees)
            double ty = result.getTy(); // How far up or down the target is (degrees)
            double angle = pythonOutputs[1];

            Robot.telemetry.addData("Target X", tx);
            Robot.telemetry.addData("Target Y", ty);
            Robot.telemetry.addData("Target Angle", angle);

            Robot.wrist.setPos(angle + 90 > Robot.wrist.maxPos ? angle - 90 : angle + 90);
            Robot.slides.setPos(Robot.slides.getPos() + Math.tan(Math.toRadians(ty)) * CAMERA_HEIGHT);

            findingGrabPose = false;
        } else {
            Robot.telemetry.addData("Target X", "N/A");
            Robot.telemetry.addData("Target Y", "N/A");
            Robot.telemetry.addData("Target Angle", "N/A");
        }

        Robot.telemetry.update();
    }
}
