# Publishing StudyGalaxy to Google Play

Three real steps. All free except Google Play's **one-time $25** developer registration.

## Step 1 — Deploy the backend (free, HTTPS, 10 min)

The app needs a public HTTPS backend URL before you can ship it.

### Render (recommended, easiest)

1. Push the whole `studygalaxy/` folder to a GitHub repo (free).
2. Go to [render.com](https://render.com) → New → Blueprint → connect your repo.
3. Render reads `backend/render.yaml` and provisions the server with a free plan + a 1 GB disk for SQLite.
4. After deploy, set `CORS_ORIGIN` in the Render env panel to your frontend URL (step 2).
5. Your backend URL will look like `https://studygalaxy-backend.onrender.com`.

Free Render tier spins down after 15 min idle and wakes on first request (~30 sec cold start). Fine for an MVP.

### Alternatives

- **Fly.io** — free tier with persistent volume for SQLite, no spin-down.
- **Railway** — simplest UI but free tier is time-limited.
- **Self-host** — run on your home PC with [Cloudflare Tunnel](https://developers.cloudflare.com/cloudflare-one/connections/connect-networks/) (free, gives you HTTPS).

## Step 2 — Deploy the frontend (free, HTTPS, 5 min)

### Netlify (recommended)

1. Same GitHub repo.
2. [netlify.com](https://netlify.com) → Add new site → Import from Git.
3. Netlify reads `frontend/netlify.toml`. Build command and publish dir are already set.
4. In site settings → Environment variables → add `VITE_API_BASE` = your Render backend URL.
5. Deploy. You get `https://studygalaxy.netlify.app` or your custom domain.

### Alternatives: Vercel (uses `frontend/vercel.json`), Cloudflare Pages, GitHub Pages.

Test on your phone: open the Netlify URL in Chrome → "Add to Home Screen". The PWA should install, work offline, and look native. If this works, Play Store packaging will work.

## Step 3 — Build the Android app (free tools, ~30 min)

### Prerequisites (install once)

- **Android Studio** (free) — https://developer.android.com/studio
- **JDK 17** — included with Android Studio since Giraffe (2023).

### Generate icons (one command)

```powershell
cd "C:\Users\Mayank Vyas\Documents\studygalaxy\frontend"
npm install          # includes sharp + capacitor packages
npm run icons        # creates all required PNGs in public/icons/
```

This generates:
- `icon-192.png`, `icon-512.png` (PWA)
- `icon-maskable-*.png` (Android adaptive icons)
- `play-store-icon-512.png` (Play Store listing)
- `feature-graphic.png` (1024×500 banner for the store page)

### Set the production API URL

Create `frontend/.env.production`:
```
VITE_API_BASE=https://studygalaxy-backend.onrender.com
```

### Build the web app + wrap it with Capacitor

```powershell
cd "C:\Users\Mayank Vyas\Documents\studygalaxy\frontend"
npm run android:init   # builds Vite output, creates android/ project, syncs
npm run android:open   # opens Android Studio with the generated project
```

In Android Studio:
1. Wait for Gradle sync to finish (first time: 5–10 min).
2. **Build → Generate Signed Bundle / APK → Android App Bundle (.aab)**.
3. Create a new keystore (save this file + password forever — losing it means you can never update your app).
4. Build. You get `app-release.aab` — this is what you upload to Play Store.

To re-build after code changes: `npm run android:sync` then rebuild in Android Studio.

## Step 4 — Play Store listing (~1 hour, then 1–3 days review)

### Register the account

1. [play.google.com/console](https://play.google.com/console) → sign up → **pay $25 one-time**.
2. Choose "Personal" account unless you have a registered business.
3. Verify ID (government-issued; takes 1–2 days for Google to approve the account).

### Create the app

In Play Console → Create app:
- **Name**: StudyGalaxy
- **Default language**: English (United States)
- **App or game**: App
- **Free or paid**: Free
- Agree to the Developer Program Policies.

### Required listing content

Use these ready-to-paste strings (short + long description are in `STORE_LISTING.md` next to this file).

- **App icon** → `frontend/public/icons/play-store-icon-512.png`
- **Feature graphic** → `frontend/public/icons/feature-graphic.png`
- **Phone screenshots** (at least 2, min 1080×1920): take with Android emulator or your phone, from the running app. Suggested shots: Home screen, Timer running, Galaxy with planets, Circles screen.
- **Short description** (80 chars): see `STORE_LISTING.md`
- **Full description** (4000 chars max): see `STORE_LISTING.md`
- **Category**: Education → Study aids
- **Tags**: productivity, focus, study, pomodoro, students

### Required compliance screens in Play Console

- **Privacy policy URL**: your frontend URL + `/privacy-policy.html` OR link to the in-app privacy screen hosted at your Netlify URL. The app also has a Privacy screen reachable from the Home footer.
- **Data safety form**: declare what you collect:
  - Device ID: Yes (for account recovery and syncing)
  - User-provided names: Yes (cosmic name)
  - App activity: Yes (sessions, streaks)
  - **Nothing else.** No PII, no location, no camera data stored.
- **Target audience**: 13+ (Family program not applicable since not targeting under-13).
- **Ads**: No ads.
- **Content rating questionnaire**: answer honestly (none of the questions should apply → Everyone rating).

### Upload the AAB

Testing track first (recommended):
1. Play Console → Testing → **Internal testing** → create release.
2. Upload `app-release.aab`.
3. Add 1–10 testers by email (yourself + friends). They install via the opt-in link.
4. Verify everything works for 2–3 days.
5. Promote the release to **Production** when ready.

Google reviews production releases in 1–3 days typically.

## Step 5 — After launch

- **Updates**: bump `android/app/build.gradle` versionCode by 1 for every release. Build new AAB. Upload to a new production release.
- **Monitoring**: Play Console Vitals shows crashes and ANRs. Fix anything above 1.09% (Google's bad behavior threshold).
- **Reviews**: respond to the first 20 reviews personally. Users love it.

## Backups you need to protect

- **Android keystore file + password** — losing this means you can never update your app on the same package name.
- **Play Console account recovery** — enable 2FA with a backup phone.
- **Server backups** — the SQLite file is user data. On Render, snapshot the disk weekly: either via the dashboard or by cron-copying it to a free S3-compatible store (Backblaze B2 has a free tier).

## Cost summary

| Item | One-time | Recurring |
|---|---|---|
| Play Console registration | $25 | — |
| Render backend | — | $0 (free tier) |
| Netlify frontend | — | $0 (free tier) |
| Icons / design | — | $0 (generated from script) |
| Android Studio / tools | — | $0 |
| **Total** | **$25** | **$0/mo** |

Any paid upgrades (custom domain, Render paid plan with no spin-down) are optional and come later only if you get real traction.
