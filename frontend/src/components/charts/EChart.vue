<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { BarChart, LineChart } from 'echarts/charts'
import {
  GridComponent,
  TooltipComponent,
} from 'echarts/components'
import { init, use, type ECharts, type EChartsOption } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'

use([BarChart, LineChart, GridComponent, TooltipComponent, CanvasRenderer])

const props = defineProps<{
  option: EChartsOption
  height?: string
}>()

const chartEl = ref<HTMLElement | null>(null)
let chart: ECharts | null = null
let observer: ResizeObserver | null = null

function render() {
  if (!chartEl.value) {
    return
  }
  chart ??= init(chartEl.value)
  chart.setOption(props.option, true)
}

onMounted(async () => {
  await nextTick()
  render()
  if (chartEl.value) {
    observer = new ResizeObserver(() => chart?.resize())
    observer.observe(chartEl.value)
  }
})

watch(
  () => props.option,
  () => render(),
  { deep: true },
)

onBeforeUnmount(() => {
  observer?.disconnect()
  chart?.dispose()
  chart = null
})
</script>

<template>
  <div ref="chartEl" class="w-full" :style="{ height: height || '280px' }" />
</template>
