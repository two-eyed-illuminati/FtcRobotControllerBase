package org.firstinspires.ftc.teamcode.utils.pedro;

import com.pedropathing.algorithm.ForesightConfig;
import com.pedropathing.controllers.Controller;
import com.pedropathing.follower.Follower;
import com.pedropathing.math.Matrix;
import com.pedropathing.math.Vector2D;
import com.pedropathing.revhub.drivetrains.MecanumConfig;
import com.qualcomm.robotcore.hardware.HardwareMap;

public class Constants {
    public static MecanumConfig drivetrainConfig = new MecanumConfig(c -> {});

    public static ForesightConfig foresightConfig = new ForesightConfig(
            c -> {
                Controller primaryTranslationalForward = Controller.proportional(0.46143747753891484);
                Controller secondaryTranslationalForward = Controller.proportional(0.17048881811414734);
                Controller primaryTranslationalLateral = Controller.proportional(0.7421916068070539);
                Controller secondaryTranslationalLateral = Controller.proportional(0.2742199669902264);

                c.forwardTranslational.set(Controller.piecewise(secondaryTranslationalForward).put(2.5, primaryTranslationalForward));
                c.strafeTranslational.set(Controller.piecewise(secondaryTranslationalLateral).put(2.5, primaryTranslationalLateral));

                c.coast.set(Controller.proportionalFeedforward(0.013828078266724485));
                c.brake.set(Controller.proportionalFeedforward(0.011753866526715812));

                c.headingFeedback.set(Controller.proportional(7.394465419100555));
                c.headingBrakeCoefficients.set(Vector2D.cartesian(0.057710307439896885, 0.007305738388892705));

                c.linearBrakeCoefficients.set(Matrix.diag(0.10604301225944404, 0.07235600480491211));
                c.quadraticBrakeCoefficients.set(Matrix.diag(0.0017051540772801824, 0.0015567358791152273));

                c.maxAchievableForwardVelocity.set(72.37704404328076);
                c.maxAchievableStrafeVelocity.set(54.82465388665514);
                c.naturalForwardDeceleration.set(28.496999108568463);
                c.naturalStrafeDeceleration.set(82.80357292419318);
            }
    );

    public static Follower create(HardwareMap h) {
        // return new Follower(Drivetrain, Localizer, new Foresight(foresightConfig));
        return null;
    }
}
