package org.firstinspires.ftc.teamcode.utils;

import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.Servo;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;

public class MotorMechanism extends Mechanism{
    public final DcMotorEx motor;
    public final double minEncoderPos;
    public final double maxEncoderPos;
    public double toEncoderPos(double angle){
        return ((maxEncoderPos - minEncoderPos)/(maxPos - minPos))*(angle - minPos)+minEncoderPos;
    }
    public double toUnits(double encoderPos){
        return ((maxPos - minPos)/(maxEncoderPos - minEncoderPos))*(encoderPos - minEncoderPos)+minPos;
    }

    /**
     * @param motor the motor to be controlled (make sure desired settings are applied e.g. RUN_TO_POSITION, zero power behavior, etc.)
     * @param minPos the minimum position in whatever units
     * @param maxPos the maximum position in units
     * @param minEncoderPos the minimum position in encoder pulses
     * @param maxEncoderPos the maximum position in encoder pulses
     * @param maxVel the maximum velocity in degrees per second
     */
    public MotorMechanism(DcMotorEx motor,
                          double minPos, double maxPos,
                          double minEncoderPos, double maxEncoderPos,
                          double maxVel
    ){
        super(minPos, maxPos, maxVel);
        this.motor = motor;
        this.minEncoderPos = minEncoderPos;
        this.maxEncoderPos = maxEncoderPos;
    }

    @Override
    public void setPos(double pos, double maxVel){
        motor.setVelocity(Math.abs(((maxEncoderPos - minEncoderPos)/(maxPos - minPos))*maxVel));
        motor.setTargetPosition((int)Clamp.clamp(toEncoderPos(pos), minEncoderPos, maxEncoderPos));
        targetVel = maxVel;
    }

    @Override
    public double getPos(){
        return toUnits(motor.getCurrentPosition());
    }

    @Override
    public double getVel(){
        return motor.getVelocity()*((maxPos - minPos)/(maxEncoderPos - minEncoderPos));
    }
}
