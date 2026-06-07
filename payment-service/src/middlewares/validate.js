import { validationResult, body, param } from 'express-validator';

export const validateRequest = (req, res, next) => {
  const errors = validationResult(req);
  if (!errors.isEmpty()) {
    const formattedErrors = errors.array().map(err => ({
      field: err.path || err.param,
      message: err.msg
    }));
    
    return res.status(400).json({
      status: 'fail',
      message: 'Validation failed',
      errors: formattedErrors
    });
  }
  next();
};

export const validateProcessPayment = [
  body('orderId')
    .notEmpty().withMessage('orderId is required')
    .isUUID().withMessage('orderId must be a valid UUID'),
  body('amount')
    .notEmpty().withMessage('amount is required')
    .isFloat({ min: 0.01 }).withMessage('amount must be a decimal greater than 0'),
  body('currency')
    .notEmpty().withMessage('currency is required')
    .isLength({ min: 3, max: 3 }).withMessage('currency must be a 3-character ISO code (e.g. USD)'),
  body('paymentMethod')
    .notEmpty().withMessage('paymentMethod is required')
    .isString().withMessage('paymentMethod must be a string')
    .trim(),
  validateRequest
];

export const validateGetPayment = [
  param('id')
    .isUUID().withMessage('Payment ID must be a valid UUID'),
  validateRequest
];

export const validateRefundPayment = [
  param('id')
    .isUUID().withMessage('Payment ID must be a valid UUID'),
  body('reason')
    .optional()
    .isString().withMessage('reason must be a string')
    .trim()
    .isLength({ min: 3, max: 255 }).withMessage('reason must be between 3 and 255 characters'),
  validateRequest
];
