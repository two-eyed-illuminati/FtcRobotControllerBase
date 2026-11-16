<script lang="ts">
  import type { StartPose } from "../../types";
  import { POINT_RADIUS } from "../../config/defaults";
  import type { FlatSegment } from "../../utils/pathTraversal";

  interface Props {
    startPoint: StartPose;
    segments: FlatSegment[];
    showControlPoints: boolean;
  }

  let { startPoint, segments, showControlPoints }: Props = $props();
</script>

<circle
  cx={startPoint.x}
  cy={startPoint.y}
  r={POINT_RADIUS * 1.3}
  fill="#ffffff"
  stroke="#000000"
  stroke-width={0.25}
  aria-label="Start point"
/>

{#each segments as { line, index } (line.id)}
  {#if showControlPoints}
    {#each line.throughPoints ?? line.controlPoints as point, pointIndex (pointIndex)}
      <circle
        cx={point.x}
        cy={point.y}
        r={POINT_RADIUS * 0.7}
        fill="none"
        stroke={line.color}
        stroke-width={0.3}
        aria-label={`Path ${index + 1} control point ${pointIndex + 1}`}
      />
    {/each}
  {/if}
  <circle
    cx={line.endPoint.x}
    cy={line.endPoint.y}
    r={POINT_RADIUS}
    fill={line.color}
    aria-label={`Path ${index + 1} end point`}
  />
{/each}
