#!/usr/bin/env bash
#
# PreToolUse/Bash hook enforcing the no-commit rule at the top of AGENTS.md.
# This command-pattern guard supplements the agent instructions; it is not a shell sandbox.
#
# Widened copy of ~/.claude/hooks/block-git-push.sh. Overlap is harmless.

input="$(cat)"

# Git Bash ships no jq; without the sed fallback the command parses empty.
if command -v jq >/dev/null 2>&1; then
  command="$(printf '%s' "$input" | jq -r '.tool_input.command // ""')"
else
  command="$(
    printf '%s' "$input" |
      sed -n 's/.*"command"[[:space:]]*:[[:space:]]*"\(.*\)".*/\1/p' |
      sed -e 's/\\"/"/g' -e 's/\\\\/\\/g'
  )"
fi

# Fail closed: an unparsable payload must never slip through.
if [ -z "$command" ]; then
  echo "Blocked: failed to parse Claude tool command." >&2
  exit 2
fi

# Wrapped and quoted forms need a separator before git for the prefix to match.
normalized="$(printf '%s' "$command" | tr '"'\''()' '    ' | tr '\\' '/')"

# Also matches sudo/command prefixes, absolute paths and -C/-c options.
git_prefix='(^|[[:space:];|&])(sudo[[:space:]]+|command[[:space:]]+)?([^[:space:]]*/)?git(\.exe)?([[:space:]]+(-C|-c)[[:space:]]+[^[:space:]]+)*[[:space:]]+'
gh_prefix='(^|[[:space:];|&])(sudo[[:space:]]+|command[[:space:]]+)?([^[:space:]]*/)?gh(\.exe)?[[:space:]]+'

refuse() {
  echo "Blocked: $1" >&2
  echo "See the rule at the top of AGENTS.md: leave the change in the working tree and tell the user what is ready." >&2
  exit 2
}

# History writes and publishing.
if printf '%s' "$normalized" |
  grep -Eiq "${git_prefix}(commit|push|merge|rebase|cherry-pick|revert|am|stash|filter-branch)([[:space:]]|$)"
then
  refuse "Claude Code is not allowed to commit, push or rewrite git history in this repository."
fi

# Listing tags is read-only, so -l, --list and -n stay allowed.
if printf '%s' "$normalized" |
  grep -Eiq "${git_prefix}tag([[:space:]]+-(a|d|f|s|m|-delete|-force|-annotate|-sign)|[[:space:]]+[^-[:space:]])"
then
  refuse "creating or deleting tags is a write to the repository."
fi

# Discards uncommitted work.
if printf '%s' "$normalized" |
  grep -Eiq "${git_prefix}(reset[[:space:]]+[^;|&]*--hard|checkout[[:space:]]+--([[:space:]]|$)|restore([[:space:]]|$)|clean[[:space:]]+[^;|&]*-[a-z]*f)"
then
  refuse "this command would discard uncommitted work."
fi

# gh publishes too.
if printf '%s' "$normalized" |
  grep -Eiq "${gh_prefix}(pr[[:space:]]+(create|merge|close|ready)|release[[:space:]]+create|repo[[:space:]]+(create|delete))"
then
  refuse "opening, merging or closing pull requests is the user's decision."
fi

exit 0
