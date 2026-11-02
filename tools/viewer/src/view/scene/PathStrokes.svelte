<script lang="ts">
  import { LINE_WIDTH } from "../../config/defaults";
  import { segmentStroke } from "../../lib/scene/stroke";
  import type { FlatSegment } from "../../utils/pathTraversal";
  import { strokePath } from "./svgPath";

  interface Props {
    segments: FlatSegment[];
  }

  let { segments }: Props = $props();

  let strokes = $derived(
    segments.map((segment) => ({
      segment,
      d: strokePath(segmentStroke(segment.curve)),
    })),
  );
</script>

{#each strokes as { segment, d } (segment.line.id)}
  <path
    id={`line-${segment.index + 1}`}
    {d}
    fill="none"
    stroke={segment.line.color}
    stroke-width={LINE_WIDTH}
    stroke-linecap="round"
  />
{/each}
