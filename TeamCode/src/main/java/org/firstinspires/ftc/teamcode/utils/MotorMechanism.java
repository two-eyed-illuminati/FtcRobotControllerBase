package org.firstinspires.ftc.teamcode.utils;

import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.Servo;

public class MotorMechanism extends Mechanism{
    private final DcMotorEx motor;
    private final double minEncoderPos;
    private final double maxEncoderPos;
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
     * @param maxVel the maximum velocity in encoder pulses per second
     */
    public MotorMechanism(DcMotorEx motor,
                          double minPos, double maxPos,
                          double minEncoderPos, double maxEncoderPos,
                          double maxVel
    ){
        super(minPos, maxPos, ((maxPos - minPos)/(maxEncoderPos - minEncoderPos))*maxVel);
        this.motor = motor;
        this.minEncoderPos = minEncoderPos;
        this.maxEncoderPos = maxEncoderPos;
    }

    @Override
    public void setPos(double pos, double maxVel){
        motor.setVelocity(maxVel);
        motor.setTargetPosition((int)Clamp.clamp(toEncoderPos(pos), minEncoderPos, maxEncoderPos));
    }

    @Override
    public double getPos(){
        return toUnits(motor.getCurrentPosition());
    }
}
