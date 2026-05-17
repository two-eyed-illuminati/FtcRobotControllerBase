package org.firstinspires.ftc.teamcode.utils;

import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.Servo;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;

public class ContinuousMotorMechanism extends Mechanism{
    public final DcMotorEx motor;
    public final double posPerEncoderTick;

    /**
     * @param motor the motor to be controlled (make sure desired settings are applied e.g. RUN_TO_POSITION, zero power behavior, etc.)
     * @param maxVel the maximum velocity in degrees per second
     */
    public ContinuousMotorMechanism(DcMotorEx motor,
                          double posPerEncoderTick,
                          double maxVel
    ){
        super(0, 0, maxVel);
        this.motor = motor;
        this.posPerEncoderTick = posPerEncoderTick;
    }

    @Override
    public void setPos(double pos, double maxVel){
        motor.setVelocity(Clamp.clamp(maxVel*(1/posPerEncoderTick), -this.maxVel*(1/posPerEncoderTick), this.maxVel*(1/posPerEncoderTick)));
        targetVel = maxVel;
        return;
    }

    @Override
    public double getPos(){
        return motor.getCurrentPosition()*posPerEncoderTick;
    }

    @Override
    public double getVel(){
        return motor.getVelocity()*posPerEncoderTick;
    }
}
