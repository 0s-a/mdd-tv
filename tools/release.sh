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

# 1. Verification Strategy
RUN_LOCAL_TEST=false
for arg in "$@"; do
    if [ "$arg" == "--test" ]; then
        RUN_LOCAL_TEST=true
    fi
done

if [ "$RUN_LOCAL_TEST" = true ]; then
    echo "[1/5] Running local unit tests..."
    ./gradlew testDebugUnitTest --no-daemon
else
    echo "[1/5] Local test execution skipped to conserve resources."
    echo "      (Cloud CI/CD runner will execute complete test suite and build on GitHub Actions)."
fi

# 2. Update app/build.gradle.kts and AppConfig.kt
echo "[2/5] Updating version in app/build.gradle.kts and AppConfig.kt..."
python3 -c "
import os, re
path = 'app/build.gradle.kts'
with open(path, 'r') as f:
    content = f.read()

current_version_match = re.search(r'versionName\s*=\s*\"([^\"]+)\"', content)
current_version = current_version_match.group(1) if current_version_match else ''

if current_version != '$TARGET_VERSION':
    content = re.sub(r'versionName\s*=\s*\"[^\"]+\"', f'versionName = \"$TARGET_VERSION\"', content)
    def increment_code(match):
        old_code = int(match.group(1))
        return f'versionCode = {old_code + 1}'
    content = re.sub(r'versionCode\s*=\s*(\d+)', increment_code, content)

with open(path, 'w') as f:
    f.write(content)

app_config_path = 'app/src/main/kotlin/org/jellyfin/mobile/AppConfig.kt'
if os.path.exists(app_config_path):
    with open(app_config_path, 'r') as f:
        cfg = f.read()
    cfg = re.sub(r'APP_VERSION\s*=\s*\"[^\"]+\"', f'APP_VERSION = \"$TARGET_VERSION\"', cfg)
    with open(app_config_path, 'w') as f:
        f.write(cfg)
"

# 3. Flush docs/UNRELEASED.md into CHANGELOG.md if not already present
echo "[3/5] Synchronizing release notes and CHANGELOG.md..."
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
    
    first_h2 = unreleased.find('\n## ')
    if first_h2 != -1:
        body = unreleased[first_h2+1:].strip()
    else:
        body = unreleased.strip()

    body = re.sub(r'(?m)^## ', '### ', body)
    body = re.sub(r'(?m)^\s*---\s*$', '', body)
    body = re.sub(r'\n{3,}', '\n\n', body)
    body = body.replace('### الميزات الجديدة قيد الإعداد (Added)', '### الميزات المضافة (Added)')
    body = body.replace('### التغييرات المعمارية أو المحذوفة (Deprecated / Removed)', '### التغييرات المعمارية والمحذوفة (Deprecated / Removed)')
    body = body.strip()
    
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
echo "[4/5] Preparing git state..."
git add app/build.gradle.kts app/src/main/kotlin/org/jellyfin/mobile/AppConfig.kt CHANGELOG.md docs/UNRELEASED.md docs/ tools/release.sh
if ! git diff --cached --quiet; then
    git commit -m "chore(release): prepare release ${TAG_NAME}"
fi

# 5. Create Git Tag and Push to GitHub
echo "[5/5] Creating git tag ${TAG_NAME} and pushing to GitHub..."
if git rev-parse "$TAG_NAME" >/dev/null 2>&1; then
    echo "Tag $TAG_NAME already exists, pushing existing tag..."
else
    git tag -a "$TAG_NAME" -m "Release ${TAG_NAME}"
fi

git push origin main "$TAG_NAME"

echo "=================================================="
echo "Release ${TAG_NAME} successfully pushed to GitHub!"
echo "Cloud GitHub Actions is now building and publishing APK:"
echo "https://github.com/0s-a/mdd-tv/actions"
echo "=================================================="
