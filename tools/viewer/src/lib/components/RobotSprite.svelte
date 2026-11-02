<script lang="ts">
  import type { BasePoint } from "../../types";

  interface Props {
    xy: BasePoint;
    heading: number;
    widthPx: number;
    heightPx: number;
    src?: string;
    opacity?: number;
    onImageSettled?: () => void;
  }

  const DEFAULT_ROBOT_IMAGE = "/robot.png";

  let {
    xy,
    heading,
    widthPx,
    heightPx,
    src = DEFAULT_ROBOT_IMAGE,
    opacity = 1,
    onImageSettled = () => {},
  }: Props = $props();
</script>

<img
  {src}
  alt="Robot"
  style={`position: absolute; top: ${xy.y}px; left: ${xy.x}px; transform: translate(-50%, -50%) rotate(${heading}deg); z-index: 20; width: ${widthPx}px; height: ${heightPx}px; user-select: none; pointer-events: none; opacity: ${opacity};`}
  draggable="false"
  onload={onImageSettled}
  onerror={(event) => {
    onImageSettled();
    (event.currentTarget as HTMLImageElement).src = DEFAULT_ROBOT_IMAGE;
  }}
/>
