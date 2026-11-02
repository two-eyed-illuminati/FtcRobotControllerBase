<script lang="ts">
  import type { SimData } from "../sim/simData";

  interface Props {
    sim: SimData;
    time: number;
    onseek: (seconds: number) => void;
  }

  let { sim, time, onseek }: Props = $props();

  const WIDTH = 1000;
  const HEIGHT = 100;
  const MARKER_LABELS = {
    join: "Segment join",
    pathStart: "Next path starts",
    pathEnd: "Path end reached",
    stop: "Stopped",
  } as const;

  let top = $derived(
    Math.max(
      1,
      ...sim.samples.speed,
      ...sim.samples.targetVelocity.map((value) => Math.min(value, 1e6)),
    ) * 1.1,
  );
  const xOf = (seconds: number) => (seconds / sim.duration) * WIDTH;
  const yOf = (value: number) => HEIGHT - (Math.max(0, value) / top) * HEIGHT;
  const line = (values: number[]) =>
    values
      .map(
        (value, i) =>
          `${xOf(sim.samples.t[i]).toFixed(1)},${yOf(value).toFixed(1)}`,
      )
      .join(" ");

  let speedLine = $derived(line(sim.samples.speed));
  let targetLine = $derived(line(sim.samples.targetVelocity));
  let markers = $derived(
    sim.events.filter((event) => event.type !== "pathStart" || event.t > 0),
  );

  let dragging = false;

  function seekFrom(event: PointerEvent) {
    const rect = (event.currentTarget as Element).getBoundingClientRect();
    const ratio = Math.min(
      1,
      Math.max(0, (event.clientX - rect.left) / rect.width),
    );
    onseek(ratio * sim.duration);
  }
</script>

<!-- svelte-ignore a11y_no_static_element_interactions -->
<div
  class="relative h-24 w-full cursor-ew-resize rounded-lg bg-body/8"
  onpointerdown={(event) => {
    dragging = true;
    (event.currentTarget as Element).setPointerCapture(event.pointerId);
    seekFrom(event);
  }}
  onpointermove={(event) => dragging && seekFrom(event)}
  onpointerup={() => (dragging = false)}
  onlostpointercapture={() => (dragging = false)}
>
  <svg
    class="absolute inset-0 size-full"
    viewBox={`0 0 ${WIDTH} ${HEIGHT}`}
    preserveAspectRatio="none"
    role="img"
    aria-label="Speed over time"
  >
    <polyline
      points={targetLine}
      fill="none"
      class="stroke-body"
      stroke-width="1.5"
      stroke-dasharray="6 4"
      vector-effect="non-scaling-stroke"
    />
    <polyline
      points={speedLine}
      fill="none"
      class="stroke-primary"
      stroke-width="2"
      vector-effect="non-scaling-stroke"
    />
    {#each markers as marker, index (index)}
      <line
        x1={xOf(marker.t)}
        x2={xOf(marker.t)}
        y1="0"
        y2={HEIGHT}
        class={marker.type === "join" ? "stroke-warning" : "stroke-body"}
        stroke-width="1.5"
        vector-effect="non-scaling-stroke"
        opacity="0.8"
      />
    {/each}
    <line
      x1={xOf(time)}
      x2={xOf(time)}
      y1="0"
      y2={HEIGHT}
      class="stroke-heading"
      stroke-width="2"
      vector-effect="non-scaling-stroke"
    />
  </svg>

  {#each markers as marker, index (index)}
    <span
      class={[
        "pointer-events-none absolute top-0.5 -translate-x-1/2 rounded px-1 text-[10px] leading-4",
        marker.type === "join"
          ? "bg-warning text-black"
          : "bg-body/30 text-heading",
      ]}
      style={`left: ${(marker.t / sim.duration) * 100}%;`}
      title={`${MARKER_LABELS[marker.type]} at ${marker.t.toFixed(2)} s, ${marker.speed.toFixed(1)} in/s`}
    >
      {marker.type === "join"
        ? "join"
        : marker.type === "pathEnd"
          ? "end"
          : marker.type === "stop"
            ? "stop"
            : "next"}
      {marker.speed.toFixed(0)}
    </span>
  {/each}
  <span
    class="pointer-events-none absolute right-1 bottom-0.5 text-[10px] text-body"
  >
    {top.toFixed(0)} in/s max · dashed = target
  </span>
</div>
