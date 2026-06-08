import { Kafka } from 'kafkajs';
import logger from './logger.js';
import dotenv from 'dotenv';

dotenv.config();

const bootstrapServers = process.env.KAFKA_BOOTSTRAP_SERVERS ? process.env.KAFKA_BOOTSTRAP_SERVERS.split(',') : ['localhost:29092'];
const clientId = process.env.KAFKA_CLIENT_ID || 'payment-service';

const kafka = new Kafka({
  clientId,
  brokers: bootstrapServers,
  retry: {
    initialRetryTime: 300,
    retries: 10
  }
});

const producer = kafka.producer();
const consumer = kafka.consumer({ groupId: 'payment-group' });

async function connectKafka() {
  try {
    logger.info('Connecting Kafka Producer...');
    await producer.connect();
    logger.info('Kafka Producer connected successfully.');

    logger.info('Connecting Kafka Consumer...');
    await consumer.connect();
    logger.info('Kafka Consumer connected successfully.');
  } catch (error) {
    logger.error('Failed to connect to Kafka:', error);
    throw error;
  }
}

async function disconnectKafka() {
  try {
    logger.info('Disconnecting Kafka Producer...');
    await producer.disconnect();
    logger.info('Kafka Producer disconnected.');

    logger.info('Disconnecting Kafka Consumer...');
    await consumer.disconnect();
    logger.info('Kafka Consumer disconnected.');
  } catch (error) {
    logger.error('Error during Kafka disconnection:', error);
  }
}

export { kafka, producer, consumer, connectKafka, disconnectKafka };
