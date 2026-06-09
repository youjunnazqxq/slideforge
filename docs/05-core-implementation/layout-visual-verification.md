# Deck Layout Visual Verification

## Scope

Target page:

```text
/app/deck-outline
```

Layout expectation:

- Left column: deck prompt and workflow actions.
- Center workspace: outline, research pack, sticky notes, generated slide previews.
- Right column: collapsible Agent panel with status, timeline, export preview, and prompt trace.
- Mobile/narrow viewport: columns collapse into a single readable column.

## Verified

- `npm.cmd run build` passes.
- Vue type-check passes through the build script.
- The known remaining build warnings are from Rolldown handling `@vueuse/core` pure annotations and large chunk size warnings.

## Browser Screenshot Attempt

Attempted to run Vite preview and capture desktop/mobile screenshots with Microsoft Edge headless:

```text
node node_modules/vite/bin/vite.js preview --host 127.0.0.1 --port 4174
msedge.exe --headless --screenshot=deck-desktop.png http://127.0.0.1:4174/auth.html
msedge.exe --headless --screenshot=deck-mobile.png http://127.0.0.1:4174/auth.html
```

Result:

- Vite preview starts successfully in the foreground.
- The Codex in-app browser tool was not available in this session.
- Local Microsoft Edge headless returned exit code 0 but did not write screenshot files, even for a minimal `data:text/html` page.

Because no screenshot artifact was produced, visual verification remains open.

## Next Verification Path

Use either of these paths when a browser automation tool is available:

1. Open `http://127.0.0.1:4174/app/deck-outline`.
2. Seed local storage before navigation:

```js
localStorage.setItem('slideforge:user', JSON.stringify({ username: 'Visual QA', token: 'visual-token' }))
```

3. Capture desktop viewport around `1440x1100`.
4. Capture mobile viewport around `390x1100`.
5. Confirm:
   - Desktop shows three columns.
   - Right Agent panel collapse button works.
   - Agent timeline and export preview fit inside the right panel.
   - Mobile layout stacks into one column without text overlap.
