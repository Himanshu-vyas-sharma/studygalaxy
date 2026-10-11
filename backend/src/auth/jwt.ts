import jwt from 'jsonwebtoken';
import { config } from '../config.js';

export interface JwtPayload {
  uid: string;
  did: string;
}

export const signJwt = (payload: JwtPayload) =>
  jwt.sign(payload, config.jwtSecret, { expiresIn: '365d' });

export const verifyJwt = (token: string): JwtPayload =>
  jwt.verify(token, config.jwtSecret) as JwtPayload;
