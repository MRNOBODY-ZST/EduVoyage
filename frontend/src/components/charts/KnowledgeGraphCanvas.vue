<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { DataSet } from 'vis-network/standalone'
import { Network } from 'vis-network/standalone'

import type { GraphView } from '@/types/api'

const props = defineProps<{
  graph: GraphView
  selectedNodeId?: number | string | null
}>()

const emit = defineEmits<{
  selectNode: [id: number | string]
}>()

const container = ref<HTMLElement | null>(null)
let network: Network | null = null

// Node role classification
function classifyNodes() {
  const inDeg = new Map<string, number>()
  const outDeg = new Map<string, number>()
  for (const n of props.graph.nodes) { inDeg.set(String(n.id), 0); outDeg.set(String(n.id), 0) }
  for (const e of props.graph.links) {
    if (e.type === 'PREREQUISITE') {
      outDeg.set(String(e.fromId), (outDeg.get(String(e.fromId)) ?? 0) + 1)
      inDeg.set(String(e.toId), (inDeg.get(String(e.toId)) ?? 0) + 1)
    }
  }
  const roles = new Map<string, 'source' | 'sink' | 'hub' | 'normal'>()
  for (const n of props.graph.nodes) {
    const id = String(n.id)
    const i = inDeg.get(id) ?? 0, o = outDeg.get(id) ?? 0
    if (i === 0 && o > 0)      roles.set(id, 'source')
    else if (o === 0 && i > 0) roles.set(id, 'sink')
    else if (i + o >= 3)       roles.set(id, 'hub')
    else                        roles.set(id, 'normal')
  }
  return roles
}

const ROLE = {
  source:   { bg: '#d1fae5', border: '#059669', hi: '#a7f3d0', font: '#065f46' },
  sink:     { bg: '#fef3c7', border: '#d97706', hi: '#fde68a', font: '#92400e' },
  hub:      { bg: '#ede9fe', border: '#7c3aed', hi: '#ddd6fe', font: '#4c1d95' },
  normal:   { bg: '#f8fafc', border: '#64748b', hi: '#e2e8f0', font: '#334155' },
  selected: { bg: '#dbeafe', border: '#2563eb', hi: '#bfdbfe', font: '#1e40af' },
}

function render() {
  if (!container.value) return
  network?.destroy()

  const roles = classifyNodes()

  const nodes = new DataSet(props.graph.nodes.map((n) => {
    const id = String(n.id)
    const selected = String(props.selectedNodeId) === id
    const role = roles.get(id) ?? 'normal'
    const c = selected ? ROLE.selected : ROLE[role]
    const degree = props.graph.links.filter(
      (e) => String(e.fromId) === id || String(e.toId) === id,
    ).length
    return {
      id,
      label: n.name,
      title: `${n.name}${n.estMinutes ? ` · ${n.estMinutes}min` : ''}`,
      size: selected ? 22 : Math.min(20, 12 + degree * 2),
      color: { background: c.bg, border: c.border, highlight: { background: c.hi, border: c.border } },
      font: { color: c.font, size: 12, bold: selected },
      borderWidth: selected ? 3 : 1.5,
    }
  }))

  const edges = new DataSet(props.graph.links.map((e) => {
    const isPrereq = e.type === 'PREREQUISITE'
    return {
      id: String(e.id),
      from: String(e.fromId),
      to: String(e.toId),
      arrows: isPrereq ? { to: { enabled: true, scaleFactor: 0.7 } } : undefined,
      dashes: !isPrereq,
      color: { color: isPrereq ? '#3b82f6' : '#94a3b8', highlight: isPrereq ? '#1d4ed8' : '#64748b', opacity: 0.85 },
      width: isPrereq ? 2 : 1.5,
      label: isPrereq ? '前置' : '关联',
      font: { size: 10, color: isPrereq ? '#3b82f6' : '#94a3b8', align: 'middle' },
      smooth: { enabled: true, type: 'dynamic' },
    }
  }))

  network = new Network(container.value, { nodes, edges }, {
    physics: {
      solver: 'forceAtlas2Based',
      forceAtlas2Based: {
        gravitationalConstant: -55,
        centralGravity: 0.008,
        springLength: 140,
        springConstant: 0.07,
        damping: 0.45,
        avoidOverlap: 1,
      },
      stabilization: { enabled: true, iterations: 200, updateInterval: 25 },
    },
    nodes: { shape: 'dot' },
    edges: { smooth: { enabled: true, type: 'dynamic', roundness: 0.5 } },
    interaction: { hover: true, tooltipDelay: 150, zoomView: true, dragView: true },
    layout: { improvedLayout: true },
  })

  network.on('click', ({ nodes: clicked }) => {
    if (clicked.length) emit('selectNode', clicked[0])
  })
}

onMounted(() => { nextTick(render) })
watch(() => [props.graph, props.selectedNodeId], () => nextTick(render), { deep: true })
onBeforeUnmount(() => { network?.destroy(); network = null })
</script>

<template>
  <div class="relative">
    <div ref="container" class="h-[500px] w-full overflow-hidden rounded-xl border border-slate-200 bg-white dark:border-white/10 dark:bg-slate-950" />

    <!-- Legend -->
    <div class="absolute bottom-3 left-3 rounded-xl border border-slate-200/80 bg-white/90 px-3 py-2.5 text-xs backdrop-blur-sm dark:border-white/10 dark:bg-slate-900/90">
      <p class="mb-2 font-semibold text-slate-400">图例</p>
      <div class="space-y-1.5">
        <div class="flex items-center gap-2"><span class="size-3 shrink-0 rounded-full border-2 border-emerald-500 bg-emerald-100" /><span class="text-slate-600 dark:text-slate-300">起点节点</span></div>
        <div class="flex items-center gap-2"><span class="size-3 shrink-0 rounded-full border-2 border-amber-500  bg-amber-100"  /><span class="text-slate-600 dark:text-slate-300">终点节点</span></div>
        <div class="flex items-center gap-2"><span class="size-3 shrink-0 rounded-full border-2 border-violet-600 bg-violet-100"/><span class="text-slate-600 dark:text-slate-300">枢纽节点</span></div>
        <div class="flex items-center gap-2"><span class="size-3 shrink-0 rounded-full border-2 border-slate-400  bg-slate-50"   /><span class="text-slate-600 dark:text-slate-300">知识节点</span></div>
        <div class="my-1 h-px bg-slate-100 dark:bg-white/10" />
        <div class="flex items-center gap-2"><span class="h-0.5 w-5 shrink-0 rounded-full bg-blue-500" /><span class="text-slate-600 dark:text-slate-300">前置关系</span></div>
        <div class="flex items-center gap-2">
          <svg class="w-5 shrink-0" height="3" viewBox="0 0 20 2" aria-hidden="true"><line x1="0" y1="1" x2="20" y2="1" stroke="#94a3b8" stroke-width="1.5" stroke-dasharray="4 3" /></svg>
          <span class="text-slate-600 dark:text-slate-300">关联关系</span>
        </div>
      </div>
    </div>

    <div class="absolute bottom-3 right-3 rounded-xl border border-slate-200/80 bg-white/90 px-2.5 py-1.5 text-xs text-slate-400 backdrop-blur-sm dark:border-white/10 dark:bg-slate-900/90">
      滚轮缩放 · 拖拽平移
    </div>
  </div>
</template>
