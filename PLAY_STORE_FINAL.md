# Submit StudyGalaxy to Play Store — final step-by-step

Everything on your side is ready. You handle the Google account + $25 fee + ID verification. These are the exact steps.

## ✅ What's already done (by me)

- Native Kotlin + Jetpack Compose app with all v2 features
- Release keystore generated and committed-safe at `android-native/keystore/studygalaxy-release.jks`
- Keystore password: `StudyGalaxy2026!`  — key alias: `studygalaxy` — key password: same
- Signed release AAB will land at: `android-native/app/build/outputs/bundle/release/app-release.aab`
- Store listing copy ready at `STORE_LISTING.md`
- Icons + feature graphic ready at `frontend/public/icons/`

## 🔐 CRITICAL — Back up the keystore NOW

Before you do anything else, make 3 copies of this file:
```
C:\Users\Mayank Vyas\Documents\studygalaxy\android-native\keystore\studygalaxy-release.jks
```

Put copies in:
1. A folder in Google Drive
2. Email the file as attachment to yourself
3. A USB stick or phone

**If you lose this file, you can NEVER update your app on Play Store** — you'd have to re-publish under a new package name and lose all your users. This is the #1 mistake Indian devs make. Don't be that dev.

Also save the password `StudyGalaxy2026!` somewhere permanent (phone notes, email draft, password manager).

## Step 1 — Deploy backend first (free, ~10 min)

The app in release mode talks to `https://studygalaxy-backend.onrender.com`. You need to deploy the backend there (or anywhere HTTPS). Follow `DEPLOY.md`:

1. Push repo to GitHub (free)
2. Go to render.com → New → Blueprint → connect repo
3. Render reads `backend/render.yaml`, provisions a free server
4. Note your URL — it may differ from `studygalaxy-backend.onrender.com`
5. If different, update `android-native/app/build.gradle.kts` line with `API_BASE`, then re-run `./gradlew bundleRelease`

**Skip this step if you want to launch with just native-only features that don't need the backend** — but the app needs the server for auth, sessions, circles, notes.

## Step 2 — Pay $25 and create Play Console account

