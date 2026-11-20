#!/usr/bin/env bash
# ==============================================================================
# Install Git Hooks for mdd TV Governance Automation
# ==============================================================================

set -e

ROOT_DIR="$(git rev-parse --show-toplevel)"
HOOKS_SRC="$ROOT_DIR/tools/hooks"
HOOKS_DEST="$ROOT_DIR/.git/hooks"

if [ ! -d "$HOOKS_SRC" ]; then
    echo "Error: Hooks source directory not found at $HOOKS_SRC"
    exit 1
fi

echo "Installing Git governance hooks to $HOOKS_DEST..."

cp "$HOOKS_SRC/pre-commit" "$HOOKS_DEST/pre-commit"
cp "$HOOKS_SRC/commit-msg" "$HOOKS_DEST/commit-msg"

chmod +x "$HOOKS_DEST/pre-commit"
chmod +x "$HOOKS_DEST/commit-msg"
chmod +x "$HOOKS_SRC/pre-commit"
chmod +x "$HOOKS_SRC/commit-msg"

echo "Git governance hooks successfully installed!"
echo "  - pre-commit: Enforces Zero Emoji and path naming standards."
echo "  - commit-msg: Enforces Conventional Commits and Zero Emoji in commit messages."
