# Google Play Families Policy Compliance Checklist (Sky Fury)

Last reviewed: 2026-05-13

## 1. Audience and content

1. Play Console target audience includes children under 13 only if intended.
2. All app visuals/text/audio are child-appropriate.
3. No explicit sexual content, graphic violence, gambling, alcohol, or drug promotion.

## 2. Ads requirements

1. Only Google Play certified ad networks are used.
2. Current SDKs in app:
   - `com.google.android.gms:play-services-ads` (AdMob)
3. Ad requests are configured for child-directed treatment in code:
   - `TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE`
   - `TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE`
   - `MAX_AD_CONTENT_RATING_G`
   - non-personalized ads (`npa=1`)
4. Ad format and frequency are appropriate for children and do not mislead users.

## 3. Privacy and legal alignment (COPPA / GDPR)

1. Public privacy policy URL is live over HTTPS and no login required.
2. Privacy policy states children-related handling clearly and matches app behavior.
3. In-app privacy screen and Play Console privacy URL point to the same page.
4. No unnecessary sensitive permissions requested (camera, microphone, contacts, location).
5. Data Safety answers exactly match actual SDK/data behavior.

## 4. App implementation spot-check

1. Verify `app/src/main/java/com/mavelastudio/skyfury/MainActivity.java` contains child-directed ad config.
2. Verify `app/src/main/res/values/strings.xml` privacy summary matches current policy.
3. Verify policy files are synced:
   - `privacy-policy.html`
   - `docs/privacy-policy/index.html`

## 5. Pre-release submission checks

1. Build and run smoke test.
2. Tap in-app `Privacy Policy` button and confirm it opens the live GitHub Pages URL.
3. Re-check Play Console sections before submitting:
   - Target audience and content
   - Ads
   - Data safety
   - App content policies

## 6. GitHub Pages publish reminder

1. Commit changes to `docs/privacy-policy/index.html`.
2. Push to the branch used by GitHub Pages (currently `main` + `/docs`).
3. Verify live page:
   - `https://mavelastudio11-creator.github.io/skyfury/privacy-policy/`
