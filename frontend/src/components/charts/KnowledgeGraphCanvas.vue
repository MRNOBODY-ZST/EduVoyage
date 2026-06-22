<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { Graph } from '@antv/g6'

import type { GraphView } from '@/types/api'

const props = defineProps<{
  graph: GraphView
  selectedNodeId?: number | string | null
}>()

const emit = defineEmits<{
  selectNode: [id: number | string]
}>()

const container = ref<HTMLElement | null>(null)
let graphInstance: InstanceType<typeof Graph> | null = null
let observer: ResizeObserver | null = null

function data() {
  return {
    nodes: props.graph.nodes.map((node, index) => ({
      id: String(node.id),
      data: node,
      style: {
        x: node.posX ?? 120 + (index % 4) * 180,
        y: node.posY ?? 90 + Math.floor(index / 4) * 120,
        labelText: node.name,
        labelPlacement: 'bottom',
        size: props.selectedNodeId === node.id ? 42 : 34,
        fill: props.selectedNodeId === node.id ? '#2563eb' : '#f8fafc',
        stroke: props.selectedNodeId === node.id ? '#1d4ed8' : '#94a3b8',
        lineWidth: props.selectedNodeId === node.id ? 2 : 1,
      },
    })),
    edges: props.graph.links.map((edge) => ({
      id: String(edge.id),
      source: String(edge.fromId),
      target: String(edge.toId),
      data: edge,
      style: {
        endArrow: true,
        stroke: edge.type === 'PREREQUISITE' ? '#2563eb' : '#94a3b8',
        lineDash: edge.type === 'RELATED' ? [4, 4] : undefined,
        labelText: edge.type === 'PREREQUISITE' ? '前置' : '关联',
      },
    })),
  }
}

async function render() {
  if (!container.value) {
    return
  }
  await nextTick()
  graphInstance?.destroy()
  graphInstance = new Graph({
    container: container.value,
    width: container.value.clientWidth,
    height: container.value.clientHeight,
    autoFit: 'view',
    data: data(),
    node: {
      type: 'circle',
      style: {
        labelFontSize: 12,
        labelFill: '#334155',
        cursor: 'pointer',
      },
    },
    edge: {
      type: 'line',
      style: {
        labelFontSize: 11,
        labelFill: '#64748b',
      },
    },
    layout: props.graph.nodes.some((node) => node.posX != null && node.posY != null)
      ? undefined
      : {
          type: 'dagre',
          rankdir: 'LR',
          nodesep: 36,
          ranksep: 72,
        },
    behaviors: ['drag-canvas', 'zoom-canvas', 'drag-element'],
  })
  graphInstance.on('node:click', (event: { target?: { id?: string } }) => {
    const id = event.target?.id
    if (id) {
      emit('selectNode', id)
    }
  })
  await graphInstance.render()
}

onMounted(() => {
  render()
  if (container.value) {
    observer = new ResizeObserver(() => render())
    observer.observe(container.value)
  }
})

watch(
  () => [props.graph, props.selectedNodeId],
  () => render(),
  { deep: true },
)

onBeforeUnmount(() => {
  observer?.disconnect()
  graphInstance?.destroy()
  graphInstance = null
})
</script>

<template>
  <div ref="container" class="h-[420px] w-full overflow-hidden rounded-md border border-slate-200 bg-white dark:border-white/10 dark:bg-slate-950" />
</template>
