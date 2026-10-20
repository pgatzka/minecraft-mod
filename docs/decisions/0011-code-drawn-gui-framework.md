# 0011. GUIs drawn in code with a small framework, no texture assets

- Status: Accepted. Exception: vanilla's built-in empty armor slot icons are used ([0031](0031-vanilla-armor-slot-icons.md)).
- Date: 2026-10-20
- Decided by: user (no assets; build a framework), Claude (design)

## Context
The first Town Hall screen used vanilla's `demo_background.png`. The user asked: "make the gui not from assets, instead
make some custom menu framework so no assets are required".

## Decision
`client/ui/` holds a small framework where everything is drawn with filled rectangles and text:
- `UiTheme`: all colors, in vanilla's gray style.
- `UiDraw`: raised boxes and panels.
- `UiButton` and `UiSelectionList`: a button and a scrollable, selectable list.
- `UiLabel`: wrapping text that can change and **never draws more than its reserved lines** (cut-off text ends in "...").
- `UiColumn`: stacks labels, text fields (vanilla `EditBox`, which draws without textures in 1.20.1), buttons, button
  rows, lists and the error line.
- `UiScreen`: the base screen. It draws the panel and title, keeps text fields blinking, sends keys to a focused text
  field so E doesn't close the screen, and has a shared error line (`showError`).

## Why
The user's request. A column layout means screens don't need hand-placed coordinates. Label clipping was added after
the Save Blueprint screen hid a wrapping "Saves as ..." line behind its button.

## Consequences
New screens should use `UiScreen` and `UiColumn`. Text that changes needs its number of lines reserved up front
(`label(supplier, color, lines)`, `errorLabel(lines)`), since the layout doesn't adjust itself.
