# biobuzz → base merge candidates

Temporary planning document. Delete it when the merge is done.

Source: `~/Documents/coding/biobuzz` (remote `two-eyed-illuminati/biobuzz`).
Common ancestor: `3925c41 Add Field Centric Drive`. biobuzz has 5 commits after it, plus uncommitted work.

## Committed changes (3925c41..HEAD)

| # | File | Change | Generic or robot-specific | Recommendation |
|---|---|---|---|---|
| 1 | `TeleOp/TeleOpMode.java` | Calls `Robot.telemetry.update()` every loop (commit "Update telemetry in loop") | Generic | **Merge** |
| 2 | `utils/Robot.java` | `follower != null` guards in `init` and `loop` (commit "Allow code to run even w/o follower") | Generic | **Merge** |
| 3 | `utils/Robot.java` | `pollenShooter` field and `hardwareMap.get(..., "pollenShooter")` | Robot-specific | Skip |
| 4 | `TeleOp/testing/TestPollenShooter.java` | New OpMode for the `pollenShooter` motor | Robot-specific | Skip |
| 5 | `utils/pedro/Constants.java` | Real `Follower` (Pinpoint, Mecanum, Foresight). Hardware names, pod offsets, tuned numbers | Robot-specific values, generic structure | Decide (see questions) |
| 6 | `utils/pedro/Tuning.java` | Four `@Tuner` methods: mecanum, pinpoint, foresight, tests | Generic, but uses `Constants.localizerConfig` and `Constants.drivetrainConfig` | Decide. Needs #5 to compile |

The `utils/pedro/procedures/` files are identical in both repos. Nothing to merge there.

## Uncommitted changes in biobuzz

| File | Change | Recommendation |
|---|---|---|
| `TeamCode/build.gradle` | Adds RoadRunner `ftc:0.1.25` and `core:1.0.1` | Skip. The base uses Pedro |
| `utils/rr/**`, `auto/testing/RoadRunnerLineTest.java` | RoadRunner drive, localizer, tuning OpModes | Skip. Same reason |
| `auto/ExampleAuto.java` | Team-specific start pose and a `through(...)` path. Adds an unused `path` import | Skip. Base keeps its generic `goToCenter` example |
| `utils/Robot.java` | Comments out the `pollenShooter` lines from commit #3 | Skip. Follows #3 |

## Questions

1. Should the base get a `Constants` with real Pedro configs (#5) and `Tuning` (#6)?
   - **Template option:** add the config fields with placeholder values, so `Tuning` compiles and the structure is reusable.
   - **Copy option:** copy biobuzz's tuned values as they are.
   - **Skip option:** leave both out of the base.
2. Do you want the merge as commits (cherry-pick of "Update telemetry in loop" and "Allow code to run even w/o follower"), or as manual edits that you commit yourself?

## Q&A log

- Q1 answer: add the tuners, but change nothing in `Constants`.
  - `MecanumTuner` and `PinpointTuner` have no-argument constructors, so they are added to `Tuning`.
  - `ForesightTuner` and `Tests` need `Constants.localizerConfig` and `Constants.drivetrainConfig`. They are NOT added until `Constants` has those fields.
- Q2 answer: cherry-pick. `8a22894` and `987623a` are cherry-picked with `-x` (now `ec9561c` and `3ca5d76`). `Tuning.java` stays uncommitted.
