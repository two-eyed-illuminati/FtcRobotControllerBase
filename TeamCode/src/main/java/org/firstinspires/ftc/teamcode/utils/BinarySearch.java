package org.firstinspires.ftc.teamcode.utils;

import java.util.function.Predicate;

public class BinarySearch {
    public static double binarySearch(double lowerBound, double upperBound, Predicate<Double> condition) {
        double left = lowerBound;
        double right = upperBound;

        do{
            double mid = (left + right) / 2.0;
            if (condition.test(mid)) {
                right = mid;
            } else {
                left = mid;
            }
        }
        while (Math.abs(right - left) > 1e-2);

        return (left + right) / 2.0;
    }
}
