package org.firstinspires.ftc.teamcode.auto.utils;

import com.pedropathing.follower.Follower;

import org.firstinspires.ftc.teamcode.auto.utils.sim.PathSimulator;
import org.firstinspires.ftc.teamcode.auto.utils.sim.SimResult;
import org.firstinspires.ftc.teamcode.auto.utils.sim.SimRobot;
import org.firstinspires.ftc.teamcode.utils.pedro.Constants;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

// ANDROID_HOME="$HOME/Library/Android/sdk" ./gradlew :TeamCode:exportAutoPath --continuous
// Optional arguments: <output .pp> <class in the auto package, default ExampleAuto>
public final class ExportAutoPath {
    private static final String AUTO_PACKAGE = "org.firstinspires.ftc.teamcode.auto";

    private ExportAutoPath() {}

    public static void main(String[] args) throws Exception {
        File output = new File(
                args.length == 0 ? "TeamCode/build/exports/auto-path.pp" : args[0]);

        String autoName = args.length > 1 ? args[1] : "ExampleAuto";
        System.out.println("Exporting " + AUTO_PACKAGE + "." + autoName);
        export(createRoutine(autoName), output);
    }

    private static AutoBuilder createRoutine(String autoName) throws Exception {
        Class<?> auto = Class.forName(AUTO_PACKAGE + "." + autoName);
        Method factory = auto.getMethod("createRoutine", Follower.class);
        try {
            return (AutoBuilder) factory.invoke(null, (Object) null);
        } catch (InvocationTargetException e) {
            throw e.getCause() instanceof Exception ? (Exception) e.getCause() : e;
        }
    }

    public static void export(AutoBuilder auto, File output) throws Exception {
        File directory = output.getAbsoluteFile().getParentFile();
        if (!directory.isDirectory() && !directory.mkdirs()) {
            throw new IOException("Could not create " + directory);
        }
        writeSim(auto, new File(output.getPath() + ".sim.json"));
        write(output, auto.buildPp());
        System.out.println("Wrote " + output.getAbsolutePath());
    }

    private static void write(File file, String text) throws IOException {
        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write(text.getBytes(StandardCharsets.UTF_8));
        }
    }

    private static void writeSim(AutoBuilder auto, File simOutput) throws Exception {
        String json;
        try {
            SimRobot robot = SimRobot.create(Constants.foresightConfig);
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
        write(simOutput, json);
    }
}
