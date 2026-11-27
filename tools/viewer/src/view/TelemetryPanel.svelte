<script lang="ts">
  import { gamepadAt, gamepadsIn, isGamepadKey } from "../sim/gamepad";
  import { telemetryAt } from "../sim/simData";
  import { useFeed, usePlayback } from "../state/viewer.svelte";
  import PanelSection from "../lib/components/ui/PanelSection.svelte";
  import GamepadView from "./GamepadView.svelte";

  const feed = useFeed();
  const playback = usePlayback();

  const MIN_WIDTH = 160;
  const MAX_WIDTH = 720;
  const KEY_STEP = 16;
  const STORAGE_KEY = "viewer.telemetryWidth";

  const clampWidth = (value: number) =>
    Math.min(MAX_WIDTH, Math.max(MIN_WIDTH, Math.round(value)));

  function loadWidth() {
    try {
      const stored = Number(localStorage.getItem(STORAGE_KEY));
      return stored ? clampWidth(stored) : 256;
    } catch {
      return 256;
    }
  }

  let width = $state(loadWidth());
  let dragging = $state(false);
  let dragOrigin = { x: 0, width: 0 };

  let logs = $derived(
    feed.runs.filter((run) => run.visible && run.sim.telemetry),
  );

  function saveWidth() {
    try {
      localStorage.setItem(STORAGE_KEY, String(width));
    } catch {
      console.warn("[viewer] could not remember the telemetry width");
    }
  }

  function startDrag(event: PointerEvent & { currentTarget: HTMLElement }) {
    event.currentTarget.setPointerCapture(event.pointerId);
    dragOrigin = { x: event.clientX, width };
    dragging = true;
  }

  function drag(event: PointerEvent) {
    if (!dragging) return;
    width = clampWidth(dragOrigin.width + dragOrigin.x - event.clientX);
  }

  function endDrag() {
    if (!dragging) return;
    dragging = false;
    saveWidth();
    console.info(`[viewer] telemetry panel width ${width}px`);
  }

  function nudge(event: KeyboardEvent) {
    const direction = { ArrowLeft: 1, ArrowRight: -1 }[event.key];
    if (!direction) return;
    event.preventDefault();
    width = clampWidth(width + direction * KEY_STEP);
    saveWidth();
  }
</script>

<div class="relative flex min-h-0 shrink-0" style={`width: ${width}px`}>
  <!-- svelte-ignore a11y_no_noninteractive_tabindex, a11y_no_noninteractive_element_interactions -->
  <div
    role="separator"
    aria-orientation="vertical"
    aria-label="Resize telemetry panel"
    aria-valuemin={MIN_WIDTH}
    aria-valuemax={MAX_WIDTH}
    aria-valuenow={width}
    tabindex="0"
    class={[
      "group absolute inset-y-0 -left-2 z-10 flex w-2 cursor-col-resize touch-none justify-center outline-none",
    ]}
    onpointerdown={startDrag}
    onpointermove={drag}
    onpointerup={endDrag}
    onpointercancel={endDrag}
    onkeydown={nudge}
  >
    <span
      class={[
        "h-full w-0.5 rounded-full transition group-hover:bg-body/40 group-focus-visible:bg-body/40",
        dragging ? "bg-body/60" : "bg-transparent",
      ]}
    ></span>
  </div>
  <aside
    class="flex min-h-0 w-full flex-col gap-1 overflow-hidden rounded-xl bg-body/10 p-2"
    aria-label="Robot telemetry"
  >
    <h2 class="px-1 pb-1 text-sm font-semibold text-heading">Telemetry</h2>

    <div class="flex min-h-0 flex-1 flex-col gap-1 overflow-y-auto">
      {#each logs as run (run.id)}
        {@const all = telemetryAt(run.sim, playback.time)}
        {@const values = Object.entries(all).filter(
          ([key]) => !isGamepadKey(key),
        )}
        <PanelSection title={run.label} count={values.length}>
          <dl
            class="grid grid-cols-[minmax(0,auto)_1fr] gap-x-3 px-2 font-mono text-[11px] text-body tabular-nums"
          >
            {#each values as [key, value] (key)}
              <dt class="truncate" title={key}>{key}</dt>
              <dd class="truncate text-heading" title={value}>{value}</dd>
            {/each}
          </dl>
          {#each gamepadsIn(all) as name (name)}
            {@const pad = gamepadAt(all, name)}
            <PanelSection
              title={name.replace("gamepad", "Gamepad ")}
              open={name === "gamepad1"}
            >
              <GamepadView {pad} />
            </PanelSection>
          {/each}
          {#if values.length === 0}
            <p class="px-2 text-xs text-body">
              Nothing was shown with addData before this time.
            </p>
          {/if}
        </PanelSection>
      {/each}
      {#if logs.length === 0}
        <p class="px-2 py-1 text-xs text-body">
          Add a robot log (.run.jsonl) to see its telemetry at the playhead.
        </p>
      {/if}
    </div>
  </aside>
</div>
