<script lang="ts">
  import type { GamepadState, StickPoint } from "../sim/gamepad";

  interface Props {
    pad: GamepadState;
  }

  interface Point {
    x: number;
    y: number;
  }

  let { pad }: Props = $props();

  const STICK_WELL_RADIUS = 20;
  const STICK_TRAVEL = 15;
  const TRIGGER_WIDTH = 44;
  const BUMPER_WIDTH = 52;
  const SHOULDER_HEIGHT = 12;
  const VALUE_FONT_SIZE = 11;
  const TRIGGER_VALUE_Y = -8;
  const STICK_VALUE_ROWS = [
    { label: "x", offset: 33 },
    { label: "y", offset: 46 },
  ] as const;
  const STICK_VALUE_HALF_WIDTH = 24;
  const BUMPER_Y = 14;
  const TRIGGERS = [
    { label: "LT", x: 44, axis: "left_trigger" },
    { label: "RT", x: 172, axis: "right_trigger" },
  ] as const;
  const BUMPERS = [
    { label: "LB", x: 40, key: "left_bumper" },
    { label: "RB", x: 168, key: "right_bumper" },
  ] as const;
  const FACE_OFFSET = 18;

  /** Logitech F310 in X mode: d-pad top-left, sticks side by side at the bottom. */
  const BODY =
    "M40 30H220C240 30 252 54 256 96C259 128 250 146 232 146C218 146 210 138 202 124H58C50 138 42 146 28 146C10 146 1 128 4 96C8 54 20 30 40 30Z";
  const LEFT_STICK = { x: 98, y: 100 };
  const RIGHT_STICK = { x: 162, y: 100 };
  const DPAD = { x: 62, y: 62 };
  const FACE = { x: 198, y: 62 };
  const FACE_BUTTONS = [
    { key: "a", label: "A", dx: 0, dy: FACE_OFFSET, lit: "fill-success" },
    { key: "b", label: "B", dx: FACE_OFFSET, dy: 0, lit: "fill-danger" },
    { key: "x", label: "X", dx: -FACE_OFFSET, dy: 0, lit: "fill-blue-500" },
    { key: "y", label: "Y", dx: 0, dy: -FACE_OFFSET, lit: "fill-warning" },
  ] as const;

  const DPAD_ARMS = [
    { key: "dpad_up", x: -6, y: -20, width: 12, height: 14 },
    { key: "dpad_down", x: -6, y: 6, width: 12, height: 14 },
    { key: "dpad_left", x: -20, y: -6, width: 14, height: 12 },
    { key: "dpad_right", x: 6, y: -6, width: 14, height: 12 },
  ] as const;

  const place = (center: Point, point: StickPoint) => {
    const length = Math.hypot(point.x, point.y);
    const scale = length > 1 ? 1 / length : 1;
    return {
      x: center.x + point.x * scale * STICK_TRAVEL,
      y: center.y + point.y * scale * STICK_TRAVEL,
    };
  };

  const format = (value: number) => value.toFixed(2);
  const lit = (pressed: boolean) => (pressed ? "fill-primary" : "fill-body/20");
</script>

{#snippet shoulderLabel(label: string, x: number, y: number)}
  <text
    {x}
    y={y + SHOULDER_HEIGHT / 2}
    font-size="8"
    font-weight="600"
    text-anchor="middle"
    dominant-baseline="central"
    class="fill-heading pointer-events-none">{label}</text
  >
{/snippet}

{#snippet valueLabel(
  text: string,
  x: number,
  y: number,
  anchor: "start" | "middle" | "end" = "middle",
)}
  <text
    {x}
    {y}
    font-size={VALUE_FONT_SIZE}
    text-anchor={anchor}
    dominant-baseline="central"
    class="fill-heading font-mono tabular-nums">{text}</text
  >
{/snippet}

{#snippet stick(center: Point, position: StickPoint, pressed: boolean)}
  {@const dot = place(center, position)}
  <circle
    cx={center.x}
    cy={center.y}
    r={STICK_WELL_RADIUS}
    class={["stroke-body/40", pressed ? "fill-primary/30" : "fill-body/10"]}
  />
  <circle
    cx={dot.x}
    cy={dot.y}
    r="5"
    class={pressed ? "fill-primary" : "fill-heading"}
  />
  {#each STICK_VALUE_ROWS as row (row.label)}
    {@const y = center.y + row.offset}
    {@render valueLabel(
      row.label,
      center.x - STICK_VALUE_HALF_WIDTH,
      y,
      "start",
    )}
    {@render valueLabel(
      format(row.label === "x" ? position.x : position.y),
      center.x + STICK_VALUE_HALF_WIDTH,
      y,
      "end",
    )}
  {/each}
{/snippet}

<div class="flex flex-col gap-1 px-2">
  <svg
    viewBox="0 -16 260 178"
    role="img"
    aria-label="Gamepad state"
    class="w-full"
  >
    <path d={BODY} class="fill-body/10 stroke-body/40" />

    {#each TRIGGERS as trigger (trigger.label)}
      <rect
        x={trigger.x}
        y="0"
        width={TRIGGER_WIDTH}
        height={SHOULDER_HEIGHT}
        rx="3"
        class="fill-body/20"
      />
      <rect
        x={trigger.x}
        y="0"
        width={TRIGGER_WIDTH * pad.axes[trigger.axis]}
        height={SHOULDER_HEIGHT}
        rx="3"
        class="fill-primary"
      />
      {@render shoulderLabel(trigger.label, trigger.x + TRIGGER_WIDTH / 2, 0)}
      {@render valueLabel(
        format(pad.axes[trigger.axis]),
        trigger.x + TRIGGER_WIDTH / 2,
        TRIGGER_VALUE_Y,
      )}
    {/each}

    {#each BUMPERS as bumper (bumper.label)}
      <rect
        x={bumper.x}
        y={BUMPER_Y}
        width={BUMPER_WIDTH}
        height={SHOULDER_HEIGHT}
        rx="3"
        class={lit(pad.buttons[bumper.key])}
      />
      {@render shoulderLabel(
        bumper.label,
        bumper.x + BUMPER_WIDTH / 2,
        BUMPER_Y,
      )}
    {/each}

    <rect
      x={DPAD.x - 6}
      y={DPAD.y - 6}
      width="12"
      height="12"
      class="fill-body/20"
    />
    {#each DPAD_ARMS as arm (arm.key)}
      <rect
        x={DPAD.x + arm.x}
        y={DPAD.y + arm.y}
        width={arm.width}
        height={arm.height}
        rx="2"
        class={lit(pad.buttons[arm.key])}
      />
    {/each}

    {#each FACE_BUTTONS as button (button.key)}
      <circle
        cx={FACE.x + button.dx}
        cy={FACE.y + button.dy}
        r="9"
        class={pad.buttons[button.key] ? button.lit : "fill-body/20"}
      />
      <text
        x={FACE.x + button.dx}
        y={FACE.y + button.dy}
        font-size="10"
        font-weight="600"
        text-anchor="middle"
        dominant-baseline="central"
        class={pad.buttons[button.key] ? "fill-white" : "fill-body"}
        >{button.label}</text
      >
    {/each}

    {@render stick(
      LEFT_STICK,
      { x: pad.axes.left_stick_x, y: pad.axes.left_stick_y },
      pad.buttons.left_stick_button,
    )}
    {@render stick(
      RIGHT_STICK,
      { x: pad.axes.right_stick_x, y: pad.axes.right_stick_y },
      pad.buttons.right_stick_button,
    )}
  </svg>
</div>
