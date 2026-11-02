# Panels capture/replay demo

The project uses [Panels](https://panels.bylazar.com/) for dashboard telemetry. The
`Panels Capture Replay Demo` OpMode generates match-like data without reading the hardware map,
so the capture/replay workflow can be demonstrated without a connected robot.

An Android Robot Controller device is still required to host Panels. It can be a Control Hub or
an Android phone running the Robot Controller app; no motors, sensors, or configured expansion
hub are needed.

## Run the demo

1. Build and install the `TeamCode` app on the Robot Controller device.
2. Connect the viewing computer to the Robot Controller Wi-Fi network.
3. Open Panels:
   - Control Hub: <http://192.168.43.1:8001>
   - Robot Controller phone: <http://192.168.49.1:8001>
4. Select **Panels Capture Replay Demo** from the **Demo** OpMode group.
5. Open the **Capture** widget and click **Start**.
6. Init and start the OpMode. Let it run long enough to pass through a few phases, then click
   **Stop** in Capture.
7. Enable **Visual Sync** and click **Start Playing**. Use the timeline and playback-speed
   controls to inspect the recorded telemetry.
8. Disable **Visual Sync** to return widgets to live data, or click **Reset** to make a new
   recording.

The demo emits changing time, phase, position, heading, battery, and cycle values. The values are
synthetic and repeat every 30 seconds, making changes obvious during replay.
