package org.firstinspires.ftc.teamcode.auto;

import static com.pedropathing.api.Paths.curve;
import static com.pedropathing.api.Paths.line;
import static com.pedropathing.ivy.groups.Groups.sequential;
import static com.pedropathing.ivy.pedro.PedroCommands.follow;

import com.pedropathing.follower.Follower;
import com.pedropathing.ivy.Command;
import com.pedropathing.math.Pose;
import com.pedropathing.paths.Path;

import java.util.ArrayList;
import java.util.List;

/**
 * A small example of a fluent, high-level autonomous builder.
 *
 * <p>The builder only assembles normal Ivy commands and Pedro paths. It does not replace either
 * library, and {@link #then(Command)} remains available as an escape hatch for commands that do not
 * warrant a high-level builder method.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * Command auto = new ExampleAutoBuilder(follower)
 *         .startAt(START)
 *         .goToMark(Mark.LEFT)
 *         .scorePreload(scoreCommand)
 *         .parkAt(PARK)
 *         .build();
 * }</pre>
 */
public final class ExampleAutoBuilder {
    public enum Mark {
        LEFT,
        CENTER,
        RIGHT
    }

    /*
     * Example field coordinates. A real autonomous should move these into its field/pose catalog so
     * that all routines share the same source of truth.
     */
    private static final Pose LEFT_MARK_APPROACH =
            degreesPose(42, 42, 135);
    private static final Pose LEFT_MARK =
            degreesPose(48, 48, 135);
    private static final Pose CENTER_MARK_APPROACH =
            degreesPose(48, 36, 90);
    private static final Pose CENTER_MARK =
            degreesPose(54, 42, 90);
    private static final Pose RIGHT_MARK_APPROACH =
            degreesPose(54, 30, 45);
    private static final Pose RIGHT_MARK =
            degreesPose(60, 36, 45);

    private final Follower follower;
    private final List<Command> commands = new ArrayList<>();
    private final PpExporter pp = new PpExporter();

    private Pose currentPose;

    public ExampleAutoBuilder(Follower follower) {
        this.follower = follower;
    }

    /**
     * Defines the localization pose used by the autonomous.
     */
    public ExampleAutoBuilder startAt(Pose startPose) {
        currentPose = startPose;
        follower.setPose(startPose);
        pp.startAt(startPose);
        return this;
    }

    /**
     * Drives to a selected mark through an approach pose.
     *
     * <p>This is intentionally high-level: one builder call expands into a curved approach followed
     * by a straight final alignment.
     */
    public ExampleAutoBuilder goToMark(Mark mark) {
        Pose control;
        Pose approach;
        Pose target;

        switch (mark) {
            case LEFT:
                control = degreesPose(32, 36, 0);
                approach = LEFT_MARK_APPROACH;
                target = LEFT_MARK;
                break;
            case CENTER:
                control = degreesPose(38, 30, 0);
                approach = CENTER_MARK_APPROACH;
                target = CENTER_MARK;
                break;
            default:
                control = degreesPose(44, 24, 0);
                approach = RIGHT_MARK_APPROACH;
                target = RIGHT_MARK;
                break;
        }

        Pose start = currentPose;
        Path approachPath = curve(start, control, approach).linear(start, approach);
        Path alignPath = line(approach, target).linear(approach, target);

        followPath(approachPath);
        followPath(alignPath);
        return this;
    }

    /**
     * Adds the robot-specific preload command at the semantic point where scoring should occur.
     */
    public ExampleAutoBuilder scorePreload(Command scoreCommand) {
        return then(scoreCommand);
    }

    /**
     * Follows a straight path from the builder's current pose to a parking pose.
     */
    public ExampleAutoBuilder parkAt(Pose parkPose) {
        Pose start = currentPose;
        Path park = line(start, parkPose).linear(start, parkPose);
        followPath(park);
        return this;
    }

    /**
     * Adds any ordinary Ivy command without requiring a dedicated builder method.
     */
    public ExampleAutoBuilder then(Command command) {
        commands.add(command);
        return this;
    }

    /**
     * Produces the Ivy command that executes all accumulated operations in order.
     */
    public Command build() {
        return sequential(commands.toArray(new Command[0]));
    }

    public String buildPp() {
        return pp.build();
    }

    private void followPath(Path path) {
        commands.add(follow(follower, path));
        pp.add(path);
        currentPose = path.endPose();
    }

    private static Pose degreesPose(double x, double y, double headingDegrees) {
        return new Pose(x, y, Math.toRadians(headingDegrees));
    }
}
