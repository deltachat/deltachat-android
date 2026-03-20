#!/usr/bin/env python3
"""Remove hard line endings from Android strings.xml values.

Literal escape sequences like \n, \t etc. are preserved as-is.
Real (hard) newlines inside string values are collapsed to a single space.
Multiple spaces/tabs left over after collapsing are also reduced to one space.
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


def fix_value(text: str) -> str:
    """Replace hard line endings (and surrounding whitespace) with a single space."""
    return re.sub(r"[ \t]*[\r\n]+[ \t]*", " ", text)


def process_file(path: Path) -> None:
    parser = etree.XMLParser(remove_blank_text=False)
    tree = etree.parse(path, parser)
    root = tree.getroot()

    changed = 0

    for tag in ("string", "item"):  # "item" covers plurals children
        for elem in root.iter(tag):
            if elem.text and re.search(r"[\r\n]", elem.text):
                original = elem.text
                elem.text = fix_value(original)
                logging.warning(
                    "%s: hard line ending removed from %r. Before: %r  After: %r",
                    path,
                    elem.attrib.get("name", elem.tag),
                    original,
                    elem.text,
                )
                changed += 1

    if changed:
        tree.write(
            path,
            encoding="utf-8",
            xml_declaration=True,
            pretty_print=True,
        )
        logging.info("%s: %d value(s) fixed, file written.", path, changed)
    else:
        logging.info("%s: nothing to fix.", path)


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
