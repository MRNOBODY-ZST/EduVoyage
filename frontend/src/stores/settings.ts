import { defineStore } from 'pinia'

type ThemeMode = 'light' | 'dark' | 'auto'

interface BrandTheme {
  key: string
  label: string
  light: string
  dark: string
  strongLight: string
  strongDark: string
  softLight: string
  softDark: string
}

const MODE_KEY = 'eduvoyage.theme.mode'
const BRAND_KEY = 'eduvoyage.theme.brand'

export const brandThemes: BrandTheme[] = [
  {
    key: 'ocean',
    label: '海洋蓝',
    light: '37 99 235',
    dark: '59 130 246',
    strongLight: '29 78 216',
    strongDark: '96 165 250',
    softLight: '239 246 255',
    softDark: '30 41 59',
  },
  {
    key: 'teal',
    label: '青碧',
    light: '13 148 136',
    dark: '20 184 166',
    strongLight: '15 118 110',
    strongDark: '45 212 191',
    softLight: '240 253 250',
    softDark: '19 78 74',
  },
  {
    key: 'indigo',
    label: '靛紫',
    light: '79 70 229',
    dark: '99 102 241',
    strongLight: '67 56 202',
    strongDark: '129 140 248',
    softLight: '238 242 255',
    softDark: '30 41 59',
  },
  {
    key: 'emerald',
    label: '翠绿',
    light: '5 150 105',
    dark: '16 185 129',
    strongLight: '4 120 87',
    strongDark: '52 211 153',
    softLight: '236 253 245',
    softDark: '20 83 45',
  },
  {
    key: 'rose',
    label: '玫红',
    light: '225 29 72',
    dark: '244 63 94',
    strongLight: '190 18 60',
    strongDark: '251 113 133',
    softLight: '255 241 242',
    softDark: '76 29 43',
  },
]

export const useSettingsStore = defineStore('settings', {
  state: () => ({
    mode: (localStorage.getItem(MODE_KEY) as ThemeMode) || 'auto',
    brand: localStorage.getItem(BRAND_KEY) || 'ocean',
  }),
  getters: {
    brandTheme: (state) => brandThemes.find((theme) => theme.key === state.brand) || brandThemes[0]!,
  },
  actions: {
    setMode(mode: ThemeMode) {
      this.mode = mode
      localStorage.setItem(MODE_KEY, mode)
      this.applyTheme()
    },
    setBrand(brand: string) {
      this.brand = brand
      localStorage.setItem(BRAND_KEY, brand)
      this.applyTheme()
    },
    applyTheme() {
      const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches
      const dark = this.mode === 'dark' || (this.mode === 'auto' && prefersDark)
      document.documentElement.classList.toggle('dark', dark)

      const brand = this.brandTheme
      document.documentElement.style.setProperty('--color-brand', dark ? brand.dark : brand.light)
      document.documentElement.style.setProperty('--color-brand-strong', dark ? brand.strongDark : brand.strongLight)
      document.documentElement.style.setProperty('--color-brand-soft', dark ? brand.softDark : brand.softLight)
    },
  },
})
