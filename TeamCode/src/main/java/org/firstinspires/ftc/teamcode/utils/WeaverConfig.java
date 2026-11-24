package org.firstinspires.ftc.teamcode.utils;

import com.skeletonarmy.marrow.weaver.PathConfig;
import com.skeletonarmy.marrow.weaver.Weaver;
import com.skeletonarmy.marrow.zones.CircleZone;
import com.skeletonarmy.marrow.zones.Point;
import com.skeletonarmy.marrow.zones.PolygonZone;
import com.skeletonarmy.marrow.zones.Zone;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Shared Weaver settings and BioBuzz field geometry in inches. */
public final class WeaverConfig {
  private static final double FIELD_SIZE = 141.5;
  private static final double HIVE_SUPPORT_SPACING = 49.46;
  private static final double HIVE_SUPPORT_LENGTH = 38.95;
  private static final double HIVE_SUPPORT_THICKNESS = 2;
  private static final double FLOWER_RADIUS = 3;
  private static final double FLOWER_CENTER_FROM_WALL = 4;

  public static final List<Zone> FIELD_OBSTACLES = Collections.unmodifiableList(Arrays.asList(
    new PolygonZone(new Point(FIELD_SIZE / 2 - HIVE_SUPPORT_SPACING / 2, FIELD_SIZE / 2),
      HIVE_SUPPORT_THICKNESS, HIVE_SUPPORT_LENGTH),
    new PolygonZone(new Point(FIELD_SIZE / 2 + HIVE_SUPPORT_SPACING / 2, FIELD_SIZE / 2),
      HIVE_SUPPORT_THICKNESS, HIVE_SUPPORT_LENGTH),
    new CircleZone(new Point(48, FIELD_SIZE - FLOWER_CENTER_FROM_WALL), FLOWER_RADIUS),
    new CircleZone(new Point(FIELD_SIZE - FLOWER_CENTER_FROM_WALL, 96), FLOWER_RADIUS),
    new CircleZone(new Point(FLOWER_CENTER_FROM_WALL, 48), FLOWER_RADIUS),
    new CircleZone(new Point(96, FLOWER_CENTER_FROM_WALL), FLOWER_RADIUS)
  ));

  private WeaverConfig() {}

  public static void apply() {
    PathConfig config = new PathConfig()
      .intakeWidth(8.0);
    Weaver.setConfig(config);
    System.out.println("WeaverConfig: " + config + ", field obstacles=" + FIELD_OBSTACLES.size());
  }
}
