package org.firstinspires.ftc.teamcode.auto;

import com.pedropathing.math.Pose;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Desktop entry point for exporting the example autonomous without a robot.
 */
public final class ExportExampleAuto {
    private ExportExampleAuto() {}

    public static void main(String[] args) throws Exception {
        Path output = Paths.get(
                args.length == 0 ? "TeamCode/build/exports/example-auto.pp" : args[0]);

        ExampleAutoBuilder auto = new ExampleAutoBuilder()
                .startAt(pose(24, 24, 0))
                .goToMark(ExampleAutoBuilder.Mark.LEFT)
                .parkAt(pose(72, 48, 90));

        if (output.getParent() != null) Files.createDirectories(output.getParent());
        Files.write(output, auto.buildPp().getBytes(StandardCharsets.UTF_8));
        System.out.println("Wrote " + output.toAbsolutePath());
    }

    private static Pose pose(double x, double y, double headingDegrees) {
        return new Pose(x, y, Math.toRadians(headingDegrees));
    }
}
