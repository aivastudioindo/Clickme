#!/usr/bin/env bash
set -e
cd /root/android/clickme
STORE_PW=$(grep '^store_pw=' .signing_local | cut -d= -f2)
echo "pw len: ${#STORE_PW}"
gh secret set SIGNING_STORE_PASSWORD --repo aivastudioindo/Clickme --body "$STORE_PW"
gh secret set SIGNING_KEY_PASSWORD --repo aivastudioindo/Clickme --body "$STORE_PW"
gh secret set SIGNING_KEY_ALIAS --repo aivastudioindo/Clickme --body "clickme"
KEY_B64=$(base64 -w0 app/release-key.jks)
echo "key_b64 len: ${#KEY_B64}"
gh secret set SIGNING_STORE_FILE --repo aivastudioindo/Clickme --body "$KEY_B64"
echo "done"
