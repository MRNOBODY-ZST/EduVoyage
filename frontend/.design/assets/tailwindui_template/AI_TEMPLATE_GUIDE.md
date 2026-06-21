# AI Template Guide

## Scope
- Source: `https://tailwindui.starxg.com/plus/ui-blocks`
- Products: `marketing`, `application-ui`, `ecommerce`
- Framework filter: `vue`
- Theme mode filter: `system` (auto theme switch)
- Dark mode support required: `True`
- Tailwind version required: `4.2`

## How to map user intent to templates
- Step 1: choose product by business context (landing page/app backend/ecommerce).
- Step 2: choose subcategory by UI purpose (hero, table, modal, checkout, etc.).
- Step 3: pick component variant by visual style and interaction density.
- Step 4: reuse .vue snippet as base and bind real data/API.

## Output schema
- `templates/<product>/<group>/<subcategory>/<NN>__<component>.vue`
- `templates/<product>/<group>/<subcategory>/<NN>__<component>.preview.html`
- `templates/<product>/<group>/<subcategory>/<NN>__<component>.meta.json`

## Product overview
- `marketing`: 23 subcategories, 179 components, 179 target-compliant
- `application-ui`: 49 subcategories, 364 components, 364 target-compliant
- `ecommerce`: 21 subcategories, 114 components, 0 target-compliant

## Suggested retrieval strategy for AI
- Prefer matching by `product + group_name + subcategory_name + component_name`.
- If user asks for auto dark mode/theme switching, filter `target_compliant=true`.
- All exported snippets are Vue + Tailwind 4.2.
- Use `target_compliant=true` to pick auto-theme capable templates first.
