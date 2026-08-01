#!/usr/bin/env bash
set -euo pipefail

source_repository="${JNAME_SOURCE_REPOSITORY:-blater/jname}"
tap_repository="${JNAME_TAP_REPOSITORY:-blater/homebrew-tap}"
script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
project_dir="$(cd "$script_dir/../.." && pwd)"

log() {
  printf '==> %s\n' "$*"
}

die() {
  printf 'Error: %s\n' "$*" >&2
  exit 1
}

[[ -n "${HOMEBREW_TAP_TOKEN:-}" ]] || die "HOMEBREW_TAP_TOKEN is required."
command -v gh >/dev/null 2>&1 || die "gh is required but was not found on PATH."

cd "$project_dir"
for workflow in .github/workflows/ci.yml .github/workflows/release.yml; do
  [[ -f "$workflow" ]] || die "Required workflow file is missing: $workflow"
done

log "Checking GitHub CLI authentication"
gh auth status >/dev/null

actual_repository="$(gh repo view "$source_repository" --json nameWithOwner --jq .nameWithOwner)"
[[ "$actual_repository" == "$source_repository" ]] || \
  die "Expected source repository $source_repository, found $actual_repository."

log "Checking Homebrew token access to $tap_repository"
tap_can_push="$(
  GH_TOKEN="$HOMEBREW_TAP_TOKEN" \
    gh api "repos/$tap_repository" --jq '.permissions.push // false'
)" || die "HOMEBREW_TAP_TOKEN cannot access $tap_repository."
[[ "$tap_can_push" == "true" ]] || \
  die "HOMEBREW_TAP_TOKEN needs Contents: Read and write access to $tap_repository."

log "Enabling GitHub Actions for $source_repository"
gh api \
  --method PUT \
  "repos/$source_repository/actions/permissions" \
  -F enabled=true \
  -f allowed_actions=all \
  --silent

log "Storing HOMEBREW_TAP_TOKEN as a repository Actions secret"
printf '%s' "$HOMEBREW_TAP_TOKEN" |
  gh secret set HOMEBREW_TAP_TOKEN --repo "$source_repository" --app actions

log "GitHub Actions setup complete"
printf '%s\n' \
  "Workflows: .github/workflows/ci.yml, .github/workflows/release.yml" \
  "Release trigger: push a v<version> tag, or dispatch Release with an existing tag."
