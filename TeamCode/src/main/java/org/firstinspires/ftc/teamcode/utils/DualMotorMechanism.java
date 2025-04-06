package org.firstinspires.ftc.teamcode.utils;

import com.qualcomm.robotcore.hardware.DcMotorEx;

public class DualMotorMechanism{
    private final MotorMechanism motor1;
    private final MotorMechanism motor2;

    /**
     * @param motor1 motor 1
     * @param motor2 motor 2
     * @param minPos the minimum position in whatever units
     * @param maxPos the maximum position in units
     * @param minEncoderPos the minimum position in encoder pulses for the primary motor
     * @param maxEncoderPos the maximum position in encoder pulses for the primary motor
     * @param maxVel the maximum velocity in encoder pulses per second
     */
    public DualMotorMechanism(DcMotorEx motor1, DcMotorEx motor2,
                              double minPos, double maxPos,
                              double minEncoderPos, double maxEncoderPos,
                              double maxVel) {
        this.motor1 = new MotorMechanism(motor1,
                minPos, maxPos,
                minEncoderPos, maxEncoderPos,
                maxVel
        );
        this.motor2 = new MotorMechanism(motor2,
                minPos, maxPos,
                minEncoderPos, maxEncoderPos,
                maxVel
        );
    }

    public void setPos(double pos, double maxVel) {
        motor1.setPos(pos, maxVel);
        motor2.setPos(pos, maxVel);
    }
    public void setPos(double pos) {
        setPos(pos, motor1.maxVel);
    }

    public double getPos() {
        return motor1.getPos();
    }
}
