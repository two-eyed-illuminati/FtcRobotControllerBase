package org.firstinspires.ftc.teamcode.utils.mechanism;

import com.pedropathing.ivy.Command;
import com.pedropathing.ivy.behaviors.BlockedBehavior;
import com.pedropathing.ivy.behaviors.ConflictBehavior;
import com.pedropathing.ivy.behaviors.EndCondition;
import com.pedropathing.ivy.behaviors.InterruptedBehavior;

import java.util.Collections;
import java.util.Set;

public class MechanismCommand implements Command {
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

    public MechanismCommand(Mechanism mechanism, double targetPos, double maxVel, Mode mode, double margin){
        this.mode = mode;
        this.mechanism = mechanism;
        this.targetPos = targetPos;
        this.maxVel = maxVel;
        this.margin = margin;
    }

    public MechanismCommand(Mechanism mechanism){
        this.mode = Mode.GO_TO;
        this.mechanism = mechanism;
        this.targetPos = mechanism.getPos();
        this.maxVel = mechanism.maxVel;
        this.margin = mechanism.maxVel * 0.05;
    }

    public MechanismCommand setMode(Mode mode){
        this.mode = mode;
        return this;
    }
    public MechanismCommand setTargetPos(double targetPos){
        this.targetPos = targetPos;
        return this;
    }
    public MechanismCommand setMaxVel(double maxVel){
        this.maxVel = maxVel;
        return this;
    }
    public MechanismCommand setMargin(double margin){
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
    public Set<Object> requirements() {
        return Collections.emptySet();
    }

    @Override
    public int priority() {
        return 0;
    }

    @Override
    public InterruptedBehavior interruptedBehavior() {
        return InterruptedBehavior.END;
    }

    @Override
    public ConflictBehavior conflictBehavior() {
        return ConflictBehavior.CANCEL;
    }

    @Override
    public BlockedBehavior blockedBehavior() {
        return BlockedBehavior.CANCEL;
    }

    @Override
    public void start() {
    }

    @Override
    public boolean done() {
        return !running();
    }

    @Override
    public void execute(){
        if(mode == Mode.GO_TO){
            mechanism.setPos(targetPos, maxVel);
        }
        if(mode == Mode.GO_GREATER){
            mechanism.setPos(mechanism.maxPos, maxVel);
        }
        if(mode == Mode.GO_LESS){
            mechanism.setPos(mechanism.minPos, maxVel);
        }
    }

    @Override
    public void end(EndCondition condition) {
    }
}
