<script lang="ts">
  import { onMount } from "svelte";
  import StatusBar from "./view/StatusBar.svelte";
  import LayersPanel from "./view/LayersPanel.svelte";
  import FieldPanel from "./view/FieldPanel.svelte";
  import {
    PathFeed,
    Playback,
    ViewOptions,
    provideViewer,
  } from "./state/viewer.svelte";
  import { Theme } from "./state/theme.svelte";

  const feed = new PathFeed();
  const options = new ViewOptions();
  const theme = new Theme();
  const playback = new Playback(feed);
  provideViewer(feed, options, playback);

  onMount(() => void feed.start());
</script>

<StatusBar {theme} />

<div class="h-screen w-screen px-3 pt-17 pb-3">
  <div class="viewer-grid h-full gap-3">
    <LayersPanel />
    <FieldPanel scheme={theme.resolved} />
  </div>
</div>
