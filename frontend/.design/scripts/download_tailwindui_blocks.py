#!/usr/bin/env python3
"""
Download Tailwind Plus UI blocks from tailwindui.starxg.com.

Goals:
1) Crawl three products: marketing / application-ui / ecommerce
2) Keep original category structure
3) Filter snippets for:
   - Tailwind 4.2
   - Vue framework
   - Auto theme mode (system) + supports dark mode
4) Save .vue code files, preview html, and metadata
5) Generate:
   - overall directory report
   - AI-friendly template guide (JSON + Markdown)
"""

from __future__ import annotations

import argparse
import json
import re
import sys
import time
from dataclasses import dataclass
from html.parser import HTMLParser
from pathlib import Path
from typing import Any
from urllib.error import HTTPError, URLError
from urllib.parse import urljoin
from urllib.request import Request, urlopen


BASE_URL = "https://tailwindui.starxg.com"
PRODUCT_PATHS = ["marketing", "application-ui", "ecommerce"]
USER_AGENT = (
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
    "AppleWebKit/537.36 (KHTML, like Gecko) "
    "Chrome/123.0.0.0 Safari/537.36"
)


class AppDivParser(HTMLParser):
    """Extract #app data-page JSON from server-rendered HTML."""

    def __init__(self) -> None:
        super().__init__()
        self.data_page: str | None = None

    def handle_starttag(self, tag: str, attrs: list[tuple[str, str | None]]) -> None:
        if self.data_page is not None or tag != "div":
            return
        d = dict(attrs)
        if d.get("id") == "app" and d.get("data-page"):
            self.data_page = d["data-page"]


@dataclass
class SnippetFilter:
    tailwind_version: str = "4.2"
    framework: str = "vue"
    mode: str = "system"
    require_dark_mode: bool = True
    strict_target_only: bool = False
    include_archived: bool = False


def slugify(value: str) -> str:
    value = value.strip().lower()
    value = re.sub(r"[^\w\- ]+", "", value, flags=re.UNICODE)
    value = re.sub(r"[\s\-]+", "-", value)
    return value.strip("-") or "untitled"


def fetch_html(url: str, retries: int = 3, sleep_sec: float = 1.0) -> str:
    last_error: Exception | None = None
    for attempt in range(1, retries + 1):
        req = Request(url, headers={"User-Agent": USER_AGENT})
        try:
            with urlopen(req, timeout=30) as resp:
                return resp.read().decode("utf-8", errors="replace")
        except (HTTPError, URLError, TimeoutError) as exc:
            last_error = exc
            if attempt < retries:
                time.sleep(sleep_sec * attempt)
    raise RuntimeError(f"Failed to fetch {url}: {last_error}")


def extract_data_page_json(html_text: str) -> dict[str, Any]:
    parser = AppDivParser()
    parser.feed(html_text)
    if not parser.data_page:
        raise ValueError("Cannot find #app[data-page] in HTML.")
    return json.loads(parser.data_page)


def unique_by_url(items: list[dict[str, Any]]) -> list[dict[str, Any]]:
    seen: set[str] = set()
    out: list[dict[str, Any]] = []
    for item in items:
        url = item.get("url_path") or item.get("url") or ""
        if not url or url in seen:
            continue
        seen.add(url)
        out.append(item)
    return out


def collect_subcategories_from_product_page(product_path: str) -> list[dict[str, Any]]:
    url = f"{BASE_URL}/plus/ui-blocks/{product_path}"
    html_text = fetch_html(url)
    data = extract_data_page_json(html_text)
    props = data.get("props", {})
    product = props.get("product", {})
    categories = product.get("categories", [])

    subcategories: list[dict[str, Any]] = []
    for group in categories:
        group_name = group.get("name", "Unknown Group")
        for sub in group.get("subcategories", []):
            sub_url = sub.get("url")
            if not sub_url:
                continue
            subcategories.append(
                {
                    "product": product_path,
                    "group_name": group_name,
                    "name": sub.get("name", "Unknown"),
                    "url_path": sub_url,
                    "absolute_url": urljoin(BASE_URL, sub_url),
                }
            )

    return unique_by_url(subcategories)


def should_keep_component(
    component: dict[str, Any],
    page_tailwind_version: str,
    wanted: SnippetFilter,
) -> tuple[bool, bool, str]:
    snippet = component.get("snippet") or {}
    if not snippet:
        return False, False, "missing snippet"

    if not wanted.include_archived and component.get("archived"):
        return False, False, "archived"

    if (snippet.get("name") or "").lower() != wanted.framework.lower():
        return False, False, f"framework={snippet.get('name')}"

    # Site exposes full version in page props (e.g. 4.2),
    # snippet may expose major-only numeric version (e.g. 4).
    if page_tailwind_version != wanted.tailwind_version:
        return False, False, f"tailwindVersion={page_tailwind_version}"

    mode_ok = (snippet.get("mode") or "").lower() == wanted.mode.lower()
    dark_ok = (not wanted.require_dark_mode) or bool(snippet.get("supportsDarkMode"))
    target_compliant = mode_ok and dark_ok

    if wanted.strict_target_only and not target_compliant:
        return False, target_compliant, "not target compliant"

    return True, target_compliant, "ok"


