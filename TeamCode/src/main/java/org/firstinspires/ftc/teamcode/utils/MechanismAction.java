package org.firstinspires.ftc.teamcode.utils;

import androidx.annotation.NonNull;

import com.acmerobotics.dashboard.telemetry.TelemetryPacket;
import com.acmerobotics.roadrunner.Action;

public class MechanismAction implements Action {
    public enum Mode{
        GO_GREATER,
        GO_TO,
        GO_LESS
    }
    private Mode mode;
    private final Mechanism mechanism;
    private double targetPos;
    private double maxVel;
    private double margin;

    public MechanismAction(Mechanism mechanism, double targetPos, double maxVel, Mode mode, double margin){
        this.mode = Mode.GO_TO;
        this.mechanism = mechanism;
        this.targetPos = targetPos;
        this.maxVel = maxVel;
        this.margin = margin;
    }

    public MechanismAction(Mechanism mechanism){
        this.mode = Mode.GO_TO;
        this.mechanism = mechanism;
        this.targetPos = mechanism.getPos();
        this.maxVel = mechanism.maxVel;
        this.margin = mechanism.maxVel * 0.05;
    }

    public MechanismAction setMode(Mode mode){
        this.mode = mode;
        return this;
    }
    public MechanismAction setTargetPos(double targetPos){
        this.targetPos = targetPos;
        return this;
    }
    public MechanismAction setMaxVel(double maxVel){
        this.maxVel = maxVel;
        return this;
    }
    public MechanismAction setMargin(double margin){
        this.margin = margin;
        return this;
    }

    public boolean running(){
        if(mode == Mode.GO_TO) {
            return Math.abs(mechanism.getPos() - targetPos) > margin;
        }
        if(mode == Mode.GO_GREATER){
            return mechanism.getPos() < targetPos;
        }
        if(mode == Mode.GO_LESS){
            return mechanism.getPos() > targetPos;
        }
        return false;
    }

    @Override
    public boolean run(@NonNull TelemetryPacket packet){
        if(mode == Mode.GO_TO){
            mechanism.setPos(targetPos, maxVel);
        }
        if(mode == Mode.GO_GREATER){
            mechanism.setPos(mechanism.maxPos, maxVel);
        }
        if(mode == Mode.GO_LESS){
            mechanism.setPos(mechanism.minPos, maxVel);
        }
        return running();
    }
}
