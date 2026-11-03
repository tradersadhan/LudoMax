#!/usr/bin/env bash
# Fill in your details in the Privacy Policy / Terms / store listing.
# Usage: ./scripts/set_contact.sh "Your Name or Studio" "you@example.com" "India"
set -e
if [ "$#" -ne 3 ]; then
  echo 'Usage: ./scripts/set_contact.sh "Developer name" "contact@email" "Country/State for governing law"'
  exit 1
fi
cd "$(dirname "$0")/.."
NAME="$1"; MAIL="$2"; COUNTRY="$3"
FILES=$(grep -rl --include='*.html' --include='*.md' -e '{{DEVELOPER_NAME}}' -e '{{CONTACT_EMAIL}}' -e '{{COUNTRY}}' app/src/main/assets docs store README.md 2>/dev/null || true)
for f in $FILES; do
  sed -i "s|{{DEVELOPER_NAME}}|$NAME|g; s|{{CONTACT_EMAIL}}|$MAIL|g; s|{{COUNTRY}}|$COUNTRY|g" "$f"
  echo "updated $f"
done
