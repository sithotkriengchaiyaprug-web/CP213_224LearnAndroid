# Product Requirements Document (PRD)

## Zero-Touch Daily Budgeting App

**Version:** 1.0  
**Status:** Draft  
**Owner:** Product / Engineering  
**Last Updated:** 2026-04-23

## 1) Executive Summary

Zero-Touch Daily Budgeting App is a local-first budgeting app designed to remove the biggest reason people stop using budget apps: manual data entry. The app automatically detects spending from bank notifications, extracts receipt data from OCR, and updates the user’s daily budget in real time. The primary experience is delivered through a home screen widget so users can glance at their remaining budget without opening the app.

The product focuses on “no-friction expense tracking” for students and working adults who already use mobile banking and do not want to enter transactions manually.

## 2) Problem Statement

Most budgeting apps fail because they require users to:

- open the app regularly,
- enter each expense manually,
- categorize transactions themselves,
- and maintain the habit over time.

This creates friction and drop-off. Users want to know how much they have left today, but they do not want extra steps. We need a budgeting experience that fits into existing behavior instead of creating new behavior.

## 3) Product Vision

Make budget tracking invisible, automatic, and useful at a glance.

The app should:

- detect expenses with minimal or no user action,
- update the budget immediately,
- surface the most important number on a widget,
- and keep user data on-device by default.

## 4) Goals

### Primary Goals

- Reduce friction so users do not need to open the app for normal expense tracking.
- Track daily spending in near real time.
- Make the home screen widget the main user interface.
- Keep all sensitive data local on the device.

### Success Goals

- Users can understand their remaining daily budget in under 3 seconds.
- Users can record an expense in under 10 seconds when using OCR/manual fallback.
- Most spend tracking happens automatically through notification parsing.
- Users continue using the app because it feels effortless.

## 5) Non-Goals

This version does not aim to:

- connect directly to bank APIs,
- perform cloud sync across devices,
- support shared family budgets,
- replace accounting software,
- or provide tax/legal financial reporting.

## 6) Target Users

### Primary Persona: Student

- Uses mobile banking and e-wallets.
- Spends on food, transport, and daily needs.
- Does not want to log every purchase manually.
- Wants a simple “money left today” view.

### Primary Persona: Working Adult

- Receives salary and spends regularly throughout the day.
- Wants awareness of daily spending without doing bookkeeping.
- Prefers automation and minimal UI.

### Secondary Persona: Budget-Conscious User

- Uses a fixed daily allowance.
- Wants quick insights into overspending.
- Prefers local-only storage for privacy.

## 7) Product Principles

- **Zero-friction first:** The app should work without users thinking about it.
- **Widget-first:** The widget is the default surface, not an optional extra.
- **Local by default:** Sensitive data stays on-device.
- **Event-driven:** Update only when an event happens; avoid polling.
- **Graceful fallback:** If automation fails, the app should still allow fast manual capture.

## 8) Core Use Cases

### UC1: Auto-detect a bank transaction

User receives a bank notification. The app parses the amount and merchant name, saves the transaction, updates the budget, and refreshes the widget.

### UC2: Scan a receipt / slip

User takes a photo of a receipt or payment slip. Gemini extracts amount and merchant data, the app validates the result, and saves it locally.

### UC3: Set a daily budget

User defines a daily spending limit. The app calculates remaining budget and rolls unused budget to the next day if enabled.

### UC4: Check remaining budget from widget

User glances at the widget and instantly sees today’s remaining amount, spending so far, and quick actions.

## 9) MVP Scope

### In Scope

- Notification-based transaction tracking.
- OCR receipt scanning with Gemini for extraction.
- Local Room database.
- Daily budget setup.
- Remaining budget calculation.
- Widget showing today’s summary.
- Manual add transaction fallback.
- Midnight reset using WorkManager.

### Out of Scope for MVP

- Bank API integrations.
- Cross-device sync.
- Cloud backup.
- Smart category prediction.
- Weekly analytics dashboards.
- Duplicate detection beyond basic heuristics.

## 10) Functional Requirements

### 10.1 Auto Transaction Tracking

