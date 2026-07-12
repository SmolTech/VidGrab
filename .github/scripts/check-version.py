#!/usr/bin/env python3
"""Verify that a version tag matches build.gradle.kts and F-Droid metadata."""

import os
import re
import sys
from pathlib import Path


def fail(message: str) -> None:
    print(f"::error::{message}")
    sys.exit(1)


def main() -> None:
    tag = os.environ.get("GITHUB_REF_NAME", "")
    if not tag.startswith("v"):
        fail(f"Tag '{tag}' must start with 'v'")
    tag_version = tag[1:]

    app_build = Path("VidGrab/app/build.gradle.kts").read_text()

    version_name_match = re.search(r'versionName\s*=\s*"([^"]+)"', app_build)
    if not version_name_match:
        fail("Could not find versionName in VidGrab/app/build.gradle.kts")
    version_name = version_name_match.group(1)

    if version_name != tag_version:
        fail(
            f"Tag version '{tag_version}' does not match "
            f"build.gradle.kts versionName '{version_name}'"
        )

    version_code_match = re.search(r'versionCode\s*=\s*(\d+)', app_build)
    if not version_code_match:
        fail("Could not find versionCode in VidGrab/app/build.gradle.kts")
    version_code = version_code_match.group(1)

    metadata = Path("metadata/us.smoltech.vidgrab.yml").read_text()

    current_version_match = re.search(
        r'^CurrentVersion:\s*["\']?([^"\'\n]+)["\']?$',
        metadata,
        re.MULTILINE,
    )
    if not current_version_match:
        fail("Could not find CurrentVersion in metadata/us.smoltech.vidgrab.yml")
    current_version = current_version_match.group(1).strip()

    if current_version != version_name:
        fail(
            f"metadata CurrentVersion '{current_version}' does not match "
            f"build.gradle.kts versionName '{version_name}'"
        )

    current_vercode_match = re.search(
        r'^CurrentVersionCode:\s*(\d+)$',
        metadata,
        re.MULTILINE,
    )
    if not current_vercode_match:
        fail("Could not find CurrentVersionCode in metadata/us.smoltech.vidgrab.yml")
    current_vercode = current_vercode_match.group(1)

    if current_vercode != version_code:
        fail(
            f"metadata CurrentVersionCode '{current_vercode}' does not match "
            f"build.gradle.kts versionCode '{version_code}'"
        )

    print(
        f"Version check passed: {tag} -> versionName={version_name}, "
        f"versionCode={version_code}"
    )


if __name__ == "__main__":
    main()
