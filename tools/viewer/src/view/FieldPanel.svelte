<script lang="ts">
  import { FIELD_SIZE, type ColorScheme } from "../config/defaults";
  import { robotConfigOf } from "../io";
  import { DEFAULT_SETTINGS } from "../config";
  import { generateGhostPathPoints, generateOnionLayers } from "../model/sweep";
  import { flattenToAtomicSegments } from "../utils/pathTraversal";
  import { useFeed, useOptions, usePlayback } from "../state/viewer.svelte";
  import { surfaceClass } from "../lib/components/ui/styles";
  import FieldMapImage from "../lib/components/FieldMapImage.svelte";
  import FieldLoadingOverlay from "../lib/components/FieldLoadingOverlay.svelte";
  import RobotSprite from "../lib/components/RobotSprite.svelte";
  import PathStrokes from "./scene/PathStrokes.svelte";
  import PathMarkers from "./scene/PathMarkers.svelte";
  import HeadingArrows from "./scene/HeadingArrows.svelte";
  import PlaybackBar from "./PlaybackBar.svelte";
  import { polylinePath } from "./scene/svgPath";
  import { FIELD_POINT_COLOR, GHOST_COLOR } from "./scene/colors";
  import { fieldMapSource } from "./fieldMap";
  import { createViewport } from "./viewport";

  interface Props {
    scheme: ColorScheme;
  }

  let { scheme }: Props = $props();

  const feed = useFeed();
  const options = useOptions();
  const playback = usePlayback();

  const MIN_ZOOM = 1;
  const MAX_ZOOM = 12;
  let view = $state({ zoom: 1, panX: 0, panY: 0 });
  let panning: { x: number; y: number } | null = $state(null);

  let stageWidth = $state(FIELD_SIZE);
  let stageHeight = $state(FIELD_SIZE);
  let pixels = $derived(
    Math.max(1, Math.floor(Math.min(stageWidth, stageHeight) - 16)),
  );
  let viewport = $derived(createViewport(pixels));
  let { x, y } = $derived(viewport);

  let doc = $derived(feed.doc);
  let robot = $derived(doc?.robot ?? robotConfigOf(DEFAULT_SETTINGS));
  let allSegments = $derived(
    doc ? flattenToAtomicSegments(doc.startPoint, doc.lines) : [],
  );
  let shownSegments = $derived(
    allSegments.filter(
      (segment) => !options.hiddenPathIds.has(segment.line.id),
    ),
  );
  let ghost = $derived(
    doc && options.showSwept
      ? generateGhostPathPoints(doc.startPoint, doc.lines, robot.rWidth, 50)
      : [],
  );
  let footprints = $derived(
    doc && options.showFootprints
      ? generateOnionLayers(
          doc.startPoint,
          doc.lines,
          robot.rLength,
          robot.rWidth,
          Math.max(1, options.footprintSpacing),
        ).filter((layer) => !options.hiddenPathIds.has(layer.lineId))
      : [],
  );

  let sample = $derived(playback.sample);
  let trace = $derived(
    feed.sim && options.showTrace
      ? feed.sim.samples.x.map((px, i) => ({
          x: px,
          y: feed.sim!.samples.y[i],
        }))
      : [],
  );

  let fieldMapSrc = $derived(
    fieldMapSource({ ...DEFAULT_SETTINGS, fieldMap: options.fieldMap }, scheme),
  );
  let loadedFieldMap = $state("");
  let loadedRobotImage = $state("");
  const robotImageSrc = "/robot.png";
  let assetsReady = $derived(
    loadedFieldMap === fieldMapSrc && loadedRobotImage === robotImageSrc,
  );

  function onWheel(event: WheelEvent) {
    event.preventDefault();
    const zoom = Math.min(
      MAX_ZOOM,
      Math.max(MIN_ZOOM, view.zoom * Math.exp(-event.deltaY * 0.002)),
    );
    const rect = (event.currentTarget as HTMLElement)
      .querySelector("[data-field]")!
      .getBoundingClientRect();
    const ratio = zoom / view.zoom;
    const centerX = rect.left + rect.width / 2;
    const centerY = rect.top + rect.height / 2;
    view = {
      zoom,
      panX: view.panX + (event.clientX - centerX) * (1 - ratio),
      panY: view.panY + (event.clientY - centerY) * (1 - ratio),
    };
    if (zoom === MIN_ZOOM) resetView();
  }

  function onPointerDown(event: PointerEvent) {
    if (event.button !== 0) return;
    panning = { x: event.clientX - view.panX, y: event.clientY - view.panY };
    (event.currentTarget as Element).setPointerCapture(event.pointerId);
  }

  function onPointerMove(event: PointerEvent) {
    if (!panning) return;
    view = {
      ...view,
      panX: event.clientX - panning.x,
      panY: event.clientY - panning.y,
    };
  }

  function resetView() {
    view = { zoom: 1, panX: 0, panY: 0 };
  }
