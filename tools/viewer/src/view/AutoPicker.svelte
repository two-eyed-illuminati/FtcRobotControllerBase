<script lang="ts">
  import { ChevronDown, LoaderCircle, TriangleAlert } from "@lucide/svelte";
  import { useFeed } from "../state/viewer.svelte";
  import { surfaceClass } from "../lib/components/ui/styles";

  const feed = useFeed();
  let open = $state(false);
  let root = $state<HTMLElement>();
  let autos = $derived(feed.autos);

  function choose(name: string) {
    open = false;
    void feed.selectAuto(name);
  }

  function badge(auto: (typeof autos.autos)[number]) {
    if (autos.exporting === auto.name) return "exporting…";
    if (auto.failed) return "failed";
    if (auto.exportedAt === null) return "not exported";
    if (auto.stale) return "stale";
    return null;
  }
</script>

<svelte:window
  onkeydown={(event) => event.key === "Escape" && (open = false)}
/>

{#if !autos.locked && autos.autos.length > 0}
  <div class="relative" bind:this={root}>
    <button
      type="button"
      aria-haspopup="listbox"
      aria-expanded={open}
      class="flex items-center gap-2 rounded-lg border border-border bg-base px-2.5 py-1.5 text-sm text-heading transition hover:border-primary"
      onclick={() => (open = !open)}
    >
      {#if autos.exporting}
        <LoaderCircle class="size-4 animate-spin text-primary" />
      {/if}
      <span class="font-mono">{autos.selected ?? "Choose an auto"}</span>
      <ChevronDown class="size-4 text-body" />
    </button>

    {#if open}
      <ul
        role="listbox"
        class="absolute left-0 z-10 mt-1 min-w-64 p-1 {surfaceClass}"
      >
        {#each autos.autos as auto (auto.name)}
          {@const status = badge(auto)}
          <li role="option" aria-selected={auto.name === autos.selected}>
            <button
              type="button"
              class="flex w-full items-center justify-between gap-4 rounded-lg px-2.5 py-1.5 text-left text-sm hover:bg-base {auto.name ===
              autos.selected
                ? 'text-primary'
                : 'text-heading'}"
              onclick={() => choose(auto.name)}
            >
              <span class="font-mono">{auto.name}</span>
              {#if status}
                <span
                  class="flex items-center gap-1 text-xs {auto.failed
                    ? 'text-danger'
                    : 'text-body'}"
                >
                  {#if auto.failed}<TriangleAlert class="size-3" />{/if}
                  {status}
                </span>
              {/if}
            </button>
          </li>
        {/each}
      </ul>
    {/if}
  </div>
{/if}

{#if feed.autoError}
  <span class="text-xs text-danger" role="alert">{feed.autoError}</span>
{/if}
