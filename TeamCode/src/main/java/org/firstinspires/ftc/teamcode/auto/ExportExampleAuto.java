package org.firstinspires.ftc.teamcode.auto;

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

        ExampleAutoBuilder auto = ExampleAuto.createRoutine(null);

        if (output.getParent() != null) Files.createDirectories(output.getParent());
        Files.write(output, auto.buildPp().getBytes(StandardCharsets.UTF_8));
        System.out.println("Wrote " + output.toAbsolutePath());
    }
}
