# Feature Spec: User Input Request Response

## Goal
Support structured request/response loops for tool-driven user input.

## Contract
- `item/tool/requestUserInput` parses `questions[]` + `options[]`.
- Answers are returned in shape `{ answers: { questionId: { answers: string[] } } }`.

## Acceptance checks
- Invalid requests are routed to unknown-attention queue.
- Valid requests keep option labels and descriptions.
