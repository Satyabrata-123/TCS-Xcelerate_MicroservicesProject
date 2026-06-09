import crypto from 'crypto';
import sequelize from '../config/database.js';
import IdempotencyKey from '../models/idempotencyKey.js';
import { ConflictError, BadRequestError } from '../utils/errors.js';
import logger from '../config/logger.js';

// Calculate SHA-256 hash of request body to ensure parameters match on retry
const calculateHash = (body) => {
  const serialized = JSON.stringify(body || {});
  return crypto.createHash('sha256').update(serialized).digest('hex');
};

export const idempotencyMiddleware = async (req, res, next) => {
  // Idempotency is only applicable to mutating POST/PUT requests
  if (req.method !== 'POST' && req.method !== 'PUT') {
    return next();
  }

  const idempotencyKey = 
    req.headers['idempotency-key'] || 
    req.headers['x-idempotency-key'] || 
    req.body.idempotencyKey;

  // If no key is provided, let it pass (or enforce it based on configuration)
  if (!idempotencyKey) {
    return next();
  }

  const requestHash = calculateHash(req.body);
  const requestPath = req.originalUrl;

  const transaction = await sequelize.transaction();
  try {
    // Acquire a row lock on any existing key
    const record = await IdempotencyKey.findOne({
      where: { idempotencyKey },
      transaction,
      lock: transaction.LOCK.UPDATE,
    });

    if (record) {
      // Release row lock immediately
      await transaction.rollback();

      // Check for parameter mismatch (hijacking or mistake)
      if (record.requestHash !== requestHash) {
        throw new ConflictError(
          `Idempotency key conflict: key '${idempotencyKey}' was previously used with different request parameters.`
        );
      }

      // Check if previous request is still in progress
      if (record.status === 'STARTED') {
        const timeElapsed = Date.now() - new Date(record.createdAt).getTime();
        const timeoutMs = 120000; // 2 minutes timeout for stale locks

        if (timeElapsed < timeoutMs) {
          throw new ConflictError(
            `A request with idempotency key '${idempotencyKey}' is already in progress. Please try again shortly.`
          );
        } else {
          // Stale request lock: Reset status and let this request re-run
          logger.warn(`Stale idempotency key '${idempotencyKey}' detected. Resetting lock to process retry.`);
          await IdempotencyKey.update({ status: 'STARTED', createdAt: new Date() }, { where: { id: record.id } });
          req.idempotencyRecord = record;
          setupResponseInterceptor(req, res);
          return next();
        }
      }

      // If resolved, return the cached response
      logger.info(`Idempotency key resolved. Returning cached response for key: ${idempotencyKey}`);
      res.set('Idempotency-Key-Response', 'true');
      res.status(record.responseStatusCode).json(record.responseBody);
      return;
    }

    // Insert new key record in STARTED state
    const newRecord = await IdempotencyKey.create({
      idempotencyKey,
      requestPath,
      requestHash,
      status: 'STARTED',
    }, { transaction });

    await transaction.commit();

    req.idempotencyRecord = newRecord;
    setupResponseInterceptor(req, res);
    next();

  } catch (error) {
    if (transaction.finished !== 'rollback' && transaction.finished !== 'commit') {
      await transaction.rollback();
    }
    next(error);
  }
};

const setupResponseInterceptor = (req, res) => {
  const originalSend = res.send;

  res.send = function (chunk) {
    // Restore original send method
    res.send = originalSend;

    const recordId = req.idempotencyRecord?.id;
    if (recordId) {
      const statusCode = res.statusCode;

      if (statusCode >= 500) {
        // Do not cache server errors (5xx) to allow retries. Delete key.
        logger.warn(`Server error ${statusCode} occurred. Deleting idempotency key so retry can process.`);
        IdempotencyKey.destroy({ where: { id: recordId } }).catch((err) => {
          logger.error('Failed to clean up idempotency key on server error:', err);
        });
      } else {
        // Cache success or validation client error responses (2xx, 4xx)
        let responseBody = chunk;
        if (typeof chunk === 'string') {
          try {
            responseBody = JSON.parse(chunk);
          } catch (e) {
            // Not a JSON string, keep it as text
          }
        }

        IdempotencyKey.update({
          status: 'RESOLVED',
          responseStatusCode: statusCode,
          responseBody: responseBody,
        }, {
          where: { id: recordId },
        }).catch((err) => {
          logger.error('Failed to resolve idempotency key response cache:', err);
        });
      }
    }

    return originalSend.apply(res, arguments);
  };
};

export default idempotencyMiddleware;
