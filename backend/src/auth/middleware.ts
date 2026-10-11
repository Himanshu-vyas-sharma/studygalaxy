import type { Request, Response, NextFunction } from 'express';
import { verifyJwt } from './jwt.js';

export interface AuthedRequest extends Request {
  userId: string;
  deviceId: string;
}

export function requireAuth(req: Request, res: Response, next: NextFunction) {
  const header = req.header('authorization');
  if (!header?.startsWith('Bearer ')) {
    return res.status(401).json({ error: 'missing_token' });
  }
  try {
    const payload = verifyJwt(header.slice(7));
    (req as AuthedRequest).userId = payload.uid;
    (req as AuthedRequest).deviceId = payload.did;
    next();
  } catch {
    res.status(401).json({ error: 'invalid_token' });
  }
}
