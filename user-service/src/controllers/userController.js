import userService from '../services/userService.js';

class UserController {
  async register(req, res, next) {
    try {
      const { name, email, password } = req.body;
      const user = await userService.registerUser({ name, email, password });
      
      res.status(201).json({
        status: 'success',
        message: 'User registered successfully',
        data: { user },
      });
    } catch (err) {
      next(err);
    }
  }

  async login(req, res, next) {
    try {
      const { email, password } = req.body;
      const user = await userService.loginUser(email, password);

      res.status(200).json({
        status: 'success',
        message: 'Login successful',
        data: { user },
      });
    } catch (err) {
      next(err);
    }
  }

  async getById(req, res, next) {
    try {
      const { id } = req.params;
      const user = await userService.getUserById(id);

      res.status(200).json({
        status: 'success',
        data: { user },
      });
    } catch (err) {
      next(err);
    }
  }

  async update(req, res, next) {
    try {
      const { id } = req.params;
      const { name, email, password } = req.body;
      
      const user = await userService.updateUser(id, { name, email, password });

      res.status(200).json({
        status: 'success',
        message: 'User updated successfully',
        data: { user },
      });
    } catch (err) {
      next(err);
    }
  }
}

export default new UserController();
export { UserController };
