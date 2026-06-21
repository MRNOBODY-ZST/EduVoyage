# Tailwind UI Blocks Downloader

# The Main Skill is under the frontend-tailwind-css directory

## What this does

- Crawls `marketing`, `application-ui`, `ecommerce` from `https://tailwindui.starxg.com/plus/ui-blocks`
- Downloads all `Vue` snippets under `Tailwind 4.2` pages
- Keeps original category structure
- Marks each template as `target_compliant` when it supports:
  - auto theme mode (`system`)
  - dark mode support (`supportsDarkMode=true`)
- Generates:
  - `tailwindui_export/DIRECTORY_OVERVIEW.md`
  - `tailwindui_export/AI_TEMPLATE_GUIDE.md`
  - `tailwindui_export/catalog.json`

## Run

```bash
python download_tailwindui_blocks.py
```

## Useful options

```bash
# output to custom directory
python download_tailwindui_blocks.py --out-dir my_export

# include archived components
python download_tailwindui_blocks.py --include-archived

# strict mode: only keep auto-theme + dark-mode templates
python download_tailwindui_blocks.py --strict-target-only
```

## Output structure

```text
tailwindui_export/
  templates/
    <product>/
      <group>/
        <subcategory>/
          01__<component>.vue
          01__<component>.preview.html
          01__<component>.meta.json
  DIRECTORY_OVERVIEW.md
  AI_TEMPLATE_GUIDE.md
  AI_TEMPLATE_GUIDE.json
  catalog.json
```
