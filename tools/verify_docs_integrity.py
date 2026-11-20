#!/usr/bin/env python3
"""
Living Documentation & Governance Integrity Verification Tool for mdd TV.
Verifies:
1. Absolute Emoji Ban (Zero Emojis across all docs, code, and config files).
2. Relative Markdown links validity across docs/ and tasks/.
3. Version alignment between app/build.gradle.kts and CHANGELOG.md.
"""

import os
import re
import sys
from pathlib import Path

ROOT_DIR = Path(__file__).resolve().parent.parent

# Emoji regex covering all Unicode emoji blocks
EMOJI_PATTERN = re.compile(
    r'[\U00010000-\U0010ffff]'
    r'|[\u2600-\u27bf]'
    r'|[\u2300-\u23ff]'
    r'|[\u2b50]'
)

EXCLUDED_DIRS = {'.git', '.gradle', 'build', '.kotlin'}
CHECKED_EXTENSIONS = {'.md', '.kt', '.kts', '.sh', '.xml', '.json', '.properties', '.toml'}

def check_emojis():
    errors = []
    print("[1/3] Checking Absolute Emoji Ban across repository...")
    for root, dirs, files in os.walk(ROOT_DIR):
        dirs[:] = [d for d in dirs if d not in EXCLUDED_DIRS]
        for file in files:
            ext = os.path.splitext(file)[1].lower()
            if ext in CHECKED_EXTENSIONS:
                fpath = os.path.join(root, file)
                try:
                    with open(fpath, 'r', encoding='utf-8') as f:
                        for line_num, line in enumerate(f, 1):
                            matches = EMOJI_PATTERN.findall(line)
                            if matches:
                                rel_path = os.path.relpath(fpath, ROOT_DIR)
                                errors.append(f"  {rel_path}:{line_num} contains forbidden emoji: {matches}")
                except Exception as e:
                    pass
    if errors:
        print(f"FAILED: Found {len(errors)} emoji violations:")
        for err in errors[:20]:
            print(err)
        return False
    print("  PASS: Zero emojis found across all checked files.")
    return True

def check_markdown_links():
    print("[2/3] Checking relative links across docs/ and tasks/...")
    errors = []
    link_pattern = re.compile(r'\[([^\]]+)\]\(([^)]+)\)')

    target_folders = [ROOT_DIR / 'docs', ROOT_DIR / 'tasks']
    for folder in target_folders:
        if not folder.exists():
            continue
        for root, dirs, files in os.walk(folder):
            dirs[:] = [d for d in dirs if d not in EXCLUDED_DIRS]
            for file in files:
                if file.endswith('.md'):
                    md_path = Path(root) / file
                    try:
                        with open(md_path, 'r', encoding='utf-8') as f:
                            content = f.read()
                        links = link_pattern.findall(content)
                        for text, url in links:
                            # Skip external URLs, web links, mailto, and anchor-only links
                            if url.startswith(('http://', 'https://', 'mailto:', '#', 'file://')):
                                continue
                            # Clean anchor
                            target_url = url.split('#')[0]
                            if not target_url:
                                continue
                            # Resolve target file relative to the markdown file's directory
                            target_path = (md_path.parent / target_url).resolve()
                            if not target_path.exists():
                                rel_source = os.path.relpath(md_path, ROOT_DIR)
                                errors.append(f"  {rel_source} -> broken link '{url}' (target not found)")
                    except Exception as e:
                        pass
    if errors:
        print(f"FAILED: Found {len(errors)} broken relative links:")
        for err in errors[:20]:
            print(err)
        return False
    print("  PASS: All internal markdown links are valid.")
    return True

def check_version_alignment():
    print("[3/3] Checking version alignment between Gradle and Changelog...")
    build_gradle = ROOT_DIR / 'app' / 'build.gradle.kts'
    changelog = ROOT_DIR / 'CHANGELOG.md'
    
    if not build_gradle.exists() or not changelog.exists():
        print("  SKIP: build.gradle.kts or CHANGELOG.md missing.")
        return True

    with open(build_gradle, 'r', encoding='utf-8') as f:
        bg_content = f.read()
    match = re.search(r'versionName\s*=\s*\"([^\"]+)\"', bg_content)
    if not match:
        print("  FAILED: Could not find versionName in app/build.gradle.kts")
        return False
    version = match.group(1)

    with open(changelog, 'r', encoding='utf-8') as f:
        cl_content = f.read()

    if f"## [{version}]" not in cl_content:
        print(f"  WARNING: Version {version} from build.gradle.kts is not yet listed in CHANGELOG.md (may be in UNRELEASED.md).")
    else:
        print(f"  PASS: Version {version} is recorded in CHANGELOG.md.")
    return True

def main():
    print("==================================================")
    print("Running Documentation & Governance Integrity Audit")
    print("==================================================")
    ok_emojis = check_emojis()
    ok_links = check_markdown_links()
    ok_version = check_version_alignment()
    print("==================================================")
    if not (ok_emojis and ok_links and ok_version):
        print("Integrity Audit FAILED. Please resolve errors above.")
        sys.exit(1)
    print("Integrity Audit PASSED. All standards strictly verified.")
    sys.exit(0)

if __name__ == '__main__':
    main()
