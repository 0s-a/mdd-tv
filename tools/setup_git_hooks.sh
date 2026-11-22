#!/usr/bin/env bash
# إعداد خطافات Git للحوكمة وضبط الجودة ومنع الإيموجيات
# Setup Git Governance Hooks for mdd TV

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
HOOKS_DIR="$REPO_ROOT/.git/hooks"

if [ ! -d "$HOOKS_DIR" ]; then
    echo "Error: .git/hooks directory not found at $HOOKS_DIR"
    exit 1
fi

echo "Setting up pre-commit hook..."
cat << 'EOF' > "$HOOKS_DIR/pre-commit"
#!/usr/bin/env bash
# Git pre-commit hook: Absolute Emoji Ban and Naming Standards

set -e

# 1. فحص الملفات المجهزة للكوميت للتأكد من خلوها بنسبة 100% من الإيموجيات
STAGED_FILES=$(git diff --cached --name-only --diff-filter=ACM)

if [ -z "$STAGED_FILES" ]; then
    exit 0
fi

EMOJI_DETECTED=0

for FILE in $STAGED_FILES; do
    # استثناء الملفات الثنائية والصور
    if git check-attr -a "$FILE" | grep -q "binary"; then
        continue
    fi
    case "$FILE" in
        *.png|*.jpg|*.jpeg|*.webp|*.gif|*.ico|*.jar|*.apk|*.aab|*.so)
            continue
            ;;
    esac

    if [ -f "$FILE" ]; then
        python3 -c "
import sys
path = '$FILE'
found = False
try:
    with open(path, 'r', encoding='utf-8', errors='ignore') as f:
        for idx, line in enumerate(f, 1):
            for ch in line:
                if ord(ch) > 0x1F000:
                    print(f'GOVERNANCE ERROR: Emoji detected in {path}:{idx} -> {ch} (U+{ord(ch):04X})', file=sys.stderr)
                    found = True
                    break
            if found:
                break
except Exception as e:
    pass
if found:
    sys.exit(1)
" || EMOJI_DETECTED=1
    fi
done

if [ $EMOJI_DETECTED -ne 0 ]; then
    echo "=========================================================="
    echo "COMMIT REJECTED: Absolute Emoji Ban violation detected."
    echo "Please remove all emojis before committing."
    echo "=========================================================="
    exit 1
fi

exit 0
EOF

chmod +x "$HOOKS_DIR/pre-commit"

echo "Setting up commit-msg hook..."
cat << 'EOF' > "$HOOKS_DIR/commit-msg"
#!/usr/bin/env bash
# Git commit-msg hook: Enforce Conventional Commits and Emoji Ban

set -e

COMMIT_MSG_FILE="$1"
FIRST_LINE=$(head -n 1 "$COMMIT_MSG_FILE")

# 1. منع الإيموجيات في رسالة الكوميت
if python3 -c "
import sys
msg = '''$FIRST_LINE'''
for ch in msg:
    if ord(ch) > 0x1F000:
        sys.exit(1)
"; then
    :
else
    echo "=========================================================="
    echo "COMMIT REJECTED: Emojis are strictly banned in commit messages."
    echo "=========================================================="
    exit 1
fi

# 2. فحص نمط Conventional Commits
PATTERN="^(feat|fix|refactor|test|docs|chore|perf|build|ci)(\([a-zA-Z0-9_\-]+\))?: .+"

if [[ ! "$FIRST_LINE" =~ $PATTERN ]]; then
    echo "=========================================================="
    echo "COMMIT REJECTED: Commit message does not follow Conventional Commits standard."
    echo "Format: <type>(<scope>): <subject>"
    echo "Allowed types: feat, fix, refactor, test, docs, chore, perf, build, ci"
    echo "Example: feat(player): add volume boost gesture"
    echo "Received: $FIRST_LINE"
    echo "=========================================================="
    exit 1
fi

exit 0
EOF

chmod +x "$HOOKS_DIR/commit-msg"

echo "Git governance hooks successfully configured."
