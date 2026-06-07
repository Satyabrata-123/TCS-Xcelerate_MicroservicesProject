import paymentRepository from '../repositories/paymentRepository.js';
import sequelize from '../config/database.js';
import { ConflictError, NotFoundError, BadRequestError } from '../utils/errors.js';
import logger from '../config/logger.js';

class PaymentService {
  /**
   * Processes a payment with idempotency checks
   */
  async processPayment(paymentData) {
    const { orderId, amount, currency, paymentMethod, idempotencyKey } = paymentData;

    // 1. Initial check for existing payment with the same idempotency key
    const existingPayment = await paymentRepository.findByIdempotencyKey(idempotencyKey);
    if (existingPayment) {
      logger.info(`Idempotency key match found for key: ${idempotencyKey}`);
      this._validateIdempotencyMatch(existingPayment, paymentData);
      return existingPayment;
    }

    // 2. Try to create and process the payment within a transaction
    const transaction = await sequelize.transaction();
    try {
      // Re-verify in transaction to prevent double processing in case of rapid concurrent requests
      const doubleCheck = await PaymentService._findWithLock(idempotencyKey, transaction);
      if (doubleCheck) {
        await transaction.rollback();
        this._validateIdempotencyMatch(doubleCheck, paymentData);
        return doubleCheck;
      }

      // Create initial payment in PENDING status
      const payment = await paymentRepository.create({
        orderId,
        amount,
        currency,
        paymentMethod,
        idempotencyKey,
        status: 'PENDING'
      }, transaction);

      // Simulate payment processing (external gateway integration)
      const isSuccessful = this._simulatePaymentGateway(amount);
      
      const updateData = {};
      if (isSuccessful) {
        updateData.status = 'SUCCESS';
        logger.info(`Payment ${payment.id} processed successfully.`);
      } else {
        updateData.status = 'FAILED';
        updateData.errorMessage = 'Simulated payment processing failure';
        logger.warn(`Payment ${payment.id} processing failed.`);
      }

      const updatedPayment = await paymentRepository.update(payment, updateData, transaction);
      await transaction.commit();
      return updatedPayment;

    } catch (error) {
      await transaction.rollback();

      // Check if error is due to concurrent unique constraint violation on idempotencyKey
      if (error.name === 'SequelizeUniqueConstraintError') {
        logger.warn(`Concurrent insert collision resolved for idempotency key: ${idempotencyKey}`);
        const resolvedPayment = await paymentRepository.findByIdempotencyKey(idempotencyKey);
        if (resolvedPayment) {
          this._validateIdempotencyMatch(resolvedPayment, paymentData);
          return resolvedPayment;
        }
      }

      logger.error('Error occurred during payment processing:', error);
      throw error;
    }
  }

  /**
   * Retrieves payment details by ID
   */
  async getPaymentById(id) {
    const payment = await paymentRepository.findById(id);
    if (!payment) {
      throw new NotFoundError(`Payment with ID ${id} not found`);
    }
    return payment;
  }

  /**
   * Refunds a successful payment
   */
  async refundPayment(id, reason) {
    const payment = await paymentRepository.findById(id);
    if (!payment) {
      throw new NotFoundError(`Payment with ID ${id} not found`);
    }

    if (payment.status === 'REFUNDED') {
      throw new BadRequestError('Payment has already been refunded');
    }

    if (payment.status !== 'SUCCESS') {
      throw new BadRequestError(`Only successful payments can be refunded. Current status: ${payment.status}`);
    }

    const updatedPayment = await paymentRepository.update(payment, {
      status: 'REFUNDED',
      refundReason: reason || 'Requested by customer'
    });

    logger.info(`Payment ${id} refunded successfully. Reason: ${reason || 'N/A'}`);
    return updatedPayment;
  }

  /**
   * Helper: validates if the incoming request matches details of the previously stored payment
   */
  _validateIdempotencyMatch(existing, incoming) {
    const amountMatch = Number(existing.amount) === Number(incoming.amount);
    const orderIdMatch = existing.orderId === incoming.orderId;
    const currencyMatch = existing.currency.toUpperCase() === incoming.currency.toUpperCase();

    if (!amountMatch || !orderIdMatch || !currencyMatch) {
      throw new ConflictError(
        `Idempotency key conflict: key '${incoming.idempotencyKey}' was previously used with different request parameters.`
      );
    }
  }

  /**
   * Helper: query database using lock
   */
  static async _findWithLock(idempotencyKey, transaction) {
    // We import the Model directly or use repository
    return paymentRepository.findByIdempotencyKey(idempotencyKey, {
      transaction,
      lock: transaction.LOCK.UPDATE
    });
  }

  /**
   * Helper: gateway simulator
   * Amount of 9999.00 or 99.99 will simulate a payment failure.
   */
  _simulatePaymentGateway(amount) {
    const numericAmount = Number(amount);
    if (numericAmount === 9999.99 || numericAmount === 99.99) {
      return false;
    }
    return true;
  }
}

export default new PaymentService();
export { PaymentService };
