import dotenv from 'dotenv';
import app from './app.js';
import sequelize from './config/database.js';
import logger from './config/logger.js';
import { startEureka, stopEureka } from './config/eureka.js';

dotenv.config();

const PORT = parseInt(process.env.PORT || '5000', 10);
let server;

async function startServer() {
  try {
    logger.info('Connecting to PostgreSQL database...');
    await sequelize.authenticate();
    logger.info('Database connection established successfully.');

    logger.info('Synchronizing database models...');
    // In production, we'd use migrations, but for simple startup:
    await sequelize.sync();
    logger.info('Database models synchronized.');

    server = app.listen(PORT, () => {
      logger.info(`Server is running in ${process.env.NODE_ENV || 'development'} mode on port ${PORT}`);
      logger.info(`API documentation available at http://localhost:${PORT}/api-docs`);
      
      // Start Eureka Client Registration
      startEureka();
    });
  } catch (error) {
    logger.error('Failed to start the application server:', error);
    process.exit(1);
  }
}

// Graceful Shutdown Handler
async function gracefulShutdown(signal) {
  logger.info(`Received ${signal}. Starting graceful shutdown...`);
  
  // Stop Eureka first so it deregisters immediately
  await stopEureka();
  
  if (server) {
    server.close(() => {
      logger.info('HTTP server closed.');
      process.exit(0);
    });
  } else {
    process.exit(0);
  }
}

// Handle shutdown signals
process.on('SIGINT', () => gracefulShutdown('SIGINT'));
process.on('SIGTERM', () => gracefulShutdown('SIGTERM'));

// Handle unhandled rejections and exceptions outside express
process.on('unhandledRejection', (reason) => {
  logger.error('Unhandled Promise Rejection:', reason);
});

process.on('uncaughtException', (error) => {
  logger.error('Uncaught Exception thrown:', error);
  gracefulShutdown('UNCAUGHT_EXCEPTION');
});

startServer();
