package org.firstinspires.ftc.teamcode.utils;

import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.Servo;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;

public class ContinuousMotorMechanism extends Mechanism {
    public final DcMotorEx motor;
    public final double posPerEncoderTick;

    /**
     * @param motor  the motor to be controlled (make sure desired settings are applied e.g. RUN_TO_POSITION, zero power behavior, etc.)
     * @param maxVel the maximum velocity in degrees per second
     */
    public ContinuousMotorMechanism(DcMotorEx motor,
                                    double posPerEncoderTick,
                                    double maxVel
    ) {
        super(0, 0, maxVel);
        this.motor = motor;
        this.posPerEncoderTick = posPerEncoderTick;
    }

    public void setVel(double vel) {
        targetVel = Clamp.clamp(vel * (1 / posPerEncoderTick), -this.maxVel * (1 / posPerEncoderTick), this.maxVel * (1 / posPerEncoderTick));
        targetPos = targetVel;
        motor.setVelocity(targetVel);
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