**Description:** The app listens to approved notifications and extracts transaction details.

**Requirements:**

- Detect notifications from selected finance apps or banks.
- Parse transaction amount from the notification body or title.
- Extract merchant/brand when available.
- Identify transaction direction: expense vs income when possible.
- Save the transaction locally.
- Update the widget immediately after save.
- Log parsing confidence for later debugging.

**Fallback Behavior:**

- If parsing fails, store the raw notification text for manual review.
- If merchant name is unavailable, mark it as “Unknown”.

### 10.2 OCR Receipt Scanner

**Description:** The app uses camera capture and Gemini extraction for receipt/slip parsing.

**Requirements:**

- Allow the user to capture a receipt or bank slip image.
- Send the image to Gemini only when the user explicitly triggers OCR.
- Ask Gemini to return structured JSON.
- Extract:
  - amount,
  - merchant/brand,
  - date if available,
  - optional category.
- Validate JSON output before saving.
- Provide fallback parsing if Gemini output is incomplete or malformed.

**Fallback Behavior:**

- Use regex or heuristic parsing on extracted text.
- Let the user edit the result before saving.

### 10.3 Daily Budget System

**Description:** The app tracks a daily limit and calculates remaining balance.

**Requirements:**

- User can set a daily spending budget.
- App calculates “spent today”.
- App calculates “remaining today”.
- App supports rollover of unused budget to the next day, if enabled.
- App resets daily totals at midnight local time.

**Rules:**

- Budget can be edited anytime.
- Negative remaining balance should be shown clearly as overspent.
- Daily totals should be based on the device timezone.

### 10.4 Home Widget

**Description:** The widget is the main product surface.

**Requirements:**

- Show remaining budget today.
- Show total spent today.
- Show budget limit.
- Provide quick actions:
  - Scan,
  - Add manual.
- Support a compact and a medium widget layout.
- Update after every save and on daily reset.

### 10.5 Manual Expense Entry

**Description:** Users can manually add a transaction when auto-detection fails.

**Requirements:**

- Amount entry.
- Merchant/brand field.
- Optional note.
- Optional category.
- Transaction type: expense or income.

### 10.6 Transaction List

**Description:** Users can review recent captured items.

**Requirements:**

- Show timestamp, amount, merchant, source, and confidence.
- Allow edit and delete.
- Filter by day.
- Distinguish auto-captured vs manually entered transactions.

## 11) UX / UI Requirements

### Home Screen / Widget

- Primary number: remaining today.
- Secondary stats: spent today, budget limit.
- Clear color treatment for safe vs overspent state.
- One-tap access to scan and manual add.

### App Shell

- Minimal navigation.
- Default landing page should mirror the widget summary.
- Settings should be easy to find but not intrusive.

### Editing Flow

- Parsed data should be reviewable before save.
- Errors should be explainable in plain language.
- User should never feel “stuck” after a failed parse.

## 12) User Journeys

### Journey A: Notification Auto-Tracking

1. User receives a banking notification.
2. Notification listener detects supported content.
3. Parser extracts amount and merchant.
4. Transaction is saved to Room.
5. Budget total recalculates.
6. Widget refreshes.

### Journey B: OCR Scan

1. User taps Scan from widget.
2. Camera opens.
3. User captures receipt/slip.
4. Image is sent to Gemini for extraction.
5. JSON result is validated.
6. User confirms or edits fields.
7. Transaction saves and widget updates.

### Journey C: Daily Reset

1. Midnight arrives.
2. WorkManager triggers reset logic.
3. App computes the new day’s budget state.
4. Widget refreshes with the new totals.

## 13) Technical Design Requirements

### Architecture

- Clean Architecture.
- MVVM presentation layer.
- Event-driven state updates.
- Local-only persistence.

### Persistence

- Room Database for transactions, budgets, settings, and parsed events.
- No cloud sync in MVP.

### Background Processing

- WorkManager for midnight reset.
- No polling loop.
- Update only on events:
  - notification received,
  - OCR completed,
  - transaction saved,
  - budget changed,
  - midnight reset.

### AI Usage

