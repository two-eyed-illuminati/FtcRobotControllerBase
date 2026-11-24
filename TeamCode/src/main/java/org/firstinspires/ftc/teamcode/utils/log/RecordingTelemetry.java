package org.firstinspires.ftc.teamcode.utils.log;

import com.acmerobotics.dashboard.telemetry.MultipleTelemetry;

import org.firstinspires.ftc.robotcore.external.Func;
import org.firstinspires.ftc.robotcore.external.Telemetry;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * MultipleTelemetry that also writes every addData value to the RunLog on update(), only when the
 * value changed since the last update. Values added through addLine() or Item.setValue() are not recorded.
 */
public final class RecordingTelemetry extends MultipleTelemetry {
  private final RunLog runLog;
  private final Map<String, Object> pending = new LinkedHashMap<>();
  private final Map<String, String> lastRecorded = new HashMap<>();

  public RecordingTelemetry(RunLog runLog, Telemetry... telemetries) {
    super(telemetries);
    this.runLog = runLog;
  }

  @Override
  public Item addData(String caption, String format, Object... args) {
    pending.put(caption, String.format(format, args));
    return super.addData(caption, format, args);
  }

  @Override
  public Item addData(String caption, Object value) {
    pending.put(caption, value);
    return super.addData(caption, value);
  }

  @Override
  public <T> Item addData(String caption, Func<T> valueProducer) {
    pending.put(caption, valueProducer);
    return super.addData(caption, valueProducer);
  }

  @Override
  public <T> Item addData(String caption, String format, Func<T> valueProducer) {
    pending.put(caption, (Func<String>) () -> String.format(format, valueProducer.value()));
    return super.addData(caption, format, valueProducer);
  }

  @Override
  public void clear() {
    pending.clear();
    super.clear();
  }

  @Override
  public void clearAll() {
    pending.clear();
    super.clearAll();
  }

  @Override
  public boolean update() {
    Map<String, String> changed = new LinkedHashMap<>();
    for (Map.Entry<String, Object> entry : pending.entrySet()) {
      Object value = entry.getValue() instanceof Func ? ((Func<?>) entry.getValue()).value() : entry.getValue();
      String text = String.valueOf(value);
      if (!text.equals(lastRecorded.put(entry.getKey(), text))) changed.put(entry.getKey(), text);
    }
    pending.clear();
    if (!changed.isEmpty()) runLog.telemetry(changed);
    return super.update();
  }
}
