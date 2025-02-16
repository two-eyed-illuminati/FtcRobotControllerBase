package org.firstinspires.ftc.teamcode.utils;

import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.util.ElapsedTime;

public class ServoMechanism extends Mechanism{
    private final Servo servo;
    private final double minServoPos;
    private final double maxServoPos;
    private double prevPos;
    private final ElapsedTime time = new ElapsedTime();
    public double toServoPos(double angle){
        return ((maxServoPos - minServoPos)/(maxPos - minPos))*(angle - minPos)+minServoPos;
    }
    public double toDegrees(double servoPos){
        return ((maxPos - minPos)/(maxServoPos - minServoPos))*(servoPos - minServoPos)+minPos;
    }

    /**
    * @param servo the servo to be controlled
    * @param minPos the minimum position in degrees
    * @param maxPos the maximum position in degrees
    * @param minServoPos the minimum position in servo position units
    * @param maxServoPos the maximum position in servo position units
    * @param maxVel the maximum velocity in revolutions per second
     */
    public ServoMechanism(Servo servo,
                          double minPos, double maxPos,
                          double minServoPos, double maxServoPos,
                          double maxVel
    ){
        super(minPos, maxPos, maxVel/360);
        this.minServoPos = minServoPos;
        this.maxServoPos = maxServoPos;
        this.prevPos = servo.getPosition();
        this.servo = servo;
    }

    @Override
    //TODO implement maxVel
    public void setPos(double angle, double maxVel){
        prevPos = toDegrees(getPos());
        time.reset();
        servo.setPosition(Clamp.clamp(toServoPos(angle), minServoPos, maxServoPos));
    }

    @Override
    public double getPos(){
        return prevPos > toDegrees(servo.getPosition()) ?
                Math.max(prevPos - time.seconds() * maxVel, toDegrees(servo.getPosition())) :
                Math.min(prevPos + time.seconds() * maxVel, toDegrees(servo.getPosition()));
    }
}
