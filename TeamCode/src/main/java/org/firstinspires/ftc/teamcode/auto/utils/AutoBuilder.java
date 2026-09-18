package org.firstinspires.ftc.teamcode.auto.utils;

import static com.pedropathing.ivy.groups.Groups.sequential;
import static com.pedropathing.ivy.pedro.PedroCommands.follow;

import com.pedropathing.follower.Follower;
import com.pedropathing.ivy.Command;
import com.pedropathing.math.Pose;
import com.pedropathing.paths.Path;

import java.util.ArrayList;
import java.util.List;

public final class AutoBuilder {
    private final Follower follower;
    private final List<Command> commands = new ArrayList<>();
    private final PpExporter pp = new PpExporter();

    public AutoBuilder() {
        follower = null;
    }

    public AutoBuilder(Follower follower) {
        this.follower = follower;
    }

    public AutoBuilder startAt(Pose pose) {
        pp.startAt(pose);
        if (follower != null) follower.setPose(pose);
        return this;
    }

    public AutoBuilder followPath(Path path) {
        if (follower != null) commands.add(follow(follower, path));
        pp.add(path);
        return this;
    }

    public AutoBuilder then(Command command) {
        commands.add(command);
        return this;
    }

    public Command build() {
        return sequential(commands.toArray(new Command[0]));
    }

    public String buildPp() {
        return pp.build();
    }
}
