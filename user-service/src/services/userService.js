import jwt from 'jsonwebtoken';
import userRepository from '../repositories/userRepository.js';
import { ConflictError, NotFoundError, UnauthorizedError } from '../utils/errors.js';

class UserService {
  generateToken(user) {
    const payload = {
      id: user.id,
      email: user.email,
      role: user.role,
    };
    const secret = process.env.JWT_SECRET || 'fallback_secret_key_123456789';
    const expiresIn = process.env.JWT_EXPIRES_IN || '24h';
    return jwt.sign(payload, secret, { expiresIn });
  }

  async registerUser(userData) {
    const existingUser = await userRepository.findByEmail(userData.email);
    if (existingUser) {
      throw new ConflictError('Email already registered');
    }

    return userRepository.create(userData);
  }

  async loginUser(email, password) {
    // We fetch including the password to compare it, since toJSON strips it normally, but findOne returns the instance
    const user = await userRepository.findByEmail(email);
    if (!user) {
      throw new UnauthorizedError('Invalid email or password');
    }

    const isMatch = await user.comparePassword(password);
    if (!isMatch) {
      throw new UnauthorizedError('Invalid email or password');
    }

    const token = this.generateToken(user);
    return { user, token };
  }

  async getUserById(id) {
    const user = await userRepository.findById(id);
    if (!user) {
      throw new NotFoundError(`User with ID ${id} not found`);
    }
    return user;
  }

  async updateUser(id, updateData) {
    const user = await userRepository.findById(id);
    if (!user) {
      throw new NotFoundError(`User with ID ${id} not found`);
    }

    if (updateData.email && updateData.email !== user.email) {
      const emailConflict = await userRepository.findByEmail(updateData.email);
      if (emailConflict) {
        throw new ConflictError('Email already in use');
      }
    }

    return userRepository.update(user, updateData);
  }
}

export default new UserService();
export { UserService };
