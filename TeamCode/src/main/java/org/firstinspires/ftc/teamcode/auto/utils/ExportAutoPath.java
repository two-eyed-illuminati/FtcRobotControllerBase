package org.firstinspires.ftc.teamcode.auto.utils;

import org.firstinspires.ftc.teamcode.auto.ExampleAuto;
import org.firstinspires.ftc.teamcode.auto.utils.sim.PathSimulator;
import org.firstinspires.ftc.teamcode.auto.utils.sim.SimResult;
import org.firstinspires.ftc.teamcode.auto.utils.sim.SimRobot;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

// ANDROID_HOME="$HOME/Library/Android/sdk" ./gradlew :TeamCode:exportAutoPath --continuous
public final class ExportAutoPath {
    private ExportAutoPath() {}

    public static void main(String[] args) throws Exception {
        Path output = Paths.get(
                args.length == 0 ? "TeamCode/build/exports/auto-path.pp" : args[0]);

        export(ExampleAuto.createRoutine(null), output);
    }

    public static void export(AutoBuilder auto, Path output) throws Exception {
        if (output.getParent() != null) Files.createDirectories(output.getParent());
        writeSim(auto, Paths.get(output + ".sim.json"));
        Files.write(output, auto.buildPp().getBytes(StandardCharsets.UTF_8));
        System.out.println("Wrote " + output.toAbsolutePath());
    }

    private static void writeSim(AutoBuilder auto, Path simOutput) throws Exception {
        String json;
        try {
            SimRobot robot = SimRobot.create();
            long started = System.nanoTime();
            SimResult result = new PathSimulator(robot).simulate(auto.startPose, auto.paths);
            System.out.printf("Simulated %d paths: %.2f s, %d samples, max %.1f in/s, end speed %.2f in/s,"
                            + " end error %.2f in / %.2f deg, %d warnings, in %d ms%n",
                    auto.paths.size(), result.duration(), result.sampleCount(), result.maxSpeed(),
                    result.endSpeed(), result.endPositionError(),
                    Math.toDegrees(result.endHeadingError()), result.warnings().size(),
                    (System.nanoTime() - started) / 1_000_000);
            for (String warning : result.warnings()) System.out.println("Sim warning: " + warning);
            json = result.toJson();
        } catch (RuntimeException e) {
            e.printStackTrace();
            json = SimResult.failureJson("Simulation failed: " + e);
        }
        Files.write(simOutput, json.getBytes(StandardCharsets.UTF_8));
    }
}
