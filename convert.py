#!/usr/bin/env python3
"""Remove hard line endings from Android strings.xml values.

Literal escape sequences like \n, \t etc. are preserved as-is.
Real (hard) newlines inside string values are collapsed to a single space.
The XML declaration and <resources> tag are left completely untouched.
"""
from __future__ import annotations

import logging
import re
import sys
from pathlib import Path

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s %(levelname)-5s %(message)s",
    datefmt="%Y-%m-%d %H:%M:%S",
)

try:
    from lxml import etree
except ImportError:
    logging.error("lxml is not installed. Run: pip install lxml")
    sys.exit(1)


def process_file(path: Path) -> None:
    original_text = path.read_text(encoding="utf-8")

    parser = etree.XMLParser(remove_blank_text=False)
    root = etree.fromstring(original_text.encode("utf-8"), parser)

    # collect (original_value, fixed_value) pairs for all affected elements
    replacements: list[tuple[str, str]] = []

    for tag in ("string", "item"):
        for elem in root.iter(tag):
            if elem.text and re.search(r"[\r\n]", elem.text):
                original_value = elem.text
                fixed_value = re.sub(r"[ \t]*[\r\n]+[ \t]*", " ", original_value)
                replacements.append((original_value, fixed_value))
                logging.warning(
                    "%s: hard line ending removed from %r. Before: %r  After: %r",
                    path,
                    elem.attrib.get("name", elem.tag),
                    original_value,
                    fixed_value,
                )

    if not replacements:
        logging.info("%s: nothing to fix.", path)
        return

    # apply replacements as plain text substitutions on the original file content
    fixed_text = original_text
    for original_value, fixed_value in replacements:
        fixed_text = fixed_text.replace(original_value, fixed_value, 1)

    path.write_text(fixed_text, encoding="utf-8")
    logging.info("%s: %d value(s) fixed, file written.", path, len(replacements))


def main() -> None:
    if len(sys.argv) < 2:
        print(f"Usage: {sys.argv[0]} strings.xml [strings2.xml ...]")
        sys.exit(1)

    for arg in sys.argv[1:]:
        path = Path(arg)
        if not path.is_file():
            logging.error("File not found: %s", path)
            sys.exit(1)
        process_file(path)


if __name__ == "__main__":
    main()