def write_text(path: Path, content: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content, encoding="utf-8")


def build_tree_markdown(index: dict[str, Any]) -> str:
    lines = ["# Downloaded Tailwind UI Blocks Directory", ""]
    lines.append(f"- Total products: {index['stats']['products']}")
    lines.append(f"- Total subcategories: {index['stats']['subcategories']}")
    lines.append(f"- Total components kept: {index['stats']['components_kept']}")
    lines.append(
        f"- Target-compliant components: {index['stats']['components_target_compliant']}"
    )
    lines.append("")

    for product in index["products"]:
        lines.append(f"## {product['product']}")
        lines.append(
            f"- Subcategories: {product['stats']['subcategories']} | "
            f"Components: {product['stats']['components_kept']} | "
            f"Target-compliant: {product['stats']['components_target_compliant']}"
        )
        lines.append("")

        for sub in product["subcategories"]:
            lines.append(
                f"- `{sub['group_name']}/{sub['subcategory_name']}` -> {sub['components_kept']} component(s)"
            )
        lines.append("")
    return "\n".join(lines).rstrip() + "\n"


def build_ai_guide_markdown(index: dict[str, Any], wanted: SnippetFilter) -> str:
    lines = [
        "# AI Template Guide",
        "",
        "## Scope",
        f"- Source: `{BASE_URL}/plus/ui-blocks`",
        "- Products: `marketing`, `application-ui`, `ecommerce`",
        f"- Framework filter: `{wanted.framework}`",
        f"- Theme mode filter: `{wanted.mode}` (auto theme switch)",
        f"- Dark mode support required: `{wanted.require_dark_mode}`",
        f"- Tailwind version required: `{wanted.tailwind_version}`",
        "",
        "## How to map user intent to templates",
        "- Step 1: choose product by business context (landing page/app backend/ecommerce).",
        "- Step 2: choose subcategory by UI purpose (hero, table, modal, checkout, etc.).",
        "- Step 3: pick component variant by visual style and interaction density.",
        "- Step 4: reuse .vue snippet as base and bind real data/API.",
        "",
        "## Output schema",
        "- `templates/<product>/<group>/<subcategory>/<NN>__<component>.vue`",
        "- `templates/<product>/<group>/<subcategory>/<NN>__<component>.preview.html`",
        "- `templates/<product>/<group>/<subcategory>/<NN>__<component>.meta.json`",
        "",
        "## Product overview",
    ]

    for product in index["products"]:
        lines.append(
            f"- `{product['product']}`: {product['stats']['subcategories']} subcategories, "
            f"{product['stats']['components_kept']} components, "
            f"{product['stats']['components_target_compliant']} target-compliant"
        )

    lines.append("")
    lines.append("## Suggested retrieval strategy for AI")
    lines.append(
        "- Prefer matching by `product + group_name + subcategory_name + component_name`."
    )
    lines.append(
        "- If user asks for auto dark mode/theme switching, filter `target_compliant=true`."
    )
    lines.append("- All exported snippets are Vue + Tailwind 4.2.")
    lines.append("- Use `target_compliant=true` to pick auto-theme capable templates first.")
    lines.append("")
    return "\n".join(lines).rstrip() + "\n"


