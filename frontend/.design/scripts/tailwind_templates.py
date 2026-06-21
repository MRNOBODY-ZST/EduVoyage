#!/usr/bin/env python3
"""Search and copy bundled Tailwind UI templates."""

from __future__ import annotations

import argparse
import json
import shutil
import sys
from pathlib import Path
from typing import Any, Iterable


SKILL_ROOT = Path(__file__).resolve().parents[1]
DEFAULT_LIBRARY_ROOT = SKILL_ROOT / "assets" / "tailwindui_template"


def load_catalog(library_root: Path) -> dict[str, Any]:
    catalog_path = library_root / "catalog.json"
    if not catalog_path.exists():
        raise FileNotFoundError(f"Cannot find catalog: {catalog_path}")
    return json.loads(catalog_path.read_text(encoding="utf-8"))


def iter_components(catalog: dict[str, Any]) -> Iterable[dict[str, Any]]:
    for product in catalog.get("products", []):
        product_name = product.get("product", "")
        for subcategory in product.get("subcategories", []):
            for component in subcategory.get("components", []):
                record = dict(component)
                record["product"] = component.get("product") or product_name
                record["group_name"] = component.get("group_name") or subcategory.get("group_name", "")
                record["subcategory_name"] = component.get("subcategory_name") or subcategory.get("subcategory_name", "")
                record["subcategory_path"] = subcategory.get("subcategory_path", "")
                record["components_kept"] = subcategory.get("components_kept", 0)
                yield record


def text(value: Any) -> str:
    return str(value or "").lower()


def haystack(record: dict[str, Any]) -> str:
    files = record.get("files", {})
    parts = [
        record.get("product", ""),
        record.get("group_name", ""),
        record.get("subcategory_name", ""),
        record.get("component_name", ""),
        files.get("vue", ""),
        files.get("preview", ""),
    ]
    return " ".join(text(part) for part in parts)


def matches_filter(value: str | None, candidate: Any) -> bool:
    if not value:
        return True
    return text(value) in text(candidate)


def filter_records(records: Iterable[dict[str, Any]], args: argparse.Namespace) -> list[dict[str, Any]]:
    out: list[dict[str, Any]] = []
    for record in records:
        if args.product and text(args.product) != text(record.get("product")):
            continue
        if not matches_filter(args.group, record.get("group_name")):
            continue
        if not matches_filter(args.subcategory, record.get("subcategory_name")):
            continue
        if not matches_filter(args.component, record.get("component_name")):
            continue
        if args.target_compliant and not record.get("target_compliant"):
            continue
        out.append(record)
    return out


def score_record(record: dict[str, Any], query: str | None) -> int:
    if not query:
        return 1
    terms = [term for term in text(query).replace("/", " ").replace("-", " ").split() if term]
    body = haystack(record).replace("/", " ").replace("-", " ")
    score = 0
    for term in terms:
        if term not in body:
            return 0
        score += body.count(term)
        if term in text(record.get("component_name")):
            score += 6
        if term in text(record.get("subcategory_name")):
            score += 4
        if term in text(record.get("group_name")):
            score += 2
    if record.get("target_compliant"):
        score += 1
    return score


def component_paths(library_root: Path, record: dict[str, Any]) -> dict[str, Path]:
    files = record.get("files", {})
    vue = library_root / files.get("vue", "")
    preview = library_root / files.get("preview", "")
    meta = vue.with_suffix(".meta.json")
    return {"vue": vue, "preview": preview, "meta": meta}


def display_record(record: dict[str, Any], index: int | None = None, score: int | None = None) -> str:
    files = record.get("files", {})
    prefix = f"{index}. " if index is not None else ""
    target = "yes" if record.get("target_compliant") else "no"
    score_part = f" score={score}" if score is not None else ""
    return (
        f"{prefix}[{record.get('product')}] {record.get('group_name')} / "
        f"{record.get('subcategory_name')} - {record.get('component_name')} "
        f"target={target}{score_part}\n"
        f"   {files.get('vue')}"
    )


