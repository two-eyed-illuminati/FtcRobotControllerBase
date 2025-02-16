package org.firstinspires.ftc.teamcode.utils;

public class Clamp {
    public static double clamp(double val, double min, double max){
        return Math.max(min, Math.min(max, val));
    }
    public static int clamp(int val, int min, int max){
        return Math.max(min, Math.min(max, val));
    }
    public static float clamp(float val, float min, float max){
        return Math.max(min, Math.min(max, val));
    }
    public static long clamp(long val, long min, long max){
        return Math.max(min, Math.min(max, val));
    }
}
