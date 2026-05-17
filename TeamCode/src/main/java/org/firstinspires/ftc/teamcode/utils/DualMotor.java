package org.firstinspires.ftc.teamcode.utils;

import com.qualcomm.hardware.bosch.BNO055IMU;
import com.qualcomm.robotcore.hardware.DcMotorController;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.PIDCoefficients;
import com.qualcomm.robotcore.hardware.PIDFCoefficients;
import com.qualcomm.robotcore.hardware.configuration.typecontainers.MotorConfigurationType;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.CurrentUnit;

public class DualMotor implements DcMotorEx{
    public DcMotorEx motor1;
    public DcMotorEx motor2;

    public DualMotor(DcMotorEx motor1, DcMotorEx motor2){
        this.motor1 = motor1;
        this.motor2 = motor2;
    }
    public void setMotorEnable(){
        motor1.setMotorEnable();
        motor2.setMotorEnable();
    }
    public void setMotorDisable(){
        motor1.setMotorDisable();
        motor2.setMotorDisable();
    }
    public boolean isMotorEnabled(){
        return motor1.isMotorEnabled() && motor2.isMotorEnabled();
    }
    public void setVelocity(double angularRate){
        motor1.setVelocity(angularRate);
        motor2.setVelocity(angularRate);
    }
    public void setVelocity(double angularRate, AngleUnit unit){
        motor1.setVelocity(angularRate, unit);
        motor2.setVelocity(angularRate, unit);
    }
    public double getVelocity(){
        return (motor1.getVelocity() + motor2.getVelocity())/2;
    }
    public double getVelocity(AngleUnit unit){
        return (motor1.getVelocity(unit) + motor2.getVelocity(unit))/2;
    }
    public void setPIDCoefficients(RunMode mode, PIDCoefficients pidCoefficients){
        motor1.setPIDCoefficients(mode, pidCoefficients);
        motor2.setPIDCoefficients(mode, pidCoefficients);
    }
    public void setPIDFCoefficients(RunMode mode, PIDFCoefficients pidfCoefficients){
        motor1.setPIDFCoefficients(mode, pidfCoefficients);
        motor2.setPIDFCoefficients(mode, pidfCoefficients);
    }
    public void setVelocityPIDFCoefficients(double p, double i, double d, double f){
        motor1.setVelocityPIDFCoefficients(p, i, d, f);
        motor2.setVelocityPIDFCoefficients(p, i, d, f);
    }
    public void setPositionPIDFCoefficients(double p){
        motor1.setPositionPIDFCoefficients(p);
        motor2.setPositionPIDFCoefficients(p);
    }
    public PIDCoefficients getPIDCoefficients(RunMode mode){
        return motor1.getPIDCoefficients(mode);
    }
    public PIDFCoefficients getPIDFCoefficients(RunMode mode){
        return motor1.getPIDFCoefficients(mode);
    }
    public void setTargetPositionTolerance(int tolerance){
        motor1.setTargetPositionTolerance(tolerance);
        motor2.setTargetPositionTolerance(tolerance);
    }
    public int getTargetPositionTolerance(){
        return motor1.getTargetPositionTolerance();
    }
    public double getCurrent(CurrentUnit unit){
        return motor1.getCurrent(unit) + motor2.getCurrent(unit);
    }
    public double getCurrentAlert(CurrentUnit unit){
        return motor1.getCurrentAlert(unit) + motor2.getCurrentAlert(unit);
    }
    public void setCurrentAlert(double current, CurrentUnit unit){
        motor1.setCurrentAlert(current, unit);
        motor2.setCurrentAlert(current, unit);
    }
    public boolean isOverCurrent(){
        return motor1.isOverCurrent() || motor2.isOverCurrent();
    }
    public MotorConfigurationType getMotorType(){
        return motor1.getMotorType();
    }
    public void setMotorType(MotorConfigurationType motorType){
        motor1.setMotorType(motorType);
        motor2.setMotorType(motorType);
    }
    public DcMotorController getController(){
        return motor1.getController();
    }
    public int getPortNumber() {
        return motor1.getPortNumber();
    }
    public void setZeroPowerBehavior(ZeroPowerBehavior zeroPowerBehavior){
        motor1.setZeroPowerBehavior(zeroPowerBehavior);
        motor2.setZeroPowerBehavior(zeroPowerBehavior);
    }
    public ZeroPowerBehavior getZeroPowerBehavior() {
        return motor1.getZeroPowerBehavior();
    }
    public void setPowerFloat(){
        motor1.setPowerFloat();
        motor2.setPowerFloat();
    }
    public boolean getPowerFloat() {
        return motor1.getPowerFloat() && motor2.getPowerFloat();
    }
    public void setTargetPosition(int position){
        motor1.setTargetPosition(position);
        motor2.setTargetPosition(position);
    }
    public int getTargetPosition() {
        return (motor1.getTargetPosition() + motor2.getTargetPosition())/2;
    }
    public boolean isBusy() {
        return motor1.isBusy() || motor2.isBusy();
    }
    public int getCurrentPosition() {
        return (motor1.getCurrentPosition() + motor2.getCurrentPosition())/2;
    }
    public void setMode(RunMode mode){
        motor1.setMode(mode);
        motor2.setMode(mode);
    }
    public RunMode getMode() {
        return motor1.getMode();
    }
    public void setDirection(Direction direction){
        motor1.setDirection(direction);
        motor2.setDirection(direction);
    }
    public Direction getDirection() {
        return motor1.getDirection();
    }
    public void setPower(double power){
        motor1.setPower(power);
        motor2.setPower(power);
    }
    public double getPower() {
        return (motor1.getPower() + motor2.getPower())/2;
    }
    public Manufacturer getManufacturer() {
        return motor1.getManufacturer();
    }
    public String getDeviceName() {
        return motor1.getDeviceName();
    }
    public String getConnectionInfo() {
        return motor1.getConnectionInfo();
    }
    public int getVersion() {
        return motor1.getVersion();
    }
    public void resetDeviceConfigurationForOpMode() {
        motor1.resetDeviceConfigurationForOpMode();
        motor2.resetDeviceConfigurationForOpMode();
    }
    public void close() {
        motor1.close();
        motor2.close();
    }
}
