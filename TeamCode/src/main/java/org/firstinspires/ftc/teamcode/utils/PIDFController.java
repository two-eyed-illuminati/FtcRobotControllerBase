package org.firstinspires.ftc.teamcode.utils;

import com.qualcomm.robotcore.util.ElapsedTime;

public class PIDFController {
    double pCoefficient;
    double dCoefficient;
    double feedforwardCoefficient;
    double oldError = 0;
    ElapsedTime timeSinceOld;
    public PIDFController(double pCoefficient, double dCoefficient, double feedforwardCoefficient){
        this.pCoefficient = pCoefficient;
        this.dCoefficient = dCoefficient;
        this.feedforwardCoefficient = feedforwardCoefficient;
        timeSinceOld = new ElapsedTime();
    }
    public double getPower(double currPos, double targetPos){
        double power = feedforwardCoefficient * targetPos + pCoefficient * (targetPos-currPos) + dCoefficient * (targetPos-currPos - oldError) / timeSinceOld.seconds();
        oldError = targetPos-currPos;
        timeSinceOld.reset();
        return power;
    }
}
