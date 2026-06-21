# Template Library Reference

Use this file when choosing which bundled Tailwind UI block to start from. For exact paths and all variants, use `scripts/tailwind_templates.py` or inspect `assets/tailwindui_template/DIRECTORY_OVERVIEW.md`.

## Library Profile

- Source archive: `assets/tailwindui_template`
- Framework of snippets: Vue
- Tailwind version: 4.2
- Total products: 3
- Total subcategories: 93
- Total components: 657
- Target-compliant components: 543

Target-compliant means the metadata reports `mode=system` and `supportsDarkMode=true`.

## Product Map

| Product | Use for | Coverage | Target-compliant |
| --- | --- | --- | --- |
| `marketing` | Landing pages, brand/product pages, campaigns, content pages | 23 subcategories, 179 components | 179 |
| `application-ui` | Dashboards, SaaS tools, internal apps, data and form workflows | 49 subcategories, 364 components | 364 |
| `ecommerce` | Product, cart, checkout, reviews, order history | 21 subcategories, 114 components | 0 |

## Common Intent Mapping

| User intent | Product | Useful subcategories |
| --- | --- | --- |
| Complete landing page | `marketing` | Page Examples/Landing Pages, Hero Sections, Feature Sections, CTA Sections, Testimonials, FAQs, Footers |
| Pricing page | `marketing` | Pricing Sections, Page Examples/Pricing Pages, FAQs, CTA Sections |
| About or company page | `marketing` | About Pages, Team Sections, Stats, Logo Clouds, Content Sections |
| Dashboard shell | `application-ui` | Application Shells/Stacked Layouts, Sidebar Layouts, Multi-Column Layouts |
| Admin table or list | `application-ui` | Lists/Tables, Stacked Lists, Grid Lists, Feeds |
| Settings or account page | `application-ui` | Page Examples/Settings Screens, Forms/Form Layouts, Section Headings, Action Panels |
| Data overview | `application-ui` | Data Display/Stats, Description Lists, Calendars, Page Headings |
| Form controls | `application-ui` | Input Groups, Select Menus, Radio Groups, Checkboxes, Toggles, Comboboxes, Textareas |
| Modal or side panel | `application-ui` | Overlays/Modal Dialogs, Drawers, Notifications |
| Navigation | `application-ui` or `marketing` | Navbars, Tabs, Vertical Navigation, Breadcrumbs, Headers, Flyout Menus |
| Product listing | `ecommerce` | Product Lists, Category Previews, Category Filters |
| Product details | `ecommerce` | Product Overviews, Product Features, Product Pages, Reviews |
| Checkout flow | `ecommerce` | Shopping Carts, Checkout Forms, Checkout Pages, Order Summaries |

## Category Inventory

Marketing includes:

- Page Sections: Hero Sections, Feature Sections, CTA Sections, Bento Grids, Pricing Sections, Header Sections, Newsletter Sections, Stats, Testimonials, Blog Sections, Contact Sections, Team Sections, Content Sections, Logo Clouds, FAQs, Footers.
- Elements: Headers, Flyout Menus, Banners.
- Feedback: 404 Pages.
- Page Examples: Landing Pages, Pricing Pages, About Pages.

Application UI includes:

- Application Shells: Stacked Layouts, Sidebar Layouts, Multi-Column Layouts.
- Headings: Page Headings, Card Headings, Section Headings.
- Data Display: Description Lists, Stats, Calendars.
- Lists: Stacked Lists, Tables, Grid Lists, Feeds.
- Forms: Form Layouts, Input Groups, Select Menus, Sign-in and Registration, Textareas, Radio Groups, Checkboxes, Toggles, Action Panels, Comboboxes.
- Feedback: Alerts, Empty States.
- Navigation: Navbars, Pagination, Tabs, Vertical Navigation, Sidebar Navigation, Breadcrumbs, Progress Bars, Command Palettes.
- Overlays: Modal Dialogs, Drawers, Notifications.
- Elements: Avatars, Badges, Dropdowns, Buttons, Button Groups.
- Layout: Containers, Cards, List containers, Media Objects, Dividers.
- Page Examples: Home Screens, Detail Screens, Settings Screens.

Ecommerce includes:

- Components: Product Overviews, Product Lists, Category Previews, Shopping Carts, Category Filters, Product Quickviews, Product Features, Store Navigation, Promo Sections, Checkout Forms, Reviews, Order Summaries, Order History, Incentives.
- Page Examples: Storefront Pages, Product Pages, Category Pages, Shopping Cart Pages, Checkout Pages, Order Detail Pages, Order History Pages.

## Notes

- Snippets can include placeholder images under `/plus-assets/...`; replace these with app assets or generated images.
- Snippets may use Headless UI and Heroicons imports. Convert to the project's UI primitives when appropriate.
- Ecommerce templates are useful structurally, but add or verify dark-mode classes when the project requires them.
