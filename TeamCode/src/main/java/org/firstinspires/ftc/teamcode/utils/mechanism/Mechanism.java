package org.firstinspires.ftc.teamcode.utils.mechanism;

import org.firstinspires.ftc.teamcode.utils.Controller;

public abstract class Mechanism {
    public boolean usingCustomController;
    public Controller controller;
    public double minPos; //Min position in position units
    public double maxPos; //Max position in position units
    public double maxVel; //Max velocity in position units per second
    public double targetPos = 0;
    public double targetVel = 0;

    public Mechanism(double minPos, double maxPos, double maxVel, Controller controller){
        this.minPos = minPos;
        this.maxPos = maxPos;
        this.maxVel = maxVel;
        this.controller = controller;
        this.usingCustomController = true;
    }
    public Mechanism(double minPos, double maxPos, double maxVel){
        this(minPos, maxPos, maxVel, null);
        this.usingCustomController = false;
    }

    public abstract void setPos(double pos, double maxVel);
    public void setPos(double pos){
        setPos(pos, maxVel);
    }
    public abstract double getPos();
    public abstract double getVel();
}