def relative_to_library(library_root: Path, raw_path: str) -> str:
    raw = Path(raw_path)
    candidates = []
    if raw.is_absolute():
        candidates.append(raw)
    else:
        candidates.append(library_root / raw)
        candidates.append(SKILL_ROOT / raw)

    for candidate in candidates:
        try:
            resolved = candidate.resolve()
        except OSError:
            continue
        if resolved.exists():
            try:
                return resolved.relative_to(library_root.resolve()).as_posix()
            except ValueError:
                pass

    normalized = raw_path.replace("\\", "/")
    marker = "tailwindui_template/"
    if marker in normalized:
        return normalized.split(marker, 1)[1]
    return normalized.removeprefix("./")


def path_to_vue_rel(rel_path: str) -> str:
    if rel_path.endswith(".preview.html"):
        return rel_path.removesuffix(".preview.html") + ".vue"
    if rel_path.endswith(".meta.json"):
        return rel_path.removesuffix(".meta.json") + ".vue"
    return rel_path


def find_record_by_path(catalog: dict[str, Any], library_root: Path, raw_path: str) -> dict[str, Any]:
    rel_path = path_to_vue_rel(relative_to_library(library_root, raw_path))
    for record in iter_components(catalog):
        files = record.get("files", {})
        if files.get("vue") == rel_path:
            return record
    raise ValueError(f"No component found for path: {raw_path}")


def command_stats(catalog: dict[str, Any]) -> int:
    stats = catalog.get("stats", {})
    print(f"products: {stats.get('products', 0)}")
    print(f"subcategories: {stats.get('subcategories', 0)}")
    print(f"components: {stats.get('components_kept', 0)}")
    print(f"target_compliant: {stats.get('components_target_compliant', 0)}")
    for product in catalog.get("products", []):
        pstats = product.get("stats", {})
        print(
            f"- {product.get('product')}: "
            f"{pstats.get('subcategories', 0)} subcategories, "
            f"{pstats.get('components_kept', 0)} components, "
            f"{pstats.get('components_target_compliant', 0)} target-compliant"
        )
    return 0


def command_list(catalog: dict[str, Any], args: argparse.Namespace) -> int:
    for product in catalog.get("products", []):
        if args.product and text(args.product) != text(product.get("product")):
            continue
        print(f"[{product.get('product')}]")
        for subcategory in product.get("subcategories", []):
            if not matches_filter(args.group, subcategory.get("group_name")):
                continue
            if not matches_filter(args.subcategory, subcategory.get("subcategory_name")):
                continue
            target = subcategory.get("components_target_compliant", 0)
            kept = subcategory.get("components_kept", 0)
            print(
                f"- {subcategory.get('group_name')} / {subcategory.get('subcategory_name')} "
                f"({kept} components, {target} target-compliant)"
            )
    return 0


def command_search(catalog: dict[str, Any], args: argparse.Namespace) -> int:
    records = filter_records(iter_components(catalog), args)
    scored = [(score_record(record, args.query), record) for record in records]
    scored = [(score, record) for score, record in scored if score > 0]
    scored.sort(key=lambda item: (item[0], bool(item[1].get("target_compliant"))), reverse=True)
    limited = scored[: args.limit]

    if args.json:
        payload = []
        for score, record in limited:
            item = {
                "score": score,
                "product": record.get("product"),
                "group_name": record.get("group_name"),
                "subcategory_name": record.get("subcategory_name"),
                "component_name": record.get("component_name"),
                "target_compliant": record.get("target_compliant"),
                "files": record.get("files", {}),
            }
            payload.append(item)
        print(json.dumps(payload, indent=2, ensure_ascii=False))
        return 0

    if not limited:
        print("No matching templates found.")
        return 1

    for index, (score, record) in enumerate(limited, start=1):
        print(display_record(record, index=index, score=score))
    return 0


