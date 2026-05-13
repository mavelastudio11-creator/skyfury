# GitHub Pages Setup (Sky Fury Privacy Policy)

Last updated: 2026-05-13

## 1. Put this project in a GitHub repository

If you already have a repo, use that. If not:

```bash
git init
git add .
git commit -m "Add Play-compliant privacy policy and billing integration"
git branch -M main
git remote add origin https://github.com/<your-username>/<your-repo>.git
git push -u origin main
```

## 2. Enable GitHub Pages

In GitHub:
1. Open repository -> Settings -> Pages
2. Source: `Deploy from a branch`
3. Branch: `main`
4. Folder: `/docs`
5. Save

## 3. Use the final public policy URL

If your repo is `skyfury`, URL becomes:

`https://mavelastudio11-creator.github.io/skyfury/privacy-policy/`

If your repo has a different name, replace `skyfury` accordingly.

## 4. Update app and Play Console

1. In app file `app/src/main/res/values/strings.xml`, set `privacy_policy_url` to that exact URL.
2. In Play Console, paste the same URL into the Privacy policy field.
3. Keep in-app Privacy Policy screen active (already implemented).

## 5. Replace contact placeholders before publishing

In `privacy-policy.html` and `docs/privacy-policy/index.html`, replace:
- `privacy@YOUR_DOMAIN.com`
- `support@YOUR_DOMAIN.com`

## 6. Quick verification

1. Open URL in incognito browser.
2. Confirm no login is required.
3. Confirm page is not PDF.
4. Confirm app name and developer name appear.
5. Confirm retention/deletion and contact sections are present.
