import logger from '../config/logger.js';

export const requestLogger = (req, res, next) => {
  const start = Date.now();
  
  res.on('finish', () => {
    const duration = Date.now() - start;
    const { method, originalUrl, ip } = req;
    const { statusCode } = res;
    
    logger.info(`${method} ${originalUrl} ${statusCode} - ${duration}ms - IP: ${ip}`);
  });
  
  next();
};
