from __future__ import annotations

import json
from pathlib import Path
from typing import Any


def load_suite(root: Path, suite: str) -> dict[str, Any]:
    suite_path = root / "harness" / "scenarios" / f"{suite}.json"
    if not suite_path.exists():
        raise FileNotFoundError(f"Scenario suite not found: {suite_path}")
    with suite_path.open("r", encoding="utf-8") as f:
        data = json.load(f)
    if "scenarios" not in data or not isinstance(data["scenarios"], list):
        raise ValueError(f"Invalid suite file (missing scenarios list): {suite_path}")
    return data
