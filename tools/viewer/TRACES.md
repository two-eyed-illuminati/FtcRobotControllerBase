# Robot traces

Compare the simulation with what the robot really did.

## Record

Run any `AutoOpMode` auto with a real `Robot.follower`. `FollowerTrace` records the follower every loop and saves on stop:

- `/sdcard/FIRST/traces/<OpModeName>-<millis>.trace.json`
- `/sdcard/FIRST/traces/latest.trace.json`

## Fetch

Layers panel → **Runs** → **Fetch latest run**. The dev server runs `adb pull` for `latest.trace.json` and adds the file as a run labelled `latest HH:MM:SS`. Each press adds one run.

Over Wi-Fi, connect first:

```
adb connect 192.168.43.1:5555
```

Settings (environment variables for `pnpm dev`; the server logs them at start):

- `ADB`: the adb program. Default `adb`.
- `ADB_SERIAL`: the device to use when more than one is connected.
- `TRACE_DIR`: the folder on the hub. Default `/sdcard/FIRST/traces`.

Without the button, pull the file yourself and use **Add run**:

```
adb pull /sdcard/FIRST/traces/latest.trace.json ~/Downloads/
```

`latest.trace.json` is overwritten by every run. The timestamped files keep the history.

## Format

```json
{
  "kind": "trace",
  "version": 1,
  "name": "ExampleAuto",
  "modes": ["FOLLOW", "HOLD", "MANUAL", "IDLE"],
  "samples": {
    "t": [],
    "x": [],
    "y": [],
    "heading": [],
    "vx": [],
    "vy": [],
    "omega": [],
    "closestT": [],
    "mode": [],
    "pathIndex": [],
    "group": []
  }
}
```

`t` is in seconds, `mode` is an index into `modes`, `group` increases each time the follower gets a new path.

## How the viewer reads it

- Time `0` is the first `FOLLOW` sample. Earlier samples are dropped.
- Uneven loop times are resampled to a fixed 10 ms step (linear; heading uses the short way round).
- `speed = hypot(vx, vy)`. Columns the robot does not log (target velocity, motor powers, braking) are `0`.
- A loop longer than 100 ms gives a warning.

## Limits

- Recording stops at 60 000 samples.
- The file is written on stop. A crash before stop loses the trace.
- A run without a follower (`Robot.follower == null`) records nothing.
