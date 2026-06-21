<script setup lang="ts">
import { computed } from 'vue'
import { Listbox, ListboxButton, ListboxOption, ListboxOptions } from '@headlessui/vue'
import { CheckIcon, ChevronUpDownIcon, MoonIcon, PaintBrushIcon, SunIcon } from '@heroicons/vue/20/solid'

import { brandThemes, useSettingsStore } from '@/stores/settings'

const settings = useSettingsStore()

const modeOptions = [
  { value: 'auto', label: '跟随系统', icon: PaintBrushIcon },
  { value: 'light', label: '浅色', icon: SunIcon },
  { value: 'dark', label: '深色', icon: MoonIcon },
] as const

const selectedMode = computed({
  get: () => modeOptions.find((option) => option.value === settings.mode) || modeOptions[0],
  set: (option: (typeof modeOptions)[number]) => settings.setMode(option.value),
})
</script>

<template>
  <div class="flex items-center gap-2">
    <Listbox v-model="selectedMode">
      <div class="relative">
        <ListboxButton
          class="focus-ring inline-flex h-9 items-center gap-2 rounded-md border border-slate-200 bg-white px-2.5 text-sm text-slate-700 shadow-sm hover:bg-slate-50 dark:border-white/10 dark:bg-white/5 dark:text-slate-200 dark:hover:bg-white/10"
          title="主题模式"
        >
          <component :is="selectedMode.icon" class="size-4 text-slate-500 dark:text-slate-300" aria-hidden="true" />
          <span class="hidden sm:inline">{{ selectedMode.label }}</span>
          <ChevronUpDownIcon class="size-4 text-slate-400" aria-hidden="true" />
        </ListboxButton>
        <ListboxOptions
          class="absolute right-0 z-20 mt-2 w-36 rounded-md bg-white py-1 shadow-lg outline-1 outline-slate-900/5 dark:bg-slate-800 dark:outline-white/10"
        >
          <ListboxOption v-for="option in modeOptions" :key="option.value" v-slot="{ active, selected }" :value="option">
            <div
              :class="[
                active ? 'bg-slate-50 dark:bg-white/5' : '',
                'flex cursor-pointer items-center gap-2 px-3 py-2 text-sm text-slate-700 dark:text-slate-100',
              ]"
            >
              <component :is="option.icon" class="size-4 text-slate-400" aria-hidden="true" />
              <span class="flex-1">{{ option.label }}</span>
              <CheckIcon v-if="selected" class="size-4 text-[rgb(var(--color-brand))]" aria-hidden="true" />
            </div>
          </ListboxOption>
        </ListboxOptions>
      </div>
    </Listbox>

    <div class="hidden items-center gap-1 md:flex">
      <button
        v-for="theme in brandThemes"
        :key="theme.key"
        type="button"
        class="focus-ring size-5 rounded-full border border-white shadow-sm ring-1 ring-slate-200 dark:ring-white/15"
        :class="settings.brand === theme.key ? 'outline outline-2 outline-offset-2 outline-[rgb(var(--color-brand))]' : ''"
        :style="{ backgroundColor: `rgb(${theme.light})` }"
        :title="theme.label"
        @click="settings.setBrand(theme.key)"
      >
        <span class="sr-only">{{ theme.label }}</span>
      </button>
    </div>
  </div>
</template>