- Gemini API used only for manual OCR extraction.
- AI should not be required for the core notification flow.
- AI responses must be parsed as structured JSON.

### Privacy

- Store sensitive data locally.
- Do not upload notifications.
- Do not retain receipt images longer than needed unless user explicitly opts in.

## 14) Data Model

### Transaction

- `id`
- `timestamp`
- `amount`
- `merchant`
- `source` (`notification`, `ocr`, `manual`)
- `type` (`expense`, `income`)
- `rawText`
- `confidence`
- `category` optional

### Budget

- `id`
- `dailyLimit`
- `rolloverEnabled`
- `rolloverBalance`
- `timezone`

### Scan Session

- `id`
- `imageUri`
- `extractedJson`
- `parseStatus`
- `createdAt`

### Settings

- notification access enabled
- camera permission granted
- Gemini API key / config
- supported bank apps
- widget preferences

## 15) Business Rules

- Every captured expense reduces today’s remaining budget.
- Income can increase available balance if the user chooses to track it.
- If the user has no daily budget set, the widget should prompt setup.
- The widget should always reflect the latest local data.
- Parsing confidence should not block save unless data is clearly invalid.

## 16) Error Handling

### Notification Parsing Failure

- Preserve raw message text.
- Mark transaction as “needs review”.
- Allow manual correction.

### Gemini OCR Failure

- Offer retry.
- Offer manual editing.
- Fallback to text-based heuristic extraction if available.

### Permission Denied

- Explain why notification and camera permissions matter.
- Offer a path to settings.

### Widget Update Failure

- Retry widget refresh after save.
- Keep data in Room so widget can recover later.

## 17) Risks and Mitigations

| Risk | Impact | Mitigation |
|---|---|---|
| Notification formats change | Parsing breaks | Use fallback parsing + raw text review |
| AI parsing errors | Incorrect amounts | Multi-step JSON validation + user confirmation |
| Battery drain | Poor UX | Event-driven only, no polling |
| Privacy concerns | Low trust | Keep data local-only by default |
| Widget update lag | Stale data | Refresh on every save and midnight reset |
| Duplicate transactions | Wrong budget totals | Add future duplicate detection heuristic |

## 18) Analytics / Measurement

Track only privacy-safe product metrics locally or in aggregate if future opt-in analytics is added.

### Key Metrics

- Daily active users.
- Widget opens or interactions.
- Auto-capture success rate.
- OCR success rate.
- Manual fallback rate.
- Retention after 7 days and 30 days.
- Number of users who complete budget setup.

### Product Health Signals

- Percentage of transactions captured automatically.
- Average time from event to widget refresh.
- Number of parse failures per supported bank app.

## 19) Acceptance Criteria

The MVP is successful when:

- A user can set a daily budget and see it on the widget.
- A supported bank notification can be parsed into a saved transaction.
- A receipt can be scanned and converted into a saved transaction.
- The widget updates after every save.
- Daily totals reset correctly at midnight local time.
- All critical data remains on-device.

## 20) Release Plan

### Phase 1: Core Foundation

- Set up Room data layer.
- Implement daily budget logic.
- Build widget summary.
- Add manual entry.

### Phase 2: Automation

- Implement notification listener.
- Add parsing pipeline.
- Update widget after auto-capture.

### Phase 3: OCR

- Add camera capture.
- Integrate Gemini OCR extraction.
- Build validation and fallback flow.

### Phase 4: Hardening

- Improve error handling.
- Add duplicate detection.
- Add better review/edit flows.

## 21) Future Enhancements

- Duplicate detection.
- Weekly analytics.
- Smart category AI.
- Budget alerts.
- Optional cloud backup.
- Trend insights by merchant or category.

## 22) Open Questions

- Which banking apps should be supported first?
- Should income affect the daily remaining budget or be tracked separately?
- Should rollover be on by default?
- How long should receipt images be retained after OCR?
- Should the widget support one-tap scan directly into camera?

## 23) Appendix: Product Summary

Zero-Touch Daily Budgeting App is a privacy-first, widget-first budgeting product that automatically captures spending from notifications and OCR, calculates a daily budget in real time, and minimizes user effort by design.
