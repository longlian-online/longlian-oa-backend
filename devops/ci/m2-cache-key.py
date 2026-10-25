#!/usr/bin/env python3
"""Hash Maven inputs, ignoring the release version and SCM tag.

maven-release-plugin rewrites <version> and <tag> on every tag. Those bytes
must not change the Actions cache key, or each release stores another copy
of the local repository.
"""

import hashlib
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parents[2]
POMS = (
    ROOT / "pom.xml",
    ROOT / "common" / "pom.xml",
    ROOT / "app" / "pom.xml",
    ROOT / "generator" / "pom.xml",
)


def project_version() -> str:
    text = (ROOT / "pom.xml").read_text()
    match = re.search(
        r"<artifactId>longlian-oa-backend</artifactId>\s*<version>([^<]+)</version>",
        text,
    )
    if not match:
        sys.exit("root project version not found")
    return match.group(1)


def normalize(text: str, version: str) -> str:
    text = text.replace(f"<version>{version}</version>", "<version>STABLE</version>")
    return re.sub(r"<tag>[^<]*</tag>", "<tag>STABLE</tag>", text)


def cache_key() -> str:
    version = project_version()
    digest = hashlib.sha256()
    for path in POMS:
        digest.update(path.relative_to(ROOT).as_posix().encode())
        digest.update(b"\0")
        digest.update(normalize(path.read_text(), version).encode())
        digest.update(b"\0")
    digest.update((ROOT / "settings.xml").read_bytes())
    return digest.hexdigest()


if __name__ == "__main__":
    print(cache_key())
