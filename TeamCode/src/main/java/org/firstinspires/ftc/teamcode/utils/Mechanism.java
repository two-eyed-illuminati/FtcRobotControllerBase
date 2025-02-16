package org.firstinspires.ftc.teamcode.utils;

public abstract class Mechanism {
    public double minPos; //Min position in position units
    public double maxPos; //Max position in position units
    public double maxVel; //Max velocity in position units per second

    public Mechanism(double minPos, double maxPos, double maxVel){
        this.minPos = minPos;
        this.maxPos = maxPos;
        this.maxVel = maxVel;
    }

    public abstract void setPos(double pos, double maxVel);
    public void setPos(double pos){
        setPos(pos, maxVel);
    }
    public abstract double getPos();
}
