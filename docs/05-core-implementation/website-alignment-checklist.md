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
- SVG validation checks root closure, 1280x720 canvas, external resources, inline styles, filters, data URLs, element bounds, text density, and Bento grid density.

## Remaining Gaps

- Add an in-app step timeline for the server-side agent flow instead of only showing the current step label.
- Add per-step retry controls for research, outline, page plans, visual specs, and SVG separately.
- Add deck-level export preview that shows the final slide order before PPTX download.
- Add API documentation for the agent flow endpoint and expected BYOK AI-provider setup.
- Add backend tests around `runAgentFlow` orchestration with mocked AI services.
- Add visual regression or browser verification for the deck three-column layout.