1. Go to [play.google.com/console](https://play.google.com/console)
2. Sign in with the Google account you want to be the developer
3. Click **Get started**
4. **Pay $25 one-time** (credit/debit card, UPI works for Indian cards)
5. Fill out developer profile:
   - Account type: **Personal** (unless you have a registered business)
   - Developer name: **StudyGalaxy** (shown on Play Store)
   - Contact email: `honey6398sharma@gmail.com`
6. Google will ask to verify your identity:
   - Upload government ID (Aadhaar / Passport / Driver's License)
   - Takes 1–3 business days to approve
   - You can proceed with next steps while waiting

## Step 3 — Create the app in Play Console

Once your account is approved:

1. **Create app** button → fill:
   - App name: **StudyGalaxy**
   - Default language: **English (United States)**
   - App or game: **App**
   - Free or paid: **Free**
   - Agree to Developer Program Policies
2. Click **Create app**

## Step 4 — Fill the mandatory setup items

Play Console shows a checklist. For each item:

### a) App access
"Not restricted — all functionality is available without special access"

### b) Ads
"No, my app does not contain ads"

### c) Content rating
Click **Start questionnaire**. Honest answers:
- Category: **Reference, News, or Educational**
- Violence: No
- Sex: No
- Profanity: No
- Controlled substances: No
- User-generated content: Yes (user cosmic names)
- Social features / user interaction: Yes (circles)
- Shares personal info: No

Result: **Everyone / PEGI 3** rating.

### d) Target audience
- Target age: **13 and over** (so you're not in the Families program)

### e) News app
No.

### f) COVID-19 tracing
No.

### g) Data safety (very important — fill carefully)

Click **Start** → declare:

- **Personal info → Phone number**: Yes, collected, processed ephemerally, linked to user, required for app functionality (login identifier)
- **Personal info → User IDs**: Yes, collected, linked to user, required
- **App info → App interactions**: Yes, collected, linked, required (focus sessions, streaks)
- **App info → Other user-generated content**: Yes (notes), linked, optional
- Everything else: **No**
- Data encrypted in transit: **Yes** (HTTPS)
- Data can be deleted by user: **Yes** (via account deletion in app)

### h) Government apps / financial / health
No.

### i) Store listing

Paste from `STORE_LISTING.md`:
- **App name**: `StudyGalaxy: Focus & Study`
- **Short description** (80 chars): see file
- **Full description**: see file (4000 chars)
- **App category**: Education
- **Tags**: productivity, focus, study, pomodoro, students

Upload graphics:
- **App icon**: `frontend/public/icons/play-store-icon-512.png`
- **Feature graphic**: `frontend/public/icons/feature-graphic.png`
- **Phone screenshots** (minimum 2, I recommend 4):
  - Take with your phone after installing the APK
  - Suggested screens: Onboarding question / Home with portal / Timer with todos / Notes canvas / Galaxy

### j) Privacy policy

Required. Options:
- Deploy frontend to Netlify (DEPLOY.md), then your URL + `/#privacy` is the privacy page (reachable via the in-app Privacy screen)
- OR paste the privacy text (in `frontend/src/features/legal/PrivacyScreen.tsx`) into a free GitHub Pages site
- OR use a free generator at https://app-privacy-policy-generator.firebaseapp.com/

Paste the final URL into Play Console.

## Step 5 — Upload the AAB

1. Left sidebar → **Testing** → **Internal testing** (recommended first)
2. **Create new release**
3. Upload `app-release.aab` from `android-native/app/build/outputs/bundle/release/`
4. Google may complain the first time — this is normal. Common fixes:
   - If it says "Debuggable release" → make sure you built with `./gradlew bundleRelease` not `assembleDebug`
   - If it says "unsigned" → check the keystore config in `app/build.gradle.kts`
5. Release name: `0.1.0 — First launch`
6. Release notes: `First release. Focus with friends, grow your galaxy. Try it on day 1 — tell us what to add next.`
7. **Save → Review release → Start rollout to Internal testing**

Add yourself as a tester:
1. Internal testing → Testers → Create email list → add your Gmail
2. Click the Google Play opt-in link they generate — opens Play Store on your phone
3. Install from Play Store (verifies the signed build works on real device)

## Step 6 — Promote to Production

Once internal testing works for 1–2 days (verify login, timer, notes all work on real device via Play Store install):

1. Internal testing → **Promote release → Production**
2. Review summary → **Start rollout to Production**
3. Google reviews: **1–3 days** usually, up to 7 days first time
4. You'll get email when approved
5. Your app is now live at `https://play.google.com/store/apps/details?id=app.studygalaxy`

## Known risks you should know

- **First submission gets extra scrutiny**: Google often asks for 20-day closed testing with 12+ testers before letting you go to production. If asked, run a closed test with friends.
- **"Deceptive behavior" flag**: happens if your Data Safety declaration doesn't match what the app actually does. Our form matches the code — you're fine.
- **Rejected icon**: your icon must be 512×512 PNG with no transparent corners cut by Play Store. The generated one is fine.

## Shortcuts if a step blocks you

- Backend hosting too complicated? Use free Fly.io or self-host at home via Cloudflare Tunnel (free HTTPS)
- Privacy policy page too much? Use the generator link above (5 min)
- Screenshots? Build the APK, install on your phone, press Volume-Down + Power to capture, email yourself

## My role going forward

I can't:
- Pay the $25
- See your Play Console
- Upload the AAB (your account only)
- Approve app on Google's end

I can:
- Fix any build error that shows up
- Change the app code based on feedback
- Rebuild the AAB whenever you want
- Help you respond to Play Store review feedback
- Add new features

**Which step do you want help with next?**
