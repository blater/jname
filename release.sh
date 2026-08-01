#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

usage() {
  cat <<'USAGE'
Usage: ./release.sh X.Y.Z

Set the Maven version, commit and push it, then create and push vX.Y.Z.
The tag starts the release and Homebrew publication workflow.
USAGE
}

die() {
  printf 'Error: %s\n' "$*" >&2
  exit 1
}

[[ "$#" -eq 1 ]] || { usage >&2; exit 2; }
version="$1"
[[ "$version" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]] || die "Version must use X.Y.Z format."

cd "$script_dir"
[[ -d .git ]] || die "This directory is not a Git repository."
[[ -z "$(git status --porcelain --untracked-files=normal)" ]] || \
  die "The Git working tree is not clean."

tag="v$version"
git rev-parse --verify --quiet "refs/tags/$tag" >/dev/null && die "Tag $tag already exists."
git ls-remote --exit-code --tags origin "refs/tags/$tag" >/dev/null 2>&1 && \
  die "Tag $tag already exists on origin."

mvn --batch-mode --no-transfer-progress versions:set \
  -DnewVersion="$version" -DgenerateBackupPoms=false
mvn --batch-mode --no-transfer-progress test

git add pom.xml
git commit -m "Release $tag"
git push
git tag -a "$tag" -m "Release $tag"
git push origin "$tag"

printf 'Release workflow started for %s.\n' "$tag"
