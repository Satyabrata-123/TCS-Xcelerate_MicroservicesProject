import { Router } from 'express';
import paymentController from '../controllers/paymentController.js';
import { 
  validateProcessPayment, 
  validateGetPayment, 
  validateRefundPayment 
} from '../middlewares/validate.js';

const router = Router();

router.post('/', validateProcessPayment, paymentController.processPayment);
router.get('/:id', validateGetPayment, paymentController.getPayment);
router.post('/:id/refund', validateRefundPayment, paymentController.refundPayment);

export default router;
export { router as paymentRoutes };
