# Deploy StudyGalaxy for Free (students, no $25 fee needed)

End result: your app lives at `https://studygalaxy.netlify.app` (or similar). Any friend opens that URL in Chrome, taps "Add to Home Screen," and the app lives on their phone like a native one. **No Play Store needed.**

Three accounts, all free, all no credit card:

1. **GitHub** — hosts your code
2. **Render** — hosts your backend (free HTTPS, free 1 GB disk)
3. **Netlify** — hosts your frontend (free HTTPS, free static hosting)

Total time: **~45 minutes** the first time. Future updates: `git push` and both auto-redeploy.

## Step 1 — GitHub (5 min)

1. Go to [github.com](https://github.com) → **Sign up** (free). Verify email.
2. Click the **+** top-right → **New repository**.
3. Name it `studygalaxy`. Set **Private** if you want (free either way).
4. Do NOT tick "Add README" (we have one).
5. Create repository. GitHub will show you a page with commands — ignore those, we have ours below.

Then in PowerShell:

```powershell
cd "C:\Users\Mayank Vyas\Documents\studygalaxy"
git init
git add .
git commit -m "Initial commit: StudyGalaxy MVP"
git branch -M main
git remote add origin https://github.com/<your-username>/studygalaxy.git
git push -u origin main
```

Replace `<your-username>` with your GitHub username. First push asks you to log in via browser — Chrome opens, you approve, done.

## Step 2 — Render (backend, free, 10 min)

1. Go to [render.com](https://render.com) → **Get Started** → sign in with GitHub.
2. **New +** → **Blueprint**.
3. Connect your `studygalaxy` repo.
4. Render reads `backend/render.yaml` automatically. It will show a plan preview: 1 web service + 1 persistent disk.
5. Click **Apply**.
6. Wait ~3 min for first build. The URL will look like `https://studygalaxy-backend.onrender.com`.
7. **Important**: once backend is live, back in Render dashboard → the service → **Environment** → add:
   - `CORS_ORIGIN` = (your Netlify URL from step 3; come back and update after step 3)

**About the free tier:** Render spins down the service after 15 min of inactivity, and the next request wakes it up (30 sec cold start). This is normal and FINE for a student launch. If it becomes a problem later, Fly.io free tier has no spin-down.

Test it works:
```powershell
# Replace with your actual Render URL
curl https://studygalaxy-backend.onrender.com/health
# Should return: {"ok":true,"service":"studygalaxy"}
```

## Step 3 — Netlify (frontend, free, 10 min)

1. Go to [netlify.com](https://netlify.com) → **Sign up** with GitHub.
2. **Add new site** → **Import an existing project** → GitHub → select `studygalaxy`.
3. Netlify reads `frontend/netlify.toml` and auto-fills everything.
4. **Site configuration** → **Environment variables** → add:
   - `VITE_API_BASE` = your Render backend URL from step 2 (no trailing slash)
5. **Deploy site**.
6. First build takes ~2 min. You get a URL like `https://glittering-starlight-abc123.netlify.app`.
7. Rename it: Site settings → Change site name → `studygalaxy` (or whatever's available) → gives you `https://studygalaxy.netlify.app`.

Now go back to Render and set `CORS_ORIGIN` to this Netlify URL. Render will auto-redeploy with the new CORS setting (~1 min).

## Step 4 — Test on your phone (5 min)

1. Open your Netlify URL in Chrome on your Android phone.
2. Wait 2-3 seconds for the galaxy background to load.
3. Pick a cosmic name → the app loads.
4. Chrome menu (⋮) → **Add to Home Screen** → confirm.
5. Your app's icon appears on the phone's home screen.
6. Tap it. It opens fullscreen, no browser bar, feels native.

Share the URL with friends. Each of them does the same 2-step install. **You just shipped an app for $0.**

## Future updates

Make any code change locally. Then:

```powershell
cd "C:\Users\Mayank Vyas\Documents\studygalaxy"
git add .
git commit -m "Describe what changed"
git push
```

Netlify + Render both auto-detect the push and redeploy within ~2 minutes. Your users refresh and get the new version (the service worker handles this cleanly).

## Marketing hack: shareable install link

Netlify gives you a QR code for your site. Print it. Stick it on your college notice board. Students scan → their phone opens the app → they tap "Add to Home Screen." Done. No Play Store, no $25, real users.

Alternative: WhatsApp forward with the URL. Instagram story with the QR. College WhatsApp group with "Join our study circle: `GLX-ABC123`" + URL.

## When to pay the $25

Only pay for Play Store if ONE of these is true:
- You have 100+ regular PWA users and need the Play Store search credibility.
- Students tell you they'd use it IF it was on Play Store.
- You want in-app purchases (not needed for StudyGalaxy MVP).

Validate first. Pay later. **$25 is a reward for traction, not a prerequisite for launch.**

## Troubleshooting

- **"Add to Home Screen" doesn't appear**: You need HTTPS (Netlify is HTTPS by default — good). You also need to visit the site at least once and interact (click something). Chrome shows the prompt only after basic engagement.

- **CORS error in browser console**: Backend's `CORS_ORIGIN` doesn't match your frontend URL. Fix in Render env panel.

- **Backend 503 / slow first load**: Render free tier cold start. First request after idle takes ~30 sec. Normal. Subsequent requests are instant.

- **Service worker not updating after deploy**: Hard refresh (Ctrl+Shift+R) or close and reopen the PWA. The SW auto-updates in the background on next launch.
