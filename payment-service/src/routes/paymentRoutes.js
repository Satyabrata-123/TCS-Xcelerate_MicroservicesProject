import { Router } from 'express';
import paymentController from '../controllers/paymentController.js';
import { 
  validateProcessPayment, 
  validateGetPayment, 
  validateRefundPayment 
} from '../middlewares/validate.js';
import { idempotencyMiddleware } from '../middlewares/idempotency.js';

const router = Router();

router.post('/', idempotencyMiddleware, validateProcessPayment, paymentController.processPayment);
router.get('/:id', validateGetPayment, paymentController.getPayment);
router.post('/:id/refund', idempotencyMiddleware, validateRefundPayment, paymentController.refundPayment);

export default router;
export { router as paymentRoutes };
