import { useStore } from '../../store';

export function PrivacyScreen() {
  const setScreen = useStore(s => s.setScreen);
  return (
    <div className="min-h-screen px-5 pt-4 pb-16 max-w-xl mx-auto">
      <div className="flex items-center justify-between">
        <button onClick={() => setScreen('home')} className="text-sm text-white/70">← Back</button>
        <div className="font-display text-xl">Privacy</div>
        <div className="w-8" />
      </div>

      <article className="mt-8 space-y-4 text-sm text-white/80 leading-relaxed">
        <p>StudyGalaxy is built so students can focus and study together. We keep what we collect to the minimum.</p>

        <h2 className="font-semibold text-white mt-4">What we store</h2>
        <ul className="list-disc pl-5 space-y-1">
          <li>A random device identifier generated on your phone so your galaxy follows you between sessions.</li>
          <li>Your cosmic name and the sessions, subjects, and circles you create.</li>
          <li>Session timestamps and durations (needed to grow your galaxy and streaks).</li>
        </ul>

        <h2 className="font-semibold text-white mt-4">What we don't collect</h2>
        <ul className="list-disc pl-5 space-y-1">
          <li>No email, phone number, address, or real name.</li>
          <li>No contacts, no location, no camera or microphone access (unless you scan a circle QR code; images are processed on-device only).</li>
          <li>No ad trackers, no third-party analytics in the MVP.</li>
        </ul>

        <h2 className="font-semibold text-white mt-4">Who sees your data</h2>
        <p>Only you, unless you join a Study Circle. In a circle, other members see your cosmic name, completed sessions, and when you are focusing right now.</p>

        <h2 className="font-semibold text-white mt-4">Deleting your data</h2>
        <p>You can delete your account and all its data at any time by opening Settings → Delete account. Deletion is permanent and immediate.</p>

        <h2 className="font-semibold text-white mt-4">Contact</h2>
        <p>Questions: write to the project maintainer at the contact listed on the Play Store page.</p>

        <p className="text-xs text-white/40 mt-6">Last updated: 2026-10-11</p>
      </article>
    </div>
  );
}
