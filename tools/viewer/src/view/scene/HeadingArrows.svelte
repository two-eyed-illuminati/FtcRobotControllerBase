<script lang="ts">
  import { segmentHeadingAt } from "../../utils/headingInterpolation";
  import type { FlatSegment } from "../../utils/pathTraversal";

  interface Props {
    /** Every segment, so group headings resolve even when some are hidden. */
    all: FlatSegment[];
    shown: FlatSegment[];
  }

  let { all, shown }: Props = $props();

  const SPACING = 10;
  const LENGTH = 3.2;
  const HEAD = 1.1;

  let arrows = $derived(
    shown.flatMap((segment) => {
      const count = Math.max(1, Math.round(segment.curve.length / SPACING));
      return Array.from({ length: count + 1 }, (_, i) => {
        const s = i / count;
        const from = segment.curve.pointAt(s);
        const radians =
          (segmentHeadingAt(all, segment.index, s) * Math.PI) / 180;
        const dx = Math.cos(radians);
        const dy = Math.sin(radians);
        const tip = { x: from.x + dx * LENGTH, y: from.y + dy * LENGTH };
        const base = { x: tip.x - dx * HEAD, y: tip.y - dy * HEAD };
        const wing = { x: -dy * HEAD * 0.5, y: dx * HEAD * 0.5 };
        return {
          key: `${segment.line.id}-${i}`,
          from,
          tip,
          head: `${tip.x},${tip.y} ${base.x + wing.x},${base.y + wing.y} ${base.x - wing.x},${base.y - wing.y}`,
        };
      });
    }),
  );
</script>

{#each arrows as arrow (arrow.key)}
  <g
    stroke="#ffffff"
    stroke-width={0.35}
    fill="#ffffff"
    stroke-linejoin="round"
  >
    <line
      x1={arrow.from.x}
      y1={arrow.from.y}
      x2={arrow.tip.x}
      y2={arrow.tip.y}
    />
    <polygon points={arrow.head} />
  </g>
{/each}
