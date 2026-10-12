# Deploy StudyGalaxy — free, worldwide, persistent

End state: students anywhere open your URL (or the Android app) and it just works. Your PC can be off. **$0/month.**

```
Turso      →  the database (persistent, 5GB free)
Render     →  the backend  (free web service)
Netlify    →  the frontend (free static hosting + HTTPS)
cron-job   →  pings /health every 10 min so Render never sleeps
```

## Why this stack and not just Render

Render's free web services **cannot attach a persistent disk** — the filesystem is wiped on every restart. A SQLite file there would lose every account. So the database lives in Turso (hosted libSQL — same SQLite engine, same SQL) and Render only runs the stateless Node process.

Free-tier headroom for **50,000 students**: Turso allows 500M row reads and 10M row writes per month. StudyGalaxy's usage is roughly 15M reads and 2.5M writes — about 3% and 25% of the limits.

---

## Step 1 — Turso (database, ~5 min)

1. Sign up free at [turso.tech](https://turso.tech) (GitHub login).
2. Install the CLI:
   - Windows (PowerShell): `irm get.tur.so/install.ps1 | iex`
   - or use the web dashboard instead of the CLI — both work.
3. Create the database and grab credentials:

```powershell
turso auth login
turso db create studygalaxy
turso db show studygalaxy --url          # -> libsql://studygalaxy-<org>.turso.io
turso db tokens create studygalaxy       # -> eyJ... (long token)
```

Save both values. You'll paste them into Render in step 2.

**Tables create themselves.** `initSchema()` runs on boot, so the first deploy builds every table and index automatically.

### Optional — copy your local test data up

```powershell
cd "C:\Users\Mayank Vyas\Documents\studygalaxy\backend"
turso db shell studygalaxy < (sqlite3 data/studygalaxy.sqlite .dump)
```
Skip this if you're happy starting fresh in production.

---

## Step 2 — GitHub + Render (backend, ~15 min)

### Push to GitHub

```powershell
cd "C:\Users\Mayank Vyas\Documents\studygalaxy"
git branch -M main
git remote add origin https://github.com/<your-username>/studygalaxy.git
git push -u origin main
```

### Deploy on Render

1. [render.com](https://render.com) → sign in with GitHub → **New +** → **Blueprint**
2. Pick the `studygalaxy` repo. Render reads `backend/render.yaml`.
3. It will ask for the values marked `sync: false`. Set:

| Variable | Value |
|---|---|
| `DATABASE_URL` | `libsql://studygalaxy-<org>.turso.io` (from step 1) |
| `DATABASE_AUTH_TOKEN` | the `eyJ...` token (from step 1) |
| `CORS_ORIGIN` | leave blank for now; fill after step 3 |

`JWT_SECRET` is generated automatically — don't set it by hand.

4. **Apply**. First build takes ~3 min.
5. Verify: open `https://<your-service>.onrender.com/health` → `{"ok":true,"service":"studygalaxy"}`

---

## Step 3 — Netlify (frontend, ~10 min)

1. [netlify.com](https://netlify.com) → sign in with GitHub → **Add new site** → **Import an existing project** → pick `studygalaxy`
2. Netlify reads `frontend/netlify.toml`, so build settings are already correct.
3. **Site configuration → Environment variables** → add:

| Variable | Value |
|---|---|
| `VITE_API_BASE` | your Render URL, e.g. `https://studygalaxy-backend.onrender.com` (no trailing slash) |

4. **Deploy site**, then rename it: Site settings → Change site name → `studygalaxy`
5. Go back to Render → Environment → set `CORS_ORIGIN` to your Netlify URL (`https://studygalaxy.netlify.app`). Render redeploys automatically.

---

## Step 4 — Keep Render awake (~2 min)

Free Render services sleep after 15 minutes idle and take ~1 minute to wake — a terrible first impression. Fix it with a free pinger:

1. Go to [cron-job.org](https://cron-job.org) → sign up free
2. **Create cronjob**:
   - URL: `https://<your-service>.onrender.com/health`
   - Schedule: every **10 minutes**
3. Save and enable.

730 hours/month of uptime fits inside Render's 750 free instance-hours.

---

## Step 5 — Point the Android app at production

In `android-native/app/build.gradle.kts`, the `release` block already reads:

```kotlin
buildConfigField("String", "API_BASE", "\"https://studygalaxy-backend.onrender.com\"")
```

Change that string to **your** Render URL, then rebuild:

```powershell
cd "C:\Users\Mayank Vyas\Documents\studygalaxy\android-native"
./gradlew bundleRelease     # -> app/build/outputs/bundle/release/app-release.aab
```

That `.aab` is what you upload to Play Console. See `PLAY_STORE_FINAL.md`.

---

## Future updates

```powershell
cd "C:\Users\Mayank Vyas\Documents\studygalaxy"
git add .
git commit -m "describe the change"
git push
```

Render and Netlify both auto-deploy on push, ~2 min each.

---

## Local development still works unchanged

```powershell
# Terminal 1
cd backend;  npm run dev     # uses file:./data/studygalaxy.sqlite

# Terminal 2
cd frontend; npm run dev     # http://localhost:5173
```

`DATABASE_URL` defaults to the local SQLite file, so no Turso connection is needed to develop offline.

---

## Troubleshooting

| Symptom | Cause | Fix |
|---|---|---|
| `Cannot reach the server` in app | `VITE_API_BASE` wrong or missing on Netlify | Set it, redeploy |
| CORS error in browser console | `CORS_ORIGIN` on Render doesn't match frontend URL | Update it in Render env |
| First request takes ~1 min | Render cold start | Set up the cron pinger (step 4) |
| `SQLITE_AUTH` / connection refused | Bad `DATABASE_AUTH_TOKEN` | `turso db tokens create studygalaxy` again |
| Data vanished after redeploy | Still on a local file in production | Confirm `DATABASE_URL` starts with `libsql://` |

---

## Cost

| Item | Monthly |
|---|---|
| Turso (5GB, 500M reads) | $0 |
| Render web service | $0 |
| Netlify hosting + HTTPS | $0 |
| cron-job.org pinger | $0 |
| **Total** | **$0** |

Only paid item in the whole project: the **one-time $25** Google Play developer fee.