def crawl_and_export(out_dir: Path, wanted: SnippetFilter) -> dict[str, Any]:
    out_dir.mkdir(parents=True, exist_ok=True)
    templates_root = out_dir / "templates"
    templates_root.mkdir(parents=True, exist_ok=True)

    global_stats = {
        "products": 0,
        "subcategories": 0,
        "components_kept": 0,
        "components_target_compliant": 0,
    }
    product_records: list[dict[str, Any]] = []

    for product_path in PRODUCT_PATHS:
        product_subcats = collect_subcategories_from_product_page(product_path)
        subcategory_records: list[dict[str, Any]] = []
        product_kept = 0
        product_target_compliant = 0

        for sub in product_subcats:
            sub_html = fetch_html(sub["absolute_url"])
            sub_data = extract_data_page_json(sub_html)
            props = sub_data.get("props", {})
            page_tailwind_version = str(props.get("tailwindVersion", ""))
            subcategory = props.get("subcategory", {})
            components = subcategory.get("components", [])

            group_slug = slugify(sub["group_name"])
            sub_slug = slugify(sub["name"])
            sub_out = templates_root / product_path / group_slug / sub_slug
            sub_out.mkdir(parents=True, exist_ok=True)

            kept_components: list[dict[str, Any]] = []
            sub_target_compliant = 0
            for idx, comp in enumerate(components, start=1):
                keep, target_compliant, reason = should_keep_component(
                    comp, page_tailwind_version, wanted
                )
                if not keep:
                    continue

                snippet = comp["snippet"]
                component_slug = slugify(comp.get("name", f"component-{idx}"))
                prefix = f"{idx:02d}__{component_slug}"

                vue_path = sub_out / f"{prefix}.vue"
                preview_path = sub_out / f"{prefix}.preview.html"
                meta_path = sub_out / f"{prefix}.meta.json"

                write_text(vue_path, snippet.get("code", ""))
                write_text(preview_path, snippet.get("preview", ""))

                meta = {
                    "product": product_path,
                    "group_name": sub["group_name"],
                    "subcategory_name": sub["name"],
                    "subcategory_url": sub["absolute_url"],
                    "component_id": comp.get("id"),
                    "component_uuid": comp.get("uuid"),
                    "component_name": comp.get("name"),
                    "downloadable": comp.get("downloadable"),
                    "archived": comp.get("archived"),
                    "snippet": {
                        "framework": snippet.get("name"),
                        "language": snippet.get("language"),
                        "version": snippet.get("version"),
                        "mode": snippet.get("mode"),
                        "supportsDarkMode": snippet.get("supportsDarkMode"),
                    },
                    "target_compliant": target_compliant,
                    "filter_result": reason,
                    "files": {
                        "vue": str(vue_path.relative_to(out_dir)).replace("\\", "/"),
                        "preview": str(preview_path.relative_to(out_dir)).replace("\\", "/"),
                    },
                }
                write_text(meta_path, json.dumps(meta, ensure_ascii=False, indent=2))
                kept_components.append(meta)
                if target_compliant:
                    sub_target_compliant += 1

            sub_record = {
                "group_name": sub["group_name"],
                "subcategory_name": sub["name"],
                "subcategory_url": sub["absolute_url"],
                "subcategory_path": str(sub_out.relative_to(out_dir)).replace("\\", "/"),
                "components_kept": len(kept_components),
                "components_target_compliant": sub_target_compliant,
                "components": kept_components,
            }
            subcategory_records.append(sub_record)
            product_kept += len(kept_components)
            product_target_compliant += sub_target_compliant

        product_records.append(
            {
                "product": product_path,
                "stats": {
                    "subcategories": len(subcategory_records),
                    "components_kept": product_kept,
                    "components_target_compliant": product_target_compliant,
                },
                "subcategories": subcategory_records,
            }
        )

        global_stats["products"] += 1
        global_stats["subcategories"] += len(subcategory_records)
        global_stats["components_kept"] += product_kept
        global_stats["components_target_compliant"] += product_target_compliant

    return {
        "source": f"{BASE_URL}/plus/ui-blocks",
        "filters": {
            "tailwind_version": wanted.tailwind_version,
            "framework": wanted.framework,
            "mode": wanted.mode,
            "require_dark_mode": wanted.require_dark_mode,
            "strict_target_only": wanted.strict_target_only,
            "include_archived": wanted.include_archived,
        },
        "stats": global_stats,
        "products": product_records,
    }


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description=(
            "Download Tailwind Plus UI blocks by original categories "
            "and export AI-friendly index files."
        )
    )
    parser.add_argument(
        "--out-dir",
        default="tailwindui_export",
        help="Output directory (default: tailwindui_export)",
    )
    parser.add_argument(
        "--tailwind-version",
        default="4.2",
        help="Required Tailwind version from page props (default: 4.2)",
    )
    parser.add_argument(
        "--include-archived",
        action="store_true",
        help="Include archived components",
    )
    parser.add_argument(
        "--strict-target-only",
        action="store_true",
        help=(
            "Only keep snippets that match the target mode/dark requirements "
            "(default: keep all Vue+Tailwind templates and mark target_compliant)"
        ),
    )
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    out_dir = Path(args.out_dir).resolve()
    wanted = SnippetFilter(
        tailwind_version=args.tailwind_version,
        strict_target_only=args.strict_target_only,
        include_archived=args.include_archived,
    )

    try:
        index = crawl_and_export(out_dir=out_dir, wanted=wanted)
    except Exception as exc:  # pragma: no cover - CLI top-level
        print(f"[ERROR] {exc}", file=sys.stderr)
        return 1

    write_text(out_dir / "catalog.json", json.dumps(index, ensure_ascii=False, indent=2))
    write_text(out_dir / "DIRECTORY_OVERVIEW.md", build_tree_markdown(index))
    write_text(out_dir / "AI_TEMPLATE_GUIDE.md", build_ai_guide_markdown(index, wanted))
    write_text(
        out_dir / "AI_TEMPLATE_GUIDE.json",
        json.dumps(
            {
                "usage": "Use this as retrieval index for prompt-time template selection.",
                "guide_version": "1.0",
                "index_file": "catalog.json",
                "filters": index["filters"],
                "stats": index["stats"],
            },
            ensure_ascii=False,
            indent=2,
        ),
    )

    print("[OK] Export finished")
    print(f"[OUT] {out_dir}")
    print(
        "[STATS] "
        f"products={index['stats']['products']} "
        f"subcategories={index['stats']['subcategories']} "
        f"components_kept={index['stats']['components_kept']}"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
