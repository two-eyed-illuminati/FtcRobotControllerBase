# Tests

`pnpm test` loads every `.pp` in `fixtures/projects/` and compares the normalized document to
`golden/<fixture>/normalized.json`. When a change is meant to alter that output, run
`pnpm vitest run --update` and review the golden diff.
