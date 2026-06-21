---
name: frontend-tailwind-css
description: Build polished frontend interfaces with Tailwind CSS using a bundled Vue + Tailwind 4.2 component library. Use when Codex needs to create, redesign, or extend landing pages, dashboards, SaaS app shells, forms, tables, navigation, overlays, ecommerce pages, or adapt curated Tailwind UI blocks into Vue, React, HTML, Next.js, Nuxt, or other frontend projects with responsive, accessible, dark-mode-aware styling.
---

# Frontend Tailwind CSS

## Overview

Use this skill to build production-quality Tailwind CSS frontends from a curated library of 657 Vue + Tailwind 4.2 blocks. Treat the bundled components as high-quality starting points, then adapt them to the target framework, data model, visual language, and existing codebase conventions.

## Bundled Library

- `assets/tailwindui_template/catalog.json`: full machine-readable index of products, categories, components, metadata, and file paths.
- `assets/tailwindui_template/DIRECTORY_OVERVIEW.md`: concise category inventory.
- `assets/tailwindui_template/AI_TEMPLATE_GUIDE.md`: original retrieval guide.
- `assets/tailwindui_template/templates/`: component source files. Each component normally has `.vue`, `.preview.html`, and `.meta.json` files.
- `scripts/tailwind_templates.py`: search, inspect, and copy components without loading the whole catalog into context.
- `scripts/download_tailwindui_blocks.py`: optional library refresh utility. Use only when the user explicitly asks to recrawl or update the bundled templates.
- `references/template-library.md`: quick map from user intent to product/category choices.

## Workflow

1. Read the target app first. Identify framework, routing, styling conventions, Tailwind version, icon library, UI primitives, data shape, and existing dark-mode strategy.
2. Map the request to a product:
   - `marketing`: landing pages, hero sections, pricing, CTAs, testimonials, blog sections, headers, footers.
   - `application-ui`: dashboards, app shells, navbars, forms, tables, lists, stats, modals, drawers, notifications.
   - `ecommerce`: product pages, carts, checkout, filters, reviews, order history. These snippets are not marked target-compliant, so audit dark mode and theme behavior before using them.
3. Search templates with the bundled script. Prefer `target_compliant=true` when the user wants dark mode, system theme, or modern Tailwind 4.2 behavior.
4. Copy or read the selected component, then adapt it. Replace demo data, placeholder links, external image URLs, fake logos, and sample copy. Bind real props, state, routes, actions, and API data.
5. Convert framework details carefully. The source snippets are Vue; when targeting React, Next.js, Svelte, plain HTML, or another framework, translate state, loops, conditionals, events, and Headless UI primitives idiomatically.
6. Verify locally. Run the relevant build, lint, tests, or typecheck. For visible frontend changes, start the dev server and inspect desktop and mobile layouts, plus light and dark mode when applicable.

## Template Commands

Run commands from the skill directory or pass paths explicitly.

```bash
python scripts/tailwind_templates.py stats
python scripts/tailwind_templates.py list --product application-ui
python scripts/tailwind_templates.py search --query "checkbox table" --product application-ui --target-compliant
python scripts/tailwind_templates.py show --path templates/application-ui/lists/tables/17__with-checkboxes.vue
python scripts/tailwind_templates.py show --path templates/marketing/page-sections/hero-sections/01__simple-centered.vue --code
python scripts/tailwind_templates.py copy --path templates/application-ui/lists/tables/17__with-checkboxes.vue --out ./src/components --name UsersTable.vue
```

Use `search --json` when another script or tool needs structured output.

## Adaptation Rules

- Preserve the useful structure and spacing rhythm, but do not leave generic Tailwind UI demo content in the final product.
- Prefer the host project's component primitives and icon library. If the project already uses Lucide, Radix, shadcn/ui, Headless UI, or a local design system, adapt the snippet to those conventions.
- Install or map dependencies only when needed. Vue snippets commonly reference `@headlessui/vue` and `@heroicons/vue/24/outline`.
- Audit copied Vue snippets for JSX leftovers such as `className`, `htmlFor`, or self-closing non-component HTML where the target compiler dislikes it.
- Check Tailwind compatibility. The library uses Tailwind 4-era utilities such as `size-*`, `text-balance`, `text-pretty`, `shadow-xs`, `bg-linear-to-*`, `inset-ring`, `not-dark:*`, `group-has-*`, and `has-*`. Backport or replace these classes for older Tailwind projects.
- Keep accessibility attributes intact: `aria-label`, `sr-only`, focus states, dialog semantics, keyboard navigation, and table/list semantics.
- Make data-heavy app screens dense and scannable. Make marketing pages visually rich only when the request actually calls for a landing or brand experience.
- Replace decorative placeholders with relevant real assets or generated bitmap assets when the interface depends on imagery.
- Avoid nested cards, fragile width assumptions, overflow-prone text, and one-color palettes. Check mobile and wide desktop before finishing.

## Selection Heuristics

- Start broad with `list`, then use `search` terms from the user's nouns: "pricing", "modal", "table", "checkout", "settings", "sidebar", "hero", "stats".
- Prefer page examples when the user asks for a complete screen. Prefer components when integrating into an existing app.
- For a full SaaS app, combine an application shell, page heading, stats, table/list, form controls, and overlay/dialog templates.
- For a landing page, assemble header, hero, feature section, social proof, pricing or CTA, FAQ, and footer templates.
- For ecommerce, combine storefront/product/category/cart/checkout templates, then add dark-mode support manually if the target app requires it.

## Validation Checklist

- The app builds without Tailwind class or framework syntax errors.
- Interactive elements use real state and handlers, not dead placeholders.
- Desktop, tablet, and mobile layouts do not overlap, clip important content, or shift unexpectedly.
- Light and dark mode are visually coherent if the project supports them.
- Forms, dialogs, menus, tables, and navigation remain keyboard accessible.
- The final implementation follows the user's product domain instead of looking like a generic template dump.
