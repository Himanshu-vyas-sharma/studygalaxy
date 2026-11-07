# StudyGalaxy

> Forest says "don't touch your phone." StudyGalaxy says: your friends are studying right now, your Physics planet is dying, and if you quit this session everyone will see the black hole forever.

A focus app for students. Zero-cost, self-owned, no vendor lock-in.

## Stack

- **Frontend:** React 19 + Vite + TypeScript + Tailwind + Framer Motion (PWA, installs to Android home screen)
- **Backend:** Node 24 + Express + TypeScript + libSQL/Turso + WebSockets + JWT
- **Auth:** phone number + password (JWT sessions). No email, no SMS, no third-party sign-in.

## Run locally

Two terminals.

### Terminal 1 — backend

```powershell
cd backend
npm install
npm run dev
# Server on http://localhost:3000
```

### Terminal 2 — frontend

```powershell
cd frontend
npm install
npm run dev
# App on http://localhost:5173
```

Open `http://localhost:5173` in Chrome. First launch picks a cosmic name, drops you on the galaxy home screen.

## Deploy (later, all free)

- Backend → Render / Fly.io / Railway free tier
- Frontend → Cloudflare Pages / Netlify / GitHub Pages (static build)
- SQLite file lives on the backend host's disk

## Folder structure

```
studygalaxy/
├── backend/          Node + Express + SQLite
└── frontend/         React + Vite PWA
```
