# Google Play Privacy Policy Checklist (Sky Fury)

Last reviewed: 2026-05-13

## Required before submitting

1. Host `privacy-policy.html` at a public HTTPS URL:
   - Active and publicly accessible
   - Non-geofenced
   - No login required
   - Not a PDF

2. Replace placeholders in `privacy-policy.html`:
   - `privacy@YOUR_DOMAIN.com`
   - `support@YOUR_DOMAIN.com`
   - Any other `YOUR_DOMAIN` values

3. Put the exact same public URL in:
   - Play Console -> App content -> Privacy policy URL field
   - `app/src/main/res/values/strings.xml` -> `privacy_policy_url`

4. Confirm in-app access:
   - Open app -> Settings -> `PRIVACY POLICY` button
   - Tap `Open Full Policy` and verify it opens the public URL

5. Verify policy content aligns with app behavior:
   - Ads SDK usage (AdMob)
   - Play Billing usage
   - Local game data storage
   - Security practices
   - Retention/deletion policy
   - Contact mechanism

6. Complete Data safety in Play Console so it matches policy text.

## If app behavior changes

Update all three together:
1. Privacy policy page content
2. In-app privacy screen/summary
3. Play Console Data safety answers
