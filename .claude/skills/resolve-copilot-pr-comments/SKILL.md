---
name: review-pr-comments
description: Review and fix Copilot/reviewer comments on the current PR. Reads all review comments, assesses validity, fixes issues, runs checks, pushes code, and replies to comments.
---

# Resolve PR Comments

Process for handling review comments (Copilot or human) on the current branch's PR.

## Steps

1. **Find the PR** — determine the current branch and find its open PR using `gh pr list` or MCP GitHub tools.

2. **Read all review comments** — use `mcp__github__pull_request_read` to get all review comments on the PR. Extract
   comment IDs from `html_url` (`#discussion_r<id>`).

3. **Assess validity** — for each comment:
    - Determine if it's valid (real issue) or false positive
    - Categorize: critical / important / minor / false-positive
    - Note the file and line

4. **Print report** — output a table with columns: #, File, Category, Summary, Action. Let the user review before
   proceeding.

5. **Fix issues** — apply fixes for all valid comments (not just critical). Follow project coding style rules from
   `.claude/rules/`.

6. **Run checks** — execute `/check-all` or equivalent: lint + tests for all affected services.

7. **Push code** — ALWAYS use `mcp__github__push_files` to push changed files (and `mcp__github__delete_file` for deleted files). Never use `git push` — SSH keys are not available and it will always fail.

8. **Reply to comments** — use `mcp__github__add_reply_to_pull_request_comment` to reply to each comment explaining what
   was fixed (or why it's a false positive).

## Notes

- Comments are replied from the user's GitHub account (gh CLI uses the user's auth token).
- When fixing error handling: catch specific exceptions, never leak internal details to the client.
- When fixing thread safety: use `ThreadedConnectionPool` for psycopg2, `threading.Lock` for shared mutable state.
- Always run `ruff check` and `ruff format --check` after Python changes.
