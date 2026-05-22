package org.firstinspires.ftc.teamcode.utils.mechanism;

import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.teamcode.utils.Clamp;
import org.firstinspires.ftc.teamcode.utils.Controller;

public class ServoMechanism extends Mechanism{
    public final Servo servo;
    public final double minServoPos;
    public final double maxServoPos;
    public double prevPos;
    public final ElapsedTime time = new ElapsedTime();
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
    * @param controller the controller to be used (NOTE: If using a custom controller, the setPos method must be called every loop)
    */
    public ServoMechanism(Servo servo,
                          double minPos, double maxPos,
                          double minServoPos, double maxServoPos,
                          double maxVel,
                          Controller controller
    ){
        super(minPos, maxPos, maxVel*360, controller);
        this.minServoPos = minServoPos;
        this.maxServoPos = maxServoPos;
        this.prevPos = servo.getPosition();
        this.servo = servo;
    }

    @Override
    //TODO implement maxVel, controller
    public void setPos(double angle, double maxVel){
        prevPos = getPos();
        time.reset();
        servo.setPosition(toServoPos(Clamp.clamp(angle, minPos, maxPos)));
        targetPos = Clamp.clamp(angle, minPos, maxPos);
        targetVel = maxVel;
    }

    @Override
    public double getPos(){
        return prevPos > toDegrees(servo.getPosition()) ?
                Math.max(prevPos - time.seconds() * maxVel, toDegrees(servo.getPosition())) :
                Math.min(prevPos + time.seconds() * maxVel, toDegrees(servo.getPosition()));
    }

    @Override
    public double getVel(){
        return 0; //Not implemented
    }
}
