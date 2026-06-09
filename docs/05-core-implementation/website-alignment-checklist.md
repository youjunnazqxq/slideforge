# Website Alignment Checklist

This checklist tracks SlideForge against the referenced PPT Agent workflow:

`需求调研 -> 资料检索 -> 结构大纲 -> 便利贴排布 -> 逐页策划稿 -> Bento Grid 视觉设计 -> SVG/PPT 输出`

## Implemented

- Consultant-style clarification before deck outline.
- Model-only and search-assisted research pack flow.
- Deck outline skeleton with cover, agenda, section, content, and summary page types.
- Sticky-note page planning, reordering, editing, adding, and deleting.
- Batch one-page draft creation from deck sticky notes.
- Batch page-plan generation for every sticky note.
- Batch Bento visual-spec generation for every page plan.
- Batch SVG generation, validation, preview, failed-slide retry, and PPTX export gating.
- Prompt trace list with expandable prompt and output previews.
- Server-side `POST /api/decks/{deckId}/agent-flow` orchestration endpoint.
- Deck workspace uses a three-column shell with a collapsible right Agent panel.
- Agent Flow timeline in the right panel.
- Per-step rerun controls for consult, research, outline, page plans, Bento specs, and SVG generation.
- Deck-level export preview showing final slide order and per-slide export readiness.
- Developer-facing Deck Agent API guide with BYOK setup, flow orchestration, rerun endpoints, export preconditions, and error notes.
- Backend integration test for `runAgentFlow` orchestration with mocked AI and one-page generation services.
- SVG validation checks root closure, 1280x720 canvas, external resources, inline styles, filters, data URLs, element bounds, text density, and Bento grid density.
- Backend Bento visualSpec quality gate enforces 1280x720 canvas, 3-6 cards, primary-card presence, minimum card sizes, and at least 20px spacing; invalid layouts fall back to the default Bento pattern.
- VisualSpec now carries a `layoutPattern` selected from hero-left, hero-top, mosaic, and split-hero, and the editor exposes the selected Bento pattern.
- PagePlan now carries editable `speakerIntent` so each page keeps a consultant-style speaking purpose before visual design.
- PagePlan content blocks now carry editable `sourceIds` so research evidence bindings survive through planning and later visual generation.
- One-page planning now shows research evidence cards and an evidence coverage panel, making unlinked page-plan blocks and cited sources visible before visual design.
- One-page Prompt Trace entries now expand inline to show prompt and output previews, matching the traceability already available in the deck agent panel.
- One-page Bento visual design now includes switchable layout presets for hero-left, hero-top, mosaic, and split-hero, with card coordinates regenerated from the current content blocks.

## Remaining Gaps

- Complete browser visual verification for the deck three-column layout. See `layout-visual-verification.md` for the attempted Edge headless path and current environment blocker.
