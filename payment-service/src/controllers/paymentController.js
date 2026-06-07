import paymentService from '../services/paymentService.js';
import { BadRequestError } from '../utils/errors.js';

class PaymentController {
  processPayment = async (req, res, next) => {
    try {
      const { orderId, amount, currency, paymentMethod } = req.body;
      
      // Extract Idempotency Key from header (try both lower and upper case forms) or request body
      const idempotencyKey = 
        req.headers['idempotency-key'] || 
        req.headers['x-idempotency-key'] || 
        req.body.idempotencyKey;

      if (!idempotencyKey) {
        throw new BadRequestError("Idempotency key is required in headers ('Idempotency-Key') or in request body.");
      }

      const payment = await paymentService.processPayment({
        orderId,
        amount,
        currency,
        paymentMethod,
        idempotencyKey,
      });

      // If the payment is PENDING, or was created just now, we return 201 Created.
      // If it is a matching previous request that has already been resolved, return 200 OK.
      const isNew = (Date.now() - new Date(payment.createdAt).getTime()) < 1000;
      const responseStatus = isNew ? 201 : 200;

      res.status(responseStatus).json({
        status: 'success',
        message: isNew ? 'Payment processed successfully' : 'Payment retrieved from cache (Idempotent)',
        data: { payment },
      });
    } catch (error) {
      next(error);
    }
  };

  getPayment = async (req, res, next) => {
    try {
      const { id } = req.params;
      const payment = await paymentService.getPaymentById(id);

      res.status(200).json({
        status: 'success',
        data: { payment },
      });
    } catch (error) {
      next(error);
    }
  };

  refundPayment = async (req, res, next) => {
    try {
      const { id } = req.params;
      const { reason } = req.body;

      const payment = await paymentService.refundPayment(id, reason);

      res.status(200).json({
        status: 'success',
        message: 'Payment has been refunded successfully',
        data: { payment },
      });
    } catch (error) {
      next(error);
    }
  };
}

export default new PaymentController();
export { PaymentController };
