<script lang="ts">
  import { Plus, RefreshCw, X } from "@lucide/svelte";
  import { onMount } from "svelte";
  import { flattenToAtomicSegments } from "../utils/pathTraversal";
  import { AVAILABLE_FIELD_MAPS } from "../config/defaults";
  import { useFeed, useOptions } from "../state/viewer.svelte";
  import PanelSection from "../lib/components/ui/PanelSection.svelte";
  import IconButton from "../lib/components/ui/IconButton.svelte";
  import AutoPicker from "./AutoPicker.svelte";
  import {
    checkboxClass,
    fieldClass,
    surfaceClass,
  } from "../lib/components/ui/styles";

  const feed = useFeed();
  const options = useOptions();

  let fileInput = $state<HTMLInputElement>();
  let tab = $state<"runs" | "layers">("runs");
  let visibleRuns = $derived(feed.runs.filter((run) => run.visible).length);
  let fileRuns = $derived(feed.runs.filter((run) => run.source === null));

  onMount(() => void feed.listRobotLogs());

  let segments = $derived(
    feed.shownDoc
      ? flattenToAtomicSegments(feed.shownDoc.startPoint, feed.shownDoc.lines)
      : [],
  );
  let totalLength = $derived(
    segments.reduce((sum, segment) => sum + segment.curve.length, 0),
  );

  const sizeLabel = (bytes: number) =>
    bytes < 1024 * 1024
      ? `${Math.max(1, Math.round(bytes / 1024))} KB`
      : `${(bytes / 1024 / 1024).toFixed(1)} MB`;
  const savedLabel = (savedAt: number | null) =>
    savedAt === null
      ? ""
      : new Intl.DateTimeFormat(undefined, {
          month: "short",
          day: "numeric",
          hour: "numeric",
          minute: "2-digit",
        }).format(savedAt);
  const modeLabel = (name: string) => name.replace(/-\d+\.[\w.]+$/, "");
  const tabClass = (active: boolean) => [
    "flex h-7 items-center justify-center gap-1.5 rounded-md text-xs font-medium transition",
    active
      ? "bg-surface text-heading shadow-sm"
      : "text-body hover:text-heading",
  ];
  const cardClass =
    "rounded-xl border border-border bg-base/40 transition hover:bg-body/8";

  function toggleLog(name: string) {
    const run = feed.runForLog(name);
    if (run) run.visible = !run.visible;
    else void feed.fetchRun(name);
  }

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
  <div class="grid grid-cols-2 rounded-lg bg-body/10 p-0.5" role="tablist">
    <button
      type="button"
      role="tab"
      aria-selected={tab === "runs"}
      class={tabClass(tab === "runs")}
      onclick={() => (tab = "runs")}
    >
      Runs
      <span
        class={[
          "font-mono tabular-nums",
          tab === "runs" ? "text-primary" : "opacity-70",
        ]}>{visibleRuns}</span
      >
    </button>
    <button
      type="button"
      role="tab"
      aria-selected={tab === "layers"}
      class={tabClass(tab === "layers")}
      onclick={() => (tab = "layers")}
    >
      Layers
    </button>
  </div>

  {#if tab === "runs"}
    <div class="relative px-0.5">
      <div class="flex h-8 items-center px-0.5">
        <span class="text-xs font-semibold tracking-wide text-body uppercase"
          >Simulated path</span
        >
      </div>
      <AutoPicker />
    </div>
    <div class="flex min-h-0 flex-1 flex-col gap-1 overflow-y-auto">
      <div class="flex h-8 items-center px-1">
        <span class="text-xs font-semibold tracking-wide text-body uppercase"
          >On the robot</span
        >
        <IconButton
          label="Refresh robot logs"
          size="sm"
          class="ml-auto"
          disabled={feed.listingLogs}
          onclick={() => feed.listRobotLogs()}
        >
          <RefreshCw class={feed.listingLogs ? "animate-spin" : ""} />
        </IconButton>
      </div>
      <div class="flex flex-col gap-1.5">
        {#each feed.robotLogs as log (log.name)}
          {@const run = feed.runForLog(log.name)}
          {@const fetching = feed.fetchingRun === log.name}
          <label
            class={[
              cardClass,
              "flex cursor-pointer items-center gap-2.5 py-1.5 pr-3 pl-2.5",
              !run?.visible && "opacity-70 hover:opacity-100",
            ]}
          >
            {#if fetching}
              <RefreshCw class="size-4 shrink-0 animate-spin text-body" />
            {:else}
              <input
                type="checkbox"
                class={checkboxClass}
                style={run ? `accent-color: ${run.color}` : undefined}
                checked={run?.visible ?? false}
                disabled={feed.fetchingRun !== null}
                onchange={() => toggleLog(log.name)}
              />
            {/if}
            <span class="min-w-0 flex-1">
              <span
                class="block truncate text-sm font-medium text-heading"
                title={log.name}>{modeLabel(log.name)}</span
              >
              <span class="block font-mono text-[11px] tabular-nums text-body"
                >{savedLabel(log.savedAt)} · {sizeLabel(log.bytes)}</span
              >
            </span>
          </label>
        {/each}
        {#if feed.logsError}
          <p class="px-2 text-xs text-danger">{feed.logsError}</p>
        {:else if feed.robotLogs.length === 0 && !feed.listingLogs}
          <p class="px-2 text-xs text-body">No robot logs found.</p>
        {/if}
      </div>

      <div class="mt-3 flex h-8 items-center px-1">
        <span class="text-xs font-semibold tracking-wide text-body uppercase"
          >From files</span
        >
        <IconButton
          label="Add a file"
          size="sm"
          class="ml-auto"
          onclick={() => fileInput?.click()}
        >
          <Plus />
        </IconButton>
        <input
          bind:this={fileInput}
          type="file"
          accept=".json,.jsonl,application/json"
          multiple
          class="sr-only"
          onchange={addFiles}
        />
      </div>
      <div class="flex flex-col gap-1.5">
        {#each fileRuns as run (run.id)}
          <div
            class={[
              cardClass,
              "group flex h-10 items-center gap-2 pr-1 pl-2.5",
              !run.visible && "opacity-60",
            ]}
          >
            <label class="flex min-w-0 flex-1 items-center gap-2">
              <input
                type="checkbox"
                class={checkboxClass}
                style={`accent-color: ${run.color}`}
                bind:checked={run.visible}
              />
              <span
                class="min-w-0 truncate text-sm font-medium text-heading"
                title={run.label}>{modeLabel(run.label)}</span
              >
            </label>
            <IconButton
              label="Remove run"
              size="sm"
              class="opacity-0 group-hover:opacity-100 focus-visible:opacity-100"
              onclick={() => feed.removeRun(run.id)}
            >
              <X />
            </IconButton>
          </div>
        {/each}
      </div>
      {#if feed.runError}
        <p class="px-2 pt-1 text-xs text-danger">{feed.runError}</p>
      {/if}
    </div>
  {:else}
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
              <span
                class="ml-auto font-mono text-[11px] tabular-nums opacity-70"
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

      {#if feed.shownSim}
        <PanelSection title="Simulation">
          <dl
            class="grid grid-cols-[1fr_auto] gap-x-2 px-2 font-mono text-[11px] text-body tabular-nums"
          >
            <dt>duration</dt>
            <dd class="text-heading">{feed.shownSim.duration.toFixed(2)} s</dd>
            <dt>max speed</dt>
            <dd class="text-heading">
              {feed.shownSim.summary.maxSpeed.toFixed(1)} in/s
            </dd>
            <dt>end speed</dt>
            <dd class="text-heading">
              {feed.shownSim.summary.endSpeed.toFixed(2)} in/s
            </dd>
            <dt>end error</dt>
            <dd class="text-heading">
              {feed.shownSim.summary.endPositionError.toFixed(2)} in
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
            {#each Object.entries(feed.shownSim.assumptions) as [key, value] (key)}
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
  {/if}
</aside>
