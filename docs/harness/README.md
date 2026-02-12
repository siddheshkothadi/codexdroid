# Harness Docs

This folder captures the feature contracts and operating playbooks for the harness-first delivery loop.

## Layout
- `specs/`: behavior contracts and acceptance checks.
- `playbooks/`: operational workflows for fast-loop development and releases.

## Core policy
- New behavior must be accompanied by a small, machine-checkable spec.
- Every PR should run the smoke suite.
- Protocol-affecting changes should run the protocol suite.
