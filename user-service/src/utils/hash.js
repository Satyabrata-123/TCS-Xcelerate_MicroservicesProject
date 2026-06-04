import crypto from 'crypto';
import { promisify } from 'util';

const scrypt = promisify(crypto.scrypt);

/**
 * Hash password using scrypt
 * @param {string} password - Raw password
 * @returns {Promise<string>} Salt and hash combined string
 */
export async function hashPassword(password) {
  const salt = crypto.randomBytes(16).toString('hex');
  const hash = await scrypt(password, salt, 64);
  return `${salt}.${hash.toString('hex')}`;
}

/**
 * Verify candidate password against stored hash
 * @param {string} candidatePassword - Password input
 * @param {string} storedPassword - Combined salt.hash from database
 * @returns {Promise<boolean>} Match result
 */
export async function comparePassword(candidatePassword, storedPassword) {
  if (!storedPassword || !storedPassword.includes('.')) {
    return false;
  }
  const [salt, key] = storedPassword.split('.');
  const hash = await scrypt(candidatePassword, salt, 64);
  
  return crypto.timingSafeEqual(
    Buffer.from(key, 'hex'),
    hash
  );
}
