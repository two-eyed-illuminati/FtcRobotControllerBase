<script lang="ts">
  import { Moon, Sun, TriangleAlert } from "@lucide/svelte";
  import { useFeed } from "../state/viewer.svelte";
  import IconButton from "../lib/components/ui/IconButton.svelte";
  import type { Theme } from "../state/theme.svelte";

  interface Props {
    theme: Theme;
  }

  let { theme }: Props = $props();

  const feed = useFeed();
  let dark = $derived(theme.resolved === "dark");
</script>

<header
  class="fixed inset-x-0 top-0 z-100 flex h-14 items-center justify-between gap-4 border-b border-border bg-surface/80 px-4 backdrop-blur"
>
  <div class="flex min-w-0 items-center gap-3">
    <img src="/logo-duck.svg" alt="" class="size-6" draggable="false" />
    <span class="text-sm font-semibold whitespace-nowrap text-heading">
      Pedro Path Viewer
    </span>
    <span class="truncate font-mono text-xs text-body" title={feed.file}>
      {feed.file}
    </span>
  </div>

  <div class="flex items-center gap-3">
    {#if feed.status === "ok"}
      <span class="text-xs text-body">
        updated {new Date(feed.updatedAt ?? 0).toLocaleTimeString()}
      </span>
    {:else if feed.status === "loading"}
      <span class="text-xs text-body">loading…</span>
    {/if}
    <IconButton
      label={dark ? "Switch to light" : "Switch to dark"}
      onclick={() => (theme.choice = dark ? "light" : "dark")}
    >
      {#if dark}<Sun />{:else}<Moon />{/if}
    </IconButton>
  </div>
</header>

<div class="fixed inset-x-0 top-14 z-90 flex flex-col">
  {#if feed.status === "error"}
    <div
      role="alert"
      class="flex items-start gap-3 bg-danger px-4 py-2 text-sm text-accent-foreground"
    >
      <TriangleAlert class="mt-0.5 size-4 shrink-0" />
      <div class="min-w-0">
        <div class="font-semibold">{feed.error}</div>
        <div class="truncate font-mono text-xs opacity-90">{feed.file}</div>
        {#if feed.doc}
          <div class="text-xs opacity-90">Showing the last valid export.</div>
        {/if}
      </div>
    </div>
  {/if}

  {#if feed.exportError}
    <div
      role="alert"
      class="max-h-[40vh] overflow-auto bg-danger px-4 py-2 text-accent-foreground"
    >
      <div class="text-sm font-semibold">
        Java export failed. Showing the last good path.
      </div>
      <pre
        class="font-mono text-xs whitespace-pre-wrap">{feed.exportError}</pre>
    </div>
  {/if}

  {#if feed.simError}
    <div class="bg-warning px-4 py-1 text-xs text-black">
      No playback: {feed.simError}
    </div>
  {/if}

  {#each feed.sim?.warnings ?? [] as warning (warning)}
    <div class="bg-warning px-4 py-1 text-xs text-black">
      Simulation: {warning}
    </div>
  {/each}

  {#each feed.warnings as warning (warning)}
    <div class="bg-warning px-4 py-1 text-xs text-black">{warning}</div>
  {/each}
</div>
