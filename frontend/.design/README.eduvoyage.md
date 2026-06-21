# EduVoyage 前端设计素材（离线）

本目录是 **[MRNOBODY-ZST/TailwindCSS-DesignSkill](https://github.com/MRNOBODY-ZST/TailwindCSS-DesignSkill)**
`frontend-tailwind-css/` 的离线快照，作为第四阶段前端开发的区块素材库与检索工具。
设计方法与令牌见 [`../../docs/frontend-design.md`](../../docs/frontend-design.md)。

## 内容
- `SKILL.md` —— 设计技能说明（工作流、适配规则、Tailwind 4 类约定）。
- `assets/tailwindui_template/` —— 657 个 Vue + Tailwind 4.2 区块（marketing / application-ui / ecommerce），
  含 `catalog.json`、`DIRECTORY_OVERVIEW.md`、`AI_TEMPLATE_GUIDE.md`、各区块 `.vue` / `.preview.html` / `.meta.json`。
- `scripts/tailwind_templates.py` —— 离线检索/复制工具。
- `references/template-library.md`、`agents/` —— 参考资料。
- `UPSTREAM-README.md`、`LICENSE` —— 上游说明与 MIT 许可。

## 离线用法（无需联网）
```bash
cd frontend/.design

# 统计
python3 scripts/tailwind_templates.py stats

# 列出产品/子类
python3 scripts/tailwind_templates.py list

# 按业务名词检索（优先 application-ui + 暗色合规）
python3 scripts/tailwind_templates.py search -q dashboard --product application-ui --target-compliant --limit 10
python3 scripts/tailwind_templates.py search -q "table" --product application-ui
python3 scripts/tailwind_templates.py search -q "sidebar navbar modal form stats" --product application-ui

# 查看某区块源码/元数据
python3 scripts/tailwind_templates.py show --component <name>

# 复制区块到目标位置后再适配（删演示数据、绑真实 props/API）
python3 scripts/tailwind_templates.py copy --component <name> --dest ../src/components/<...>
```

## 适配铁律（务必遵守，详见 SKILL.md / frontend-design.md §1）
1. 保留结构与间距节奏，删除演示内容；绑定真实 props/state/route/API。
2. 优先 `target_compliant=true`（auto theme + dark）区块。
3. 图标统一 **Heroicons**，交互原语统一 **Headless UI**（与区块默认一致）。
4. 保留无障碍属性（`aria-*`、`sr-only`、焦点态、对话框语义、键盘可达）。
5. 亮/暗双跑 + 桌面/移动响应式验证后再提交。

> 注：本目录为素材与工具，不参与前端构建产物；第四阶段从这里挑区块到 `frontend/src/`。
