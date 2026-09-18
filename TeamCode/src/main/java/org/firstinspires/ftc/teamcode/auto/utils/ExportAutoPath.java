package org.firstinspires.ftc.teamcode.auto.utils;

import org.firstinspires.ftc.teamcode.auto.ExampleAuto;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

// ANDROID_HOME="$HOME/Library/Android/sdk" ./gradlew :TeamCode:exportAutoPath --continuous
public final class ExportAutoPath {
    private ExportAutoPath() {}

    public static void main(String[] args) throws Exception {
        Path output = Paths.get(
                args.length == 0 ? "TeamCode/build/exports/example-auto.pp" : args[0]);

        AutoBuilder auto = ExampleAuto.createRoutine(null);

        if (output.getParent() != null) Files.createDirectories(output.getParent());
        Files.write(output, auto.buildPp().getBytes(StandardCharsets.UTF_8));
        System.out.println("Wrote " + output.toAbsolutePath());
    }
}
