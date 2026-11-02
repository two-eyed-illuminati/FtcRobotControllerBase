<script lang="ts">
  import { Pause, Play, Repeat } from "@lucide/svelte";
  import { PLAYBACK_RATES, useFeed, usePlayback } from "../state/viewer.svelte";
  import IconButton from "../lib/components/ui/IconButton.svelte";
  import { fieldClass } from "../lib/components/ui/styles";
  import SpeedGraph from "./SpeedGraph.svelte";

  const feed = useFeed();
  const playback = usePlayback();

  let sim = $derived(feed.sim);
  let sample = $derived(playback.sample);

  $effect(() => {
    if (playback.time > playback.duration) playback.seek(playback.duration);
  });

  let phase = $derived(
    !sample
      ? ""
      : sample.holding
        ? "holding"
        : sample.braking
          ? "braking"
          : "driving",
  );
</script>

{#if sim && sample}
  <div class="flex flex-col gap-1.5 px-1" aria-label="Simulation playback">
    <div class="flex items-center gap-3">
      <div class="flex items-center gap-0.5">
        <IconButton
          label={playback.playing ? "Pause" : "Play"}
          onclick={() => playback.toggle()}
          class="text-primary!"
        >
          {#if playback.playing}<Pause />{:else}<Play />{/if}
        </IconButton>
        <IconButton
          label={playback.loop ? "Looping" : "Loop"}
          active={playback.loop}
          onclick={() => (playback.loop = !playback.loop)}
        >
          <Repeat />
        </IconButton>
      </div>
      <select
        class={[fieldClass, "h-8 w-20! shrink-0"]}
        aria-label="Playback speed"
        bind:value={playback.rate}
      >
        {#each PLAYBACK_RATES as rate (rate)}
          <option value={rate}>{rate}x</option>
        {/each}
      </select>
      <input
        type="range"
        min="0"
        max={sim.duration}
        step={sim.dt}
        value={playback.time}
        aria-label="Playback position"
        class="min-w-0 flex-1 cursor-pointer"
        oninput={(event) =>
          playback.seek(parseFloat(event.currentTarget.value))}
      />
      <div class="font-mono text-xs whitespace-nowrap text-body tabular-nums">
        <span class="text-heading">{playback.time.toFixed(2)}</span>
        / {sim.duration.toFixed(2)} s
      </div>
    </div>

    <SpeedGraph
      {sim}
      time={playback.time}
      onseek={(seconds) => playback.seek(seconds)}
    />

    <div
      class="flex flex-wrap gap-x-4 font-mono text-xs text-body tabular-nums"
    >
      <span
        >speed <b class="text-heading">{sample.speed.toFixed(1)}</b> in/s</span
      >
      <span
        >target <b class="text-heading">{sample.targetVelocity.toFixed(1)}</b> in/s</span
      >
      <span
        >segment <b class="text-heading">{sample.segment + 1}</b> of {sim
          .segments.length}</span
      >
      <span>t <b class="text-heading">{sample.closestT.toFixed(2)}</b></span>
      <span class="text-heading">{phase}</span>
    </div>
  </div>
{/if}
