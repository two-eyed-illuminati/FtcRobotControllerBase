<script lang="ts">
  import { Download, Plus, X } from "@lucide/svelte";
  import { flattenToAtomicSegments } from "../utils/pathTraversal";
  import { AVAILABLE_FIELD_MAPS } from "../config/defaults";
  import { useFeed, useOptions } from "../state/viewer.svelte";
  import PanelSection from "../lib/components/ui/PanelSection.svelte";
  import IconButton from "../lib/components/ui/IconButton.svelte";
  import {
    checkboxClass,
    fieldClass,
    surfaceClass,
  } from "../lib/components/ui/styles";

  const feed = useFeed();
  const options = useOptions();

  let segments = $derived(
    feed.doc
      ? flattenToAtomicSegments(feed.doc.startPoint, feed.doc.lines)
      : [],
  );
  let totalLength = $derived(
    segments.reduce((sum, segment) => sum + segment.curve.length, 0),
  );

  async function addFiles(event: Event & { currentTarget: HTMLInputElement }) {
    const input = event.currentTarget;
    for (const file of input.files ?? []) {
      feed.addRun(file.name, await file.text());
    }
    input.value = "";
  }
</script>

<aside
  class={[surfaceClass, "flex min-h-0 flex-col gap-1 overflow-hidden p-2"]}
  aria-label="Layers"
>
  <h2 class="px-1 pb-1 text-sm font-semibold text-heading">Layers</h2>

  <div class="flex min-h-0 flex-1 flex-col gap-1 overflow-y-auto">
    <PanelSection title="Paths" count={segments.length}>
      <div class="flex flex-col gap-px">
        {#each segments as { line, index, curve } (line.id)}
          <label
            class="flex h-7 items-center gap-2 rounded-md px-2 text-sm text-body transition hover:bg-body/8"
          >
            <input
              type="checkbox"
              class={checkboxClass}
              checked={!options.hiddenPathIds.has(line.id)}
              onchange={() => options.togglePath(line.id)}
            />
            <span
              class="size-2 shrink-0 rounded-full"
              style={`background: ${line.color}`}
            ></span>
            <span class="truncate">{line.name || `Path ${index + 1}`}</span>
            <span class="ml-auto font-mono text-[11px] tabular-nums opacity-70"
              >{curve.length.toFixed(1)} in</span
            >
          </label>
        {/each}
        {#if segments.length === 0}
          <p class="px-2 py-1 text-xs text-body">No paths.</p>
        {:else}
          <p class="px-2 pt-1 text-xs text-body">
            Total {totalLength.toFixed(1)} in
          </p>
        {/if}
      </div>
    </PanelSection>

    <PanelSection title="Show">
      <div class="flex flex-col gap-px">
        {#each [{ label: "Control points", key: "showControlPoints" }, { label: "Heading arrows", key: "showHeadings" }, { label: "Robot at start", key: "showRobotAtStart" }, { label: "Simulated trace", key: "showTrace" }, { label: "Robot footprints", key: "showFootprints" }, { label: "Swept area", key: "showSwept" }] as const as { label, key } (key)}
          <label
            class="flex h-7 items-center gap-2 rounded-md px-2 text-sm text-body transition hover:bg-body/8"
          >
            <input
              type="checkbox"
              class={checkboxClass}
              bind:checked={options[key]}
            />
            {label}
          </label>
        {/each}
        {#if options.showFootprints}
          <label class="flex items-center gap-2 px-2 pt-1 text-xs text-body">
            Spacing (in)
            <input
              type="number"
              min="1"
              class={fieldClass}
              bind:value={options.footprintSpacing}
            />
          </label>
        {/if}
      </div>
    </PanelSection>

    <PanelSection title="Runs" count={feed.runs.length}>
      <div class="flex flex-col gap-px">
        {#each feed.runs as run (run.id)}
          <div
            class="flex h-7 items-center gap-2 rounded-md pr-1 pl-2 text-sm text-body transition hover:bg-body/8"
          >
            <label class="flex min-w-0 flex-1 items-center gap-2">
              <input
                type="checkbox"
                class={checkboxClass}
                bind:checked={run.visible}
              />
              <span
                class="size-2 shrink-0 rounded-full"
                style={`background: ${run.color}`}
              ></span>
              <span class="truncate" title={run.label}>{run.label}</span>
              <span
                class="ml-auto font-mono text-[11px] tabular-nums opacity-70"
                >{run.sim.duration.toFixed(1)} s</span
              >
            </label>
            <IconButton
              label="Remove run"
              size="sm"
              onclick={() => feed.removeRun(run.id)}
            >
              <X />
            </IconButton>
          </div>
        {/each}
        <label
          class="flex h-7 cursor-pointer items-center gap-2 rounded-md px-2 text-sm text-body transition hover:bg-body/8"
        >
          <Plus class="size-4" />
          Add run (.sim.json, .trace.json)
          <input
            type="file"
            accept=".json,application/json"
            multiple
            class="sr-only"
            onchange={addFiles}
          />
        </label>
        <button
          type="button"
          class="flex h-7 cursor-pointer items-center gap-2 rounded-md px-2 text-sm text-body transition hover:bg-body/8 disabled:cursor-wait disabled:opacity-60"
          disabled={feed.fetchingRun}
          onclick={() => feed.fetchLatestRun()}
        >
          <Download class="size-4" />
          {feed.fetchingRun ? "Fetching..." : "Fetch latest run"}
        </button>
        {#if feed.runError}
          <p class="px-2 pt-1 text-xs text-danger">{feed.runError}</p>
        {/if}
      </div>
    </PanelSection>

    {#if feed.sim}
      <PanelSection title="Simulation">
        <dl
          class="grid grid-cols-[1fr_auto] gap-x-2 px-2 font-mono text-[11px] text-body tabular-nums"
        >
          <dt>duration</dt>
          <dd class="text-heading">{feed.sim.duration.toFixed(2)} s</dd>
          <dt>max speed</dt>
          <dd class="text-heading">
            {feed.sim.summary.maxSpeed.toFixed(1)} in/s
          </dd>
          <dt>end speed</dt>
          <dd class="text-heading">
            {feed.sim.summary.endSpeed.toFixed(2)} in/s
          </dd>
          <dt>end error</dt>
          <dd class="text-heading">
            {feed.sim.summary.endPositionError.toFixed(2)} in
          </dd>
        </dl>
      </PanelSection>
      <PanelSection title="Simulated robot" open={false}>
        <p class="px-2 pb-1 text-xs text-body">
          Tuned biobuzz config. Turn axis is assumed. Edit SimRobot.java.
        </p>
        <dl
          class="grid grid-cols-[1fr_auto] gap-x-2 px-2 font-mono text-[11px] text-body"
        >
          {#each Object.entries(feed.sim.assumptions) as [key, value] (key)}
            <dt class="truncate" title={key}>{key}</dt>
            <dd class="max-w-32 truncate text-heading" title={String(value)}>
              {typeof value === "number"
                ? +value.toPrecision(4)
                : String(value)}
            </dd>
          {/each}
        </dl>
      </PanelSection>
    {/if}

    <PanelSection title="Field">
      <div class="px-1">
        <select class={fieldClass} bind:value={options.fieldMap}>
          {#each AVAILABLE_FIELD_MAPS as field (field.value)}
            <option value={field.value}>{field.label}</option>
          {/each}
        </select>
      </div>
    </PanelSection>
  </div>
</aside>
