import type { App, DirectiveBinding } from 'vue'

import { useAuthStore } from '@/stores/auth'

function allowed(value: string | string[] | undefined) {
  if (!value) {
    return true
  }
  const auth = useAuthStore()
  const permissions = Array.isArray(value) ? value : [value]
  return permissions.some((permission) => auth.hasPermission(permission))
}

export function installPermissionDirective(app: App) {
  app.directive('permission', {
    mounted(el: HTMLElement, binding: DirectiveBinding<string | string[]>) {
      if (!allowed(binding.value)) {
        el.remove()
      }
    },
    updated(el: HTMLElement, binding: DirectiveBinding<string | string[]>) {
      el.style.display = allowed(binding.value) ? '' : 'none'
    },
  })
}
