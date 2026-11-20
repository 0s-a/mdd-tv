#!/usr/bin/env bash
set -e

# ==============================================================================
# Automated Release Engine for mdd TV
# Usage: ./tools/release.sh <version>
# Example: ./tools/release.sh 1.0.0
# ==============================================================================

TARGET_VERSION="$1"

if [ -z "$TARGET_VERSION" ]; then
    echo "Error: Version argument required (e.g., 1.0.0)"
    echo "Usage: ./tools/release.sh <version>"
    exit 1
fi

# Validate SemVer pattern (e.g. 1.0.0 or 1.2.3)
if ! [[ "$TARGET_VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
    echo "Error: Version must follow Semantic Versioning format (e.g., 1.0.0)"
    exit 1
fi

TAG_NAME="v${TARGET_VERSION}"
TODAY=$(date +%Y-%m-%d)

echo "=================================================="
echo "Starting Automated Release Pipeline: ${TAG_NAME}"
echo "=================================================="

# 1. Run local silent verification
echo "[1/6] Running silent local unit tests..."
./gradlew testDebugUnitTest --no-daemon

# 2. Update app/build.gradle.kts
echo "[2/6] Updating version in app/build.gradle.kts..."
python3 -c "
import re
path = 'app/build.gradle.kts'
with open(path, 'r') as f:
    content = f.read()

# Update versionName
content = re.sub(r'versionName\s*=\s*\"[^\"]+\"', f'versionName = \"$TARGET_VERSION\"', content)

# Increment versionCode
def increment_code(match):
    old_code = int(match.group(1))
    return f'versionCode = {old_code + 1}'

content = re.sub(r'versionCode\s*=\s*(\d+)', increment_code, content)

with open(path, 'w') as f:
    f.write(content)
"

# 3. Flush docs/UNRELEASED.md into CHANGELOG.md if not already present
echo "[3/6] Synchronizing release notes and CHANGELOG.md..."
python3 -c "
import re
target_version = '$TARGET_VERSION'
today = '$TODAY'

with open('CHANGELOG.md', 'r') as f:
    changelog = f.read()

# Check if version section already exists in CHANGELOG.md
if f'## [{target_version}]' not in changelog:
    # Read UNRELEASED.md
    with open('docs/UNRELEASED.md', 'r') as f:
        unreleased = f.read()
    
    # Strip headers from UNRELEASED
    body = re.sub(r'^# [^\n]*\n', '', unreleased)
    body = re.sub(r'^# [^\n]*\n', '', body)
    body = re.sub(r'^> [^\n]*\n', '', body).strip()
    
    new_entry = f'## [{target_version}] - {today}\n\n' + body + '\n\n---\n\n'
    
    # Insert at top of changelog (after main title)
    header_end = changelog.find('---')
    if header_end != -1:
        changelog = changelog[:header_end + 3] + '\n\n' + new_entry + changelog[header_end + 3:].lstrip('\n-')
    else:
        changelog = changelog + '\n\n' + new_entry

    with open('CHANGELOG.md', 'w') as f:
        f.write(changelog)

    # Reset UNRELEASED.md
    with open('docs/UNRELEASED.md', 'w') as f:
        f.write('''# التعديلات المرحلية قيد التطوير (Unreleased Staging Delta)
# Working Delta Buffer for Next Release

> هذا الملف هو الخزان الانتقالي المؤقت؛ يسجل فيه الوكيل والمطور كل ميزة جديدة أو إصلاح أثناء دورة التطوير، ويتم تفريغه تلقائياً إلى التوثيق المستقر وسجل التغييرات عند إطلاق كل إصدار رسمي.

---

## الميزات الجديدة قيد الإعداد (Added)
* لا توجد ميزات قيد الانتظار حالياً.

---

## التحسينات والتعديلات (Changed)
* لا توجد تحسينات معلقة.

---

## الإصلاحات البرمجية (Fixed)
* لا توجد إصلاحات معلقة.
''')
"

# 4. Git Stage and Commit
echo "[4/6] Creating release commit..."
git add app/build.gradle.kts CHANGELOG.md docs/UNRELEASED.md docs/
git commit -m "chore(release): prepare release ${TAG_NAME}" || true

# 5. Create Git Tag
echo "[5/6] Creating git tag ${TAG_NAME}..."
if git rev-parse "$TAG_NAME" >/dev/null 2>&1; then
    echo "Tag $TAG_NAME already exists, skipping tag creation."
else
    git tag -a "$TAG_NAME" -m "Release ${TAG_NAME}"
fi

# 6. Push to GitHub
echo "[6/6] Pushing commits and tag to GitHub..."
git push origin main --tags

echo "=================================================="
echo "Release ${TAG_NAME} successfully pushed to GitHub!"
echo "Cloud GitHub Actions is now building and publishing APK:"
echo "https://github.com/0s-a/mdd-tv/actions"
echo "=================================================="
