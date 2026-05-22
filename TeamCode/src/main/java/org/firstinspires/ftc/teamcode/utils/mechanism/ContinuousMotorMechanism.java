package org.firstinspires.ftc.teamcode.utils.mechanism;

import com.qualcomm.robotcore.hardware.DcMotorEx;

import org.firstinspires.ftc.teamcode.utils.Clamp;
import org.firstinspires.ftc.teamcode.utils.Controller;

public class ContinuousMotorMechanism extends Mechanism {
    public final DcMotorEx motor;
    public final double posPerEncoderTick;

    /**
     * @param motor  the motor to be controlled (make sure desired settings are applied e.g. RUN_TO_POSITION, zero power behavior, etc.)
     * @param posPerEncoderTick the position units per encoder tick (e.g. degrees per encoder tick)
     * @param maxVel the maximum velocity in degrees per second
     * @param controller the controller to be used (NOTE: If using a custom controller, the setPos method must be called every loop)
     */
    public ContinuousMotorMechanism(DcMotorEx motor,
                                    double posPerEncoderTick,
                                    double maxVel,
                                    Controller controller
    ) {
        super(0, 0, maxVel, controller);
        this.motor = motor;
        this.posPerEncoderTick = posPerEncoderTick;
    }

    public void setVel(double vel) {
        targetVel = Clamp.clamp(vel * (1 / posPerEncoderTick), -this.maxVel * (1 / posPerEncoderTick), this.maxVel * (1 / posPerEncoderTick));
        targetPos = targetVel;
        if(!usingCustomController) {
            motor.setVelocity(targetVel);
        }
        else{
            motor.setPower(controller.getPower(motor.getVelocity() * posPerEncoderTick, targetVel));
        }
    }
    @Override
    public void setPos(double pos, double maxVel) {
        setVel(pos);
    }

    @Override
    public double getVel() {
        return motor.getVelocity() * posPerEncoderTick;
    }
    @Override
    public double getPos() {
        return getVel();
    }
}