def command_show(catalog: dict[str, Any], library_root: Path, args: argparse.Namespace) -> int:
    record = find_record_by_path(catalog, library_root, args.path)
    paths = component_paths(library_root, record)
    if args.code:
        print(paths["vue"].read_text(encoding="utf-8"))
        return 0

    print(display_record(record))
    print(f"component_id: {record.get('component_id')}")
    print(f"source_url: {record.get('subcategory_url')}")
    print(f"vue: {paths['vue']}")
    print(f"preview: {paths['preview']}")
    print(f"meta: {paths['meta']}")
    snippet = record.get("snippet", {})
    print(
        "snippet: "
        f"framework={snippet.get('framework')} "
        f"version={snippet.get('version')} "
        f"mode={snippet.get('mode')} "
        f"supportsDarkMode={snippet.get('supportsDarkMode')}"
    )
    return 0


def copy_file(src: Path, dst: Path) -> None:
    if not src.exists():
        return
    dst.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(src, dst)


def command_copy(catalog: dict[str, Any], library_root: Path, args: argparse.Namespace) -> int:
    record = find_record_by_path(catalog, library_root, args.path)
    paths = component_paths(library_root, record)
    out_dir = Path(args.out).resolve()

    name = args.name or paths["vue"].name
    if Path(name).suffix != ".vue":
        name = f"{name}.vue"

    target_vue = out_dir / name
    stem = target_vue.stem
    copied = []

    copy_file(paths["vue"], target_vue)
    copied.append(target_vue)

    if not args.source_only:
        target_preview = out_dir / f"{stem}.preview.html"
        target_meta = out_dir / f"{stem}.meta.json"
        copy_file(paths["preview"], target_preview)
        copy_file(paths["meta"], target_meta)
        if target_preview.exists():
            copied.append(target_preview)
        if target_meta.exists():
            copied.append(target_meta)

    for path in copied:
        print(f"copied: {path}")
    return 0


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description="Search bundled Tailwind UI templates.")
    parser.add_argument(
        "--library-root",
        default=str(DEFAULT_LIBRARY_ROOT),
        help="Path to tailwindui_template asset directory.",
    )

    subparsers = parser.add_subparsers(dest="command", required=True)

    subparsers.add_parser("stats", help="Show library statistics.")

    list_parser = subparsers.add_parser("list", help="List products and subcategories.")
    list_parser.add_argument("--product", choices=["marketing", "application-ui", "ecommerce"])
    list_parser.add_argument("--group")
    list_parser.add_argument("--subcategory")

    search_parser = subparsers.add_parser("search", help="Search components.")
    search_parser.add_argument("--query", "-q", default="")
    search_parser.add_argument("--product", choices=["marketing", "application-ui", "ecommerce"])
    search_parser.add_argument("--group")
    search_parser.add_argument("--subcategory")
    search_parser.add_argument("--component")
    search_parser.add_argument("--target-compliant", action="store_true")
    search_parser.add_argument("--limit", type=int, default=12)
    search_parser.add_argument("--json", action="store_true")

    show_parser = subparsers.add_parser("show", help="Show component metadata or source.")
    show_parser.add_argument("--path", required=True, help="Component .vue, .preview.html, or .meta.json path.")
    show_parser.add_argument("--code", action="store_true", help="Print the .vue source.")

    copy_parser = subparsers.add_parser("copy", help="Copy a component into a target project.")
    copy_parser.add_argument("--path", required=True, help="Component .vue, .preview.html, or .meta.json path.")
    copy_parser.add_argument("--out", required=True, help="Output directory.")
    copy_parser.add_argument("--name", help="Output Vue filename.")
    copy_parser.add_argument("--source-only", action="store_true", help="Only copy the .vue file.")

    return parser


def main(argv: list[str] | None = None) -> int:
    parser = build_parser()
    args = parser.parse_args(argv)
    library_root = Path(args.library_root).resolve()

    try:
        catalog = load_catalog(library_root)
        if args.command == "stats":
            return command_stats(catalog)
        if args.command == "list":
            return command_list(catalog, args)
        if args.command == "search":
            return command_search(catalog, args)
        if args.command == "show":
            return command_show(catalog, library_root, args)
        if args.command == "copy":
            return command_copy(catalog, library_root, args)
    except Exception as exc:
        print(f"error: {exc}", file=sys.stderr)
        return 1

    parser.print_help()
    return 1


if __name__ == "__main__":
    raise SystemExit(main())
