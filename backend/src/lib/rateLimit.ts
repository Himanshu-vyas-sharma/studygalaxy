import type { Request, Response, NextFunction } from 'express';

interface Bucket { count: number; resetAt: number }

/**
 * In-memory fixed-window limiter. Enough for a single-node deploy at ~50k users;
 * swap for Redis only if we ever run more than one backend instance.
 */
export function rateLimit(opts: { windowMs: number; max: number; key?: (req: Request) => string }) {
  const { windowMs, max } = opts;
  const keyOf = opts.key ?? ((req: Request) => req.ip ?? 'unknown');
  const buckets = new Map<string, Bucket>();

  // Drop stale buckets so the map can't grow forever.
  const sweep = setInterval(() => {
    const now = Date.now();
    for (const [k, b] of buckets) if (b.resetAt <= now) buckets.delete(k);
  }, windowMs);
  sweep.unref?.();

  return (req: Request, res: Response, next: NextFunction) => {
    const now = Date.now();
    const key = keyOf(req);
    let b = buckets.get(key);
    if (!b || b.resetAt <= now) {
      b = { count: 0, resetAt: now + windowMs };
      buckets.set(key, b);
    }
    b.count++;
    const remaining = Math.max(0, max - b.count);
    res.setHeader('X-RateLimit-Limit', String(max));
    res.setHeader('X-RateLimit-Remaining', String(remaining));
    if (b.count > max) {
      res.setHeader('Retry-After', String(Math.ceil((b.resetAt - now) / 1000)));
      return res.status(429).json({ error: 'too_many_requests' });
    }
    next();
  };
}
