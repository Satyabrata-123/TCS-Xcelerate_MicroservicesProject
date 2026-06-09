import { Router } from 'express';
import { body, param } from 'express-validator';
import userController from '../controllers/userController.js';
import { validate } from '../middlewares/validate.js';

const router = Router();

// Validation Rules
const registerRules = [
  body('name')
    .trim()
    .notEmpty()
    .withMessage('Name is required')
    .isLength({ max: 50 })
    .withMessage('Name cannot exceed 50 characters'),
  body('email')
    .trim()
    .notEmpty()
    .withMessage('Email is required')
    .isEmail()
    .withMessage('Please provide a valid email address')
    .normalizeEmail(),
  body('password')
    .notEmpty()
    .withMessage('Password is required')
    .isLength({ min: 6 })
    .withMessage('Password must be at least 6 characters long'),
  body('role')
    .optional()
    .trim()
    .isIn(['CUSTOMER', 'ADMIN', 'RESTAURANT_OWNER'])
    .withMessage('Role must be one of CUSTOMER, ADMIN, RESTAURANT_OWNER'),
];

const loginRules = [
  body('email')
    .trim()
    .notEmpty()
    .withMessage('Email is required')
    .isEmail()
    .withMessage('Please provide a valid email address')
    .normalizeEmail(),
  body('password')
    .notEmpty()
    .withMessage('Password is required'),
];

const getByIdRules = [
  param('id')
    .isUUID()
    .withMessage('Invalid UUID format'),
];

const updateRules = [
  param('id')
    .isUUID()
    .withMessage('Invalid UUID format'),
  body('name')
    .optional()
    .trim()
    .notEmpty()
    .withMessage('Name cannot be empty if provided')
    .isLength({ max: 50 })
    .withMessage('Name cannot exceed 50 characters'),
  body('email')
    .optional()
    .trim()
    .notEmpty()
    .withMessage('Email cannot be empty if provided')
    .isEmail()
    .withMessage('Please provide a valid email address')
    .normalizeEmail(),
  body('password')
    .optional()
    .isLength({ min: 6 })
    .withMessage('Password must be at least 6 characters long'),
];

// Routes Configuration
router.post('/register', registerRules, validate, userController.register);
router.post('/login', loginRules, validate, userController.login);
router.get('/:id', getByIdRules, validate, userController.getById);
router.put('/:id', updateRules, validate, userController.update);

export default router;
