#!/bin/bash

# Fix all checkstyle issues systematically

echo "Fixing checkstyle issues..."

# 1. Fix ParenPad - remove space after ( and before )
find OpenWIGLibrary/src -name "*.java" -exec perl -i -pe 's/\(\s+/(/g; s/\s+\)/)/g' {} \;

# 2. Fix WhitespaceAfter for typecast (already done by Python but double-check)
# This is tricky, skip for now

# 3. Fix star imports by expanding them
# Skip for now as it requires knowing what classes are used

echo "First pass complete. Remaining issues:"
cd OpenWIGLibrary && gradle checkstyle 2>&1 | grep "\[INFO\]" | wc -l

