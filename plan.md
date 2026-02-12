# Plan: CodexMonitor-style flow parity (Android / single app-server over WS)

## Status (already implemented)

- WS inbound stream parsing now classifies messages into:
  - **Response** (`id` + `result|error`, no `method`)
  - **ServerRequest** (`id` + `method`, no `result|error`)
  - **Notification** (`method`, no `id`)
- WS event emission is non-blocking (buffered + drop-oldest) and client request IDs start high to reduce full-duplex ID collision risk.
- App keeps WS connected in background (no auto-close on background), and also runs a foreground keep-alive service.
- Threads are grouped **directory-wise** (by `cwd`) in the drawer.
- Directory groups in the drawer are **collapsible** and include **search**.
- New sessions now prompt for `cwd`, and `thread/start` includes `cwd` (+ `source=appServer`).
- Minimal `turnId -> threadId` routing map added for turn-only events (prevents mis-applying events like `turn/plan/updated`).
- Threads DB uses a composite key (`connectionId`, `id`) so multiple connections can’t overwrite each other.
- Global event router (`CodexEventRouter`) runs for the active connection and keeps history accurate by refreshing threads on key events.
- Router now applies streaming notifications (turns/items/deltas/plan updates) into Room with debounced persistence.
- Server-initiated requests are surfaced in UI and responded to:
  - approvals (`*requestApproval*`): returns `{ decision: "accept" | "decline" }`
  - user input (`item/tool/requestUserInput`): returns `{ answers: { [questionId]: { answers: string[] } } }`
- Session UI reads the selected thread from Room (no “active thread only” WS parsing in the ViewModel).
- Android 13+: `POST_NOTIFICATIONS` is requested so the foreground keep-alive notification can show.
- “Codex is connected” foreground service notification is tappable and opens the app (most recent / latest thread).
- `turn/completed` triggers an Android notification (only when backgrounded) and deep-links into the relevant session.
- Session top bar title shows workspace folder name (last segment of `cwd`, handles `/` + `\\`).
- Session title includes context-left percent in the centered title (`Workspace · 78%`).
- Drawer shows a running indicator next to threads with an in-progress turn.
- Markdown rendering uses `compose-richtext` and agent output renders full-width.

## Next steps (implementation plan)

### UX parity + polish (CodexMonitor-inspired)

- **Needs attention inbox**
  - Replace/augment approval + user-input dialogs with a global “Needs attention” queue.
  - Routing ambiguous requests should land here (never dropped).
  - Thread-scoped requests should deep-link into the thread.

### Capability controls (CodexMonitor-inspired)

- **Skills list**
  - Add `skills/list` support and a UI to browse skills.

- **Model selector**
  - Add `model/list` and a per-session “active model” selector.

- **Reasoning effort selector**
  - Add UI control for effort (and send it on `turn/start`).

### Reliability / debuggability

- **Router debug mode**
  - Add a toggle to show router routing decisions (threadId/turnId mapping) and last WS activity.
  - Useful for diagnosing shared-server routing issues.

- **Code cleanup**
  - Remove unused Room connection entities (`ConnectionDao`/`ConnectionEntity`) or migrate connections fully to Room.
