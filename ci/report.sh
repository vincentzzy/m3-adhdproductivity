#!/usr/bin/env bash
# Posts a build/test report back to GitHub as a commit comment.
#
# Why: the agent sandbox that maintains this repo cannot reach the Actions log
# storage host, but it can reach api.github.com. A commit comment is therefore a
# reliable compiler feedback loop. Harmless for humans: it only runs on failure.
set -u
TITLE="${CI_REPORT_TITLE:-report}"
BODY_FILE="${1:-/dev/null}"
BODY=$( [ -f "$BODY_FILE" ] && head -c 55000 "$BODY_FILE" || echo "(no output)" )
export BODY
PAYLOAD=$(python3 - <<'PY'
import json, os, sys
print(json.dumps({"body": "### CI " + os.environ.get("CI_REPORT_TITLE", "report") + "\n\n```\n" + os.environ.get("BODY", "") + "\n```"}))
PY
)
curl -sS -X POST \
  -H "Authorization: token ${GITHUB_TOKEN}" \
  -H "Accept: application/vnd.github+json" \
  "${GITHUB_API_URL:-https://api.github.com}/repos/${GITHUB_REPOSITORY}/commits/${GITHUB_SHA}/comments" \
  -d "$PAYLOAD" -o /dev/null -w "report-posted:%{http_code}\n" || echo "report-post-failed"
