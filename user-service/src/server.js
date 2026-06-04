import dotenv from 'dotenv';
import app from './app.js';
import sequelize from './config/database.js';
import logger from './config/logger.js';

dotenv.config();

const PORT = parseInt(process.env.PORT || '5000', 10);

async function startServer() {
  try {
    logger.info('Connecting to PostgreSQL database...');
    await sequelize.authenticate();
    logger.info('Database connection established successfully.');

    logger.info('Synchronizing database models...');
    // In production, we'd use migrations, but for simple startup:
    await sequelize.sync();
    logger.info('Database models synchronized.');

    app.listen(PORT, () => {
      logger.info(`Server is running in ${process.env.NODE_ENV || 'development'} mode on port ${PORT}`);
      logger.info(`API documentation available at http://localhost:${PORT}/api-docs`);
    });
  } catch (error) {
    logger.error('Failed to start the application server:', error);
    process.exit(1);
  }
}

// Handle unhandled rejections and exceptions outside express
process.on('unhandledRejection', (reason) => {
  logger.error('Unhandled Promise Rejection:', reason);
  // Optional: Graceful shutdown could go here
});

process.on('uncaughtException', (error) => {
  logger.error('Uncaught Exception thrown:', error);
  process.exit(1);
});

startServer();
