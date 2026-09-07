# Design System Specification: The Organic Editor

## 1. Overview & Creative North Star
**Creative North Star: "The Tactile Sanctuary"**

This design system rejects the clinical, rigid structure of traditional note-taking apps in favor of an editorial, organic experience. We are moving away from "software" and toward "stationery." By leveraging Material 3’s Expressive logic, we prioritize breathing room, oversized typography, and hyper-rounded geometry. The goal is to make the act of writing feel like placing soft paper on a mossy surface. We break the "template" look through intentional white space, bottom-heavy ergonomics, and a complete absence of harsh structural lines.

## 2. Colors & Surface Philosophy
The palette is rooted in a lush, botanical spectrum. It utilizes a "Low-Contrast, High-Depth" approach to reduce eye strain during long writing sessions.

### The "No-Line" Rule
**Strict Mandate:** Designers are prohibited from using 1px solid borders to define sections. Layout boundaries must be achieved through **Tonal Shifting**.
*   **Example:** A note card (`surface-container-lowest`) should sit on a `surface-container-low` background. The change in hex value is the border.

### Surface Hierarchy & Nesting
Treat the UI as physical layers of thick, recycled cardstock.
*   **Base Layer:** `surface` (#e9ffed) — The primary canvas.
*   **Secondary Content:** `surface-container` (#d0f7dc) — Used for grouping related notes.
*   **Highest Priority/Interactive:** `surface-container-highest` (#bbeecc) — For active states or emphasized cards.

### The "Glass & Gradient" Rule
To elevate the "Expressive" nature of the system, floating elements (like the Bottom Bar or FAB) must utilize **Glassmorphism**:
*   **Token:** `surface-container-low` at 80% opacity.
*   **Effect:** `backdrop-filter: blur(24px)`. This prevents the UI from feeling "clipped" and allows the organic greens to bleed through the navigation.

### Signature Textures
Use a subtle linear gradient for primary actions:
*   **CTA Gradient:** `primary` (#456748) to `primary_dim` (#395b3d) at a 135° angle. This adds "soul" and weight to the pastel palette.

---

3. Typography
The typography scale utilizes **Inter** to maintain a clean, modernist edge against the soft, organic shapes of the UI.

*   **Display (LG/MD):** Used exclusively for empty states or "New Note" headers. It should feel unapologetically large, creating an editorial, magazine-style entry point.
*   **Headline (SM/MD):** The workhorse for note titles. High contrast against body text is essential.
*   **Body (LG):** Optimized for the Markdown editor. We use `body-lg` (1rem) as the default to ensure the "Sanctuary" feel—never cramming text.
*   **Label (SM):** Used for metadata (tags, character counts).

**Hierarchy Principle:** Use `on_surface_variant` (#3a684e) for secondary metadata to create a natural "receding" effect, keeping the focus on the primary `on_surface` text.

---

## 4. Elevation & Depth
In this system, elevation is a product of light and color, not shadows.

*   **The Layering Principle:** Depth is achieved by "stacking." Place a `surface-container-lowest` (#ffffff) card on a `surface-container-low` (#dafce3) background. This creates a "soft lift."
*   **Ambient Shadows:** If a floating element requires a shadow (e.g., a Modal), use a **Botanical Shadow**:
    *   `box-shadow: 0px 12px 32px rgba(7, 58, 35, 0.08);` (A dark green tint rather than grey).
*   **The "Ghost Border" Fallback:** If a border is required for accessibility, use `outline-variant` (#8cbd9d) at **15% opacity**. Never 100%.

---

## 5. Components

### Buttons
*   **Primary:** Extra rounded (`rounded-full`). Uses the Signature Gradient. No shadow.
*   **Secondary:** `primary-container` (#c6ecc5) with `on-primary-container` text.
*   **Tertiary:** No background. Text-only with `primary` color, used for low-emphasis actions like "Cancel."

### The "Floating Dock" (Bottom Navigation)
*   **Style:** A pill-shaped container (`rounded-xl`) floating 16px from the bottom.
*   **Visuals:** Glassmorphic `surface-container-low` with a 24px blur.
*   **Active State:** Use a `secondary-container` (#ffdbc8) pill behind the active icon.

### Input Fields & Markdown Editor
*   **The Canvas:** The editor should have no visible input box. It is a borderless "infinite" sheet of `surface`.
*   **Selection:** Text selection should use `tertiary-container` (#ffa271) at 40% opacity for a warm, highlighter effect.

### Cards & Lists
*   **Rule:** **No Divider Lines.** 
*   **Separation:** Use `16px` or `24px` of vertical space. For list-heavy views, alternate the background of every other item between `surface` and `surface-container-low`.
*   **Corners:** All cards must use `rounded-lg` (2rem) to maintain the Expressive identity.

---

## 6. Do's and Don'ts

### Do
*   **Do** use asymmetrical margins (e.g., more padding at the top of a note than the sides) to create an editorial feel.
*   **Do** use the `secondary` (#80553b) and `tertiary` (#994817) tones for "warm" interactions like tagging or highlighting favorites.
*   **Do** prioritize the bottom of the screen for all interactive elements to favor one-handed mobile use.

### Don't
*   **Don't** use pure black (#000000) for text. Use `on_surface` (#073a23) to keep the contrast soft and natural.
*   **Don't** use standard Material 2 shadows. They feel "dirty" against the pastel green palette.
*   **Don't** use "Default" 4px or 8px corners. If it's not `1rem` or higher, it doesn't belong in this system.