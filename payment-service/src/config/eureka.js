import Eureka from 'eureka-js-client';
import dotenv from 'dotenv';
import logger from './logger.js';

dotenv.config();

const port = parseInt(process.env.PORT || '5002', 10);
const eurekaHost = process.env.EUREKA_HOST || 'localhost';
const eurekaPort = parseInt(process.env.EUREKA_PORT || '8761', 10);
const appName = process.env.EUREKA_APP_NAME || 'PAYMENT-SERVICE';
const instanceHost = process.env.EUREKA_INSTANCE_HOST || 'localhost';
const instanceIp = process.env.EUREKA_INSTANCE_IP || '127.0.0.1';

const eurekaClient = new Eureka.Eureka({
  instance: {
    app: appName,
    hostName: instanceHost,
    ipAddr: instanceIp,
    port: {
      '$': port,
      '@enabled': 'true',
    },
    vipAddress: appName.toLowerCase(),
    dataCenterInfo: {
      '@class': 'com.netflix.appinfo.InstanceInfo$DefaultDataCenterInfo',
      name: 'MyOwn',
    },
  },
  eureka: {
    host: eurekaHost,
    port: eurekaPort,
    servicePath: '/eureka/apps/',
  },
});

export const startEureka = () => {
  logger.info(`Registering service to Eureka server at http://${eurekaHost}:${eurekaPort}/eureka/`);
  
  eurekaClient.start((error) => {
    if (error) {
      logger.error('Error starting Eureka client registration:', error);
    } else {
      logger.info(`Successfully registered with Eureka registry as ${appName}`);
    }
  });
};

export const stopEureka = () => {
  return new Promise((resolve) => {
    logger.info('Deregistering service from Eureka server...');
    eurekaClient.stop((error) => {
      if (error) {
        logger.error('Error during Eureka client deregistration:', error);
      } else {
        logger.info('Successfully deregistered from Eureka registry.');
      }
      resolve();
    });
  });
};

export default eurekaClient;
export { eurekaClient };
