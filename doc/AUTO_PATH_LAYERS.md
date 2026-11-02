# Auto path layers (CODING-9)

Goal: tune one path in one OpMode without adding a parameter to a shared function.

## Problem

Reference: [two-eyed-illuminati/FTC-DECODE-2025-2026](https://github.com/two-eyed-illuminati/FTC-DECODE-2025-2026), `Auto/AutoBuilder.java`.

- One large `AutoBuilder` has named step methods (`goToCloseShoot`, `goToSpike1`, `intakeFromGate`, ...).
- Each step owns its geometry (global `@Config` statics), its side effects (intake, turret), and its path.
- Variants use string flags: `goToCloseShoot(type, tangentType, posType)` (RoadRunner original) with `"1"`, `"last"`, `"avoid1"`.
- Steps read the previous step's name from a string list (`actions.get(size-1).equals("GoToShoot")`).
- One special case means a new constant, a new flag, a new branch, and edits to every caller.

## Pedro 3.0.1 facts (checked in the jar)

- `Path` is built at call time. `line`/`curve`/`through` build the curve at once.
- `.linear()`, `.tangent()`, `.with(...)` return a new `Path`. They never edit the old one.
- The curve (end pose, control points) has no setters. To change geometry, build a new path.
- `PathChain` no longer exists. `path(Path...)` makes a `CompoundPath`.
- Heading is an `Interpolator`: `tangent`, `reverse`, `constant`, `linear`, `longLinear`, `facingPoint`, `piecewise`.
- Per-path settings: `path.with(Modifier...)`. `ConfigVar.at(value)` makes a `Modifier`. Foresight has about 35 config vars (`maxPathSpeed`, `brakeAtEnd`, `pathSkip`, tolerances, ...).
- Mirroring is built in: `PoseFactory.mirrorX/mirrorY/mirrorAroundPoint`.
- Pose API: `x()`, `y()`, `heading()` (radians).

## Three layers

Each layer edits the same `Step` object. Order: global, then context, then tweaks. The last layer wins.

| Layer | Question | Edited by | Lives in |
|---|---|---|---|
| 1. Global | What is a "close shoot" in general? | Rarely | Default `Step` of each named step method |
| 2. Context | What came before? Does it change the path? | When a routine pattern changes | Rules on typed history (previous step, pose, shot count) |
| 3. Tweaks | One OpMode needs a fix now | At competition | Call site: `.goToCloseShoot(s -> s.endAt(...))` and `.nudge(dx, dy, dHeading)` |

Rules for the layers:

- Poses are `Pose` objects named by place (`CLOSE_SHOOT_LAST`, `GATE_LEFT_HIT`). A one-off tune passes an inline `Pose`.
- Context rules replace the `actions.get(...)` string checks.
- A tweak never changes shared defaults.
- Log the final `Step` and the layer that set each changed field.
- Later option: live tuning through Panels, only if rebuild time at competition hurts.

## Why `Step` exists

A built `Path` cannot be edited, so layers must edit the inputs. `Step` is the Pedro builder call stored as data, run once at the end. It also holds the commands that run with the path (intake, turret).

### Names

| `Step` | Pedro |
|---|---|
| `Step` | `Path` from `path(Path...)` (`CompoundPath`) plus Ivy commands |
| `Part` | `AtomicPath` (one curve, one heading rule, modifiers) |
| `.line(end)` | `line(start, end)` |
| `.curve(c1, ..., end)` | `curve(start, c1, ..., end)` |
| `.through(a, b, ...)` | `through(start, a, b, ...)` |
| `.custom(curve)` | `path(curve)` |
| `HeadingRule` | `Interpolator` (a rule gets `from` and `end` at build time) |
| `.heading(HeadingRule.linear())` | `.linear(start, end)` |
| `.set(a -> a.maxPathSpeed, v)` | `.with(foresight.maxPathSpeed.at(v))` |
| `.alongside(cmd)` / `.then(cmd)` | Ivy `parallel` / `sequential` around `follow(follower, path)` |

Two differences from Pedro: no `start` argument (the builder tracks `currentPose`), and calls run later in `build(...)`.

### Schema

```java
public final class Step {
  final List<Part> parts;
  HeadingRule heading;                              // whole path; null keeps each part's own
  final List<Function<Follower, Modifier>> settings; // skipped when follower is null (export)
  final List<Command> alongside, after;
}

final class Part {
  enum Shape { LINE, BEZIER, THROUGH, CURVE }
  Shape shape;
  List<Pose> points;   // controls, then the end; start comes from the previous part
  Curve curve;         // CURVE only
  HeadingRule heading; // default tangent
  List<Function<Follower, Modifier>> settings;
}
```

`build(step, from, mirror, follower)` makes each part, applies heading and settings, joins parts with `path(...)`, then wraps with `follow` and the commands. Mirror (`PoseFactory.Operation`) applies once there; every pose in `Step` is in the blue frame.

### Verification status

1. Done: `line(Pose, Pose)` and `curve(Pose...)` use only x and y of each pose.
2. Open: meaning of the third argument in `linear(a, b, double)`. `Step` uses the two-argument form.
3. Partly done: `Paths.path(Path...)` makes a `CompoundPath` with the right segments and `endPose()`. Each part keeps its own heading rule. Robot behavior (no braking between parts) is not checked.
4. Open: the algorithm is `Foresight` (`set(...)` casts to it). `Step.with(...)` is skipped when the follower is null.
5. Open: units of `maxPathSpeed`. Speed caps are not ported.
6. Done: `PoseFactory.radians().mirrorX(72)` gives (144 - x, y, pi - heading). `Mirror` wraps it.

## Milestones

1. [x] This document.
2. [x] Port two DECODE autos with the DECODE `AutoBuilder` structure (`autocurrent/`).
3. [x] Implement `Step` (`autostep/`). Deleted later.
4. [x] Re-implement the same two autos with `Step`. Compare against milestone 2. Deleted with `autostep/`.
5. [x] Alliance mirroring through `PoseFactory`. Deleted with `autostep/`.
6. [x] Try the Options approach (`autonew/`).

## Milestone 2: what was ported

The code is in `autocurrent/`, a copy of `auto/`. The `auto/` package is unchanged. `autocurrent/utils/AutoBuilder` follows the structure of the DECODE `AutoBuilder`: named step methods, an `actions` history, and public static constants. It is the baseline that `Step` must replace.

| This repo | Source (DECODE repo) |
|---|---|
| `autocurrent/AutoGoalZone15B2SM4GHSM1First` | `AutoGoalZone15B2SM4GHSM1First` (same call chain) |
| `autocurrent/AutoFarZone9B1SM0GH` | `AutoFarZone9B1SM0GH` (same call chain) |
| `autocurrent/utils/AutoBuilder` | `AutoBuilder`, only the steps that the two autos call |
| `autocurrent/utils/RobotStubs` | `Robot` mechanism calls. This repo's `Robot` has no mechanisms, so each call logs. |

Check a port visually: `ANDROID_HOME="$HOME/Library/Android/sdk" ./gradlew :TeamCode:exportAutoPath -PppMain=org.firstinspires.ftc.teamcode.autocurrent.utils.ExportAutoPath -PppRoutine=org.firstinspires.ftc.teamcode.autocurrent.AutoGoalZone15B2SM4GHSM1First` (open the `.pp` in the Pedro visualizer).

### Port decisions (unverified on a robot)

- **Constants.** `autocurrent` has no RoadRunner values or conversions. Every constant is a Pedro `Pose` for blue (for example `SPIKE_CLOSE_SHOOT = new Pose(56.0003..., 84.3370..., -180 deg)`, `SPIKE_1`). A place that only gives a position uses a `Pose` without heading. They were converted once from the DECODE values with Pedro `(x, y, h) = (72 + y, 72 - x, h - 90 deg)`. This assumes blue on the left and the audience at the bottom. Verify in the visualizer. `autonew/` uses the same constants.
- **No alliance handling.** The code is blue only. `AutoOpMode` no longer sets or asks for the alliance. Red needs mirroring added later (Pedro: `(x, y, h) -> (144 - x, y, pi - h)`).
- **Pedro and Ivy calls, not RoadRunner ones.** The builder has no tangent state, no `strafeTo`/`splineTo` wrappers and no tab machinery. Steps call `line(from, to)` with `.linear(...)` or `.constant(...)` directly; only `drive(Path)` wraps them (records the path for the `.pp` export, tracks `currentPose`, follows it). A bend is `curve(from, control, to)` with one control point (the control point leaves along y and arrives along x). Markers (`afterTime(0, ...)` in the source) are `parallel(move, marker)`. Commands that come after the move are in `sequential(...)`. Waits are `waitMs`.
- **Changes caused by that.** Only `goToSpike2`, `goToSpike3`, `goToGateHit` after a shoot, and `intakeFromGate` after a shoot are curves. All other former splines are lines. The RoadRunner-only arguments (`tangentType`, the `"preload"` type of `goToSpike1`) are removed, and the `"strafe"`/`"spline"` types are now `Shape.LINE`/`Shape.CURVE`. The goal is similar structure, not the same call chains as the DECODE autos. The old `previousAction()` rules that set a start tangent now choose between a curve and a line.
- **Order.** Each step adds one command, so steps always run in call order.
- **Dropped.** Steps that neither auto calls (`goInsideZone`, `backUpAfterSpike1`, `backUpAfterSpike3`, `backUpToAvoidSpike1` as a step, `goToSpike3()` with no argument, `shoot(double)`). All velocity constraints (needs the `maxPathSpeed` units). `looseIntake`. Turret and outtake behavior (stubs). `shoot()` with no argument uses 1.2 s as a stand-in.
- **Corner intake.** Kept the original 0.01 inch turn move. A move that short may behave differently in Pedro.

## Options approach: `autonew/`

The `Step` approach (`autostep/`) was tried and deleted. `autonew/` tries a simpler one: each step has an `Options` class with plain fields, written inside `AutoBuilder` right before its step. Package `org.firstinspires.ftc.teamcode.autonew`. It starts as a copy of `autocurrent/`.

| File | Job |
| --- | --- |
| `utils/AutoBuilder` | For each step: the constants, a nested `...Options` class (`GoToSpike3Options`, `GoToGateHitOptions`, ...) with `Pose` fields, a method with flags or no argument (defaults, then context `if`s) and an overload that takes the Options (layer 3). The field initializers or a factory hold the global defaults (layer 1). |
| `AutoGoalZone15B2SM4GHSM1First`, `AutoFarZone9B1SM0GH` | The two autos, with `(new)` in the OpMode names |

### The three layers in code

```java
public AutoBuilder goToSpike3(String zone) {        // layer 1 + 2
  GoToSpike3Options ops = new GoToSpike3Options();         // layer 1: defaults from the shared constants
  if (zone.equals("far")) ops.pose = ops.pose.withY(ops.pose.y() - 3.0);  // layer 2: flag or context, a plain if
  return goToSpike3(ops);
}

public AutoBuilder goToSpike3(GoToSpike3Options ops) {    // builds the path from ops
  ...
}
```

Layer 3 (emergency tweak) goes at the top of the auto file, all together:

```java
private static final AutoBuilder.GoToGateHitOptions GATE_RIGHT = AutoBuilder.GoToGateHitOptions.right();
static {
  GATE_RIGHT.beforeHit = GATE_RIGHT.beforeHit.withY(63);
  GATE_RIGHT.hit = GATE_RIGHT.hit.withY(63);
}
...
.goToGateHit(GATE_RIGHT)
```

### Decisions

- **A tweak replaces everything.** The Options overload skips the context rules. A tweak starts from the global defaults, so it must set again any value that context changed. Example: `goToGateHit` after a shoot is a curve by context; a tweak that sets only a pose makes it a line, unless it also sets `approach = AutoBuilder.Shape.CURVE`.
- **Shape enum.** `AutoBuilder.Shape` (`LINE` or `CURVE`) is a field of `GoToGateHitOptions`, `IntakeFromGateOptions` (set by context) and `GoToCloseShootOptions`, `GoToFarShootOptions` (set by the `type` flag). `goToSpike2` and `goToSpike3` are always curves; other steps are always lines.
- **Defaults are read when the Options object is created.** A tweak declared as a `static final` reads the `AutoBuilder` constants at class load, so later changes to the constants do not reach it.
- **Flags that mean something stay.** `goToCloseShoot(Shape.LINE, "1")`, `goToGateHit("left")`, `goToSpike3("far")`. A flag chooses the starting Options (layer 1). The `Shape` argument sets `GoToCloseShootOptions.shape` and `GoToFarShootOptions.shape`; `"avoid1"` sets `viaBackUp`.
- **History names** are unchanged (`GoToShoot`, `Shoot`, ...).

### Check

Both autos export the same `.pp` files as `autocurrent` (equal within 3e-14). Command: `ANDROID_HOME="$HOME/Library/Android/sdk" ./gradlew :TeamCode:exportAutoPath -PppMain=org.firstinspires.ftc.teamcode.autonew.utils.ExportAutoPath -PppRoutine=org.firstinspires.ftc.teamcode.autonew.AutoGoalZone15B2SM4GHSM1First`. The builders have no logging. The tweak overloads were tested once before the switch to Pedro constants and not rerun since; the new `Pose`-based Options were only compiled and checked through the default (non-tweak) path. Nothing was run on a robot or in the visualizer.

### Not ported (same as `autocurrent`)

Velocity caps and `looseIntake`.