</script>

<main class={[surfaceClass, "flex min-h-0 flex-col gap-2 p-2"]}>
  <!-- svelte-ignore a11y_no_static_element_interactions -->
  <div
    class={[
      "relative flex min-h-0 flex-1 items-center justify-center overflow-hidden rounded-xl bg-body/10",
      panning ? "cursor-grabbing" : "cursor-grab",
    ]}
    bind:clientWidth={stageWidth}
    bind:clientHeight={stageHeight}
    onwheel={onWheel}
    onpointerdown={onPointerDown}
    onpointermove={onPointerMove}
    onpointerup={() => (panning = null)}
    onlostpointercapture={() => (panning = null)}
    ondblclick={resetView}
  >
    <div
      data-field
      class="relative overflow-clip rounded-lg bg-base select-none"
      role="img"
      aria-label="Field with exported path"
      style={`width: ${pixels}px; height: ${pixels}px; transform: translate(${view.panX}px, ${view.panY}px) scale(${view.zoom});`}
    >
      <FieldMapImage
        src={fieldMapSrc}
        fieldMapName={options.fieldMap}
        onSettled={() => (loadedFieldMap = fieldMapSrc)}
      />

      {#if doc}
        <svg
          class="pointer-events-none absolute inset-0 z-15"
          viewBox={`0 0 ${FIELD_SIZE} ${FIELD_SIZE}`}
          width={pixels}
          height={pixels}
          aria-hidden="true"
        >
          <g transform={`matrix(1 0 0 -1 0 ${FIELD_SIZE})`}>
            {#each doc.fieldPoints as point, i (i)}
              <circle
                cx={point.x}
                cy={point.y}
                r={(Math.max(0.6, point.radius ?? 2.2) * FIELD_SIZE) / pixels}
                fill={point.color || FIELD_POINT_COLOR}
                opacity={Math.max(0, Math.min(1, point.opacity ?? 1))}
              />
            {/each}
          </g>
        </svg>

        {#if sample}
          <RobotSprite
            xy={{ x: x(sample.x), y: y(sample.y) }}
            heading={-(sample.heading * 180) / Math.PI}
            widthPx={x(robot.rLength)}
            heightPx={x(robot.rWidth)}
            src={robotImageSrc}
            onImageSettled={() => (loadedRobotImage = robotImageSrc)}
          />
        {:else if options.showRobotAtStart}
          <RobotSprite
            xy={{ x: x(doc.startPoint.x), y: y(doc.startPoint.y) }}
            heading={-doc.startPoint.headingDeg}
            widthPx={x(robot.rLength)}
            heightPx={x(robot.rWidth)}
            src={robotImageSrc}
            onImageSettled={() => (loadedRobotImage = robotImageSrc)}
          />
        {/if}

        <svg
          class="pointer-events-none absolute inset-0 z-30"
          viewBox={`0 0 ${FIELD_SIZE} ${FIELD_SIZE}`}
          width={pixels}
          height={pixels}
          aria-hidden="true"
        >
          <g transform={`matrix(1 0 0 -1 0 ${FIELD_SIZE})`}>
            {#if ghost.length >= 3}
              <path
                d={polylinePath(ghost, true)}
                fill={GHOST_COLOR}
                stroke={GHOST_COLOR}
                stroke-width={0.5}
                opacity={0.15}
              />
            {/if}
            {#each footprints as layer, index (index)}
              <path
                d={polylinePath(layer.corners, true)}
                fill="none"
                stroke="#dc2626"
                stroke-width={0.28}
                opacity={0.9}
              />
            {/each}
            {#if trace.length > 1}
              <path
                d={polylinePath(trace, false)}
                fill="none"
                stroke="#f59e0b"
                stroke-width={0.5}
                stroke-dasharray="1.5 1"
                opacity={0.9}
              />
            {/if}
            <PathStrokes segments={shownSegments} />
            {#if options.showHeadings}
              <HeadingArrows all={allSegments} shown={shownSegments} />
            {/if}
            <PathMarkers
              startPoint={doc.startPoint}
              segments={shownSegments}
              showControlPoints={options.showControlPoints}
            />
          </g>
        </svg>
      {/if}

      {#if !assetsReady}
        <FieldLoadingOverlay />
      {/if}
    </div>
  </div>
  <PlaybackBar />
</main>
