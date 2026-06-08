import dotenv from 'dotenv';
import app from './app.js';
import sequelize from './config/database.js';
import logger from './config/logger.js';
import { startEureka, stopEureka } from './config/eureka.js';
import { connectKafka, disconnectKafka } from './config/kafka.js';
import { startOrderConsumer } from './consumers/orderConsumer.js';

dotenv.config();

const PORT = parseInt(process.env.PORT || '5002', 10);
let server;

async function startServer() {
  try {
    logger.info('Connecting to PostgreSQL database...');
    await sequelize.authenticate();
    logger.info('Database connection established successfully.');

    // Connect to Kafka
    logger.info('Connecting to Kafka broker...');
    await connectKafka();

    logger.info('Synchronizing database models...');
    // Synchronize models (in production, migrations are preferred, but sync is ideal for bootstrap)
    await sequelize.sync();
    logger.info('Database models synchronized.');

    server = app.listen(PORT, () => {
      logger.info(`Server is running in ${process.env.NODE_ENV || 'development'} mode on port ${PORT}`);
      logger.info(`API documentation available at http://localhost:${PORT}/api-docs`);
      
      // Start Eureka Client Registration
      startEureka();

      // Start Kafka Consumer
      startOrderConsumer();
    });
  } catch (error) {
    logger.error('Failed to start the application server:', error);
    process.exit(1);
  }
}

// Graceful Shutdown Handler
async function gracefulShutdown(signal) {
  logger.info(`Received ${signal}. Starting graceful shutdown...`);
  
  // Stop Eureka first so it deregisters from Eureka registry immediately
  await stopEureka();

  // Disconnect Kafka
  await disconnectKafka();
  
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
