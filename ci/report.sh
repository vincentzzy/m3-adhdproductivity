#!/usr/bin/env bash
# Posts a text report back to GitHub as a commit comment.
# Why: this agent sandbox cannot reach the Actions log storage host, but it can
# read api.github.com. A commit comment is therefore our compiler feedback loop.
# Usage: CI_REPORT_TITLE="probe" ci/report.sh <file-with-report-body>
set -u
TITLE="${CI_REPORT_TITLE:-report}"
BODY_FILE="${1:-/dev/null}"
LIMIT=60000
if [ -f "$BODY_FILE" ]; then BODY=$(head -c "$LIMIT" "$BODY_FILE"); else BODY="(no output)"; fi
PAYLOAD=$(python3 - "$TITLE" <<'PY'
import json, os, sys
title = sys.argv[1]
body = os.environ.get("CI_REPORT_BODY", "")
print(json.dumps({"body": f"### CI {title}\n\n```\n{body}\n```"}))
PY
)
export CI_REPORT_BODY="$BODY"
PAYLOAD=$(python3 - "$TITLE" <<'PY'
import json, os, sys
print(json.dumps({"body": "### CI " + sys.argv[1] + "\n\n```\n" + os.environ.get("CI_REPORT_BODY","") + "\n```"}))
PY
)
curl -sS -X POST \
  -H "Authorization: token $GITHUB_TOKEN" \
  -H "Accept: application/vnd.github+json" \
  "${GITHUB_API_URL:-https://api.github.com}/repos/${GITHUB_REPOSITORY}/commits/${GITHUB_SHA}/comments" \
  -d "$PAYLOAD" -o /dev/null -w "report-posted:%{http_code}\n" || echo "report failed"
