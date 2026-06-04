import User from '../models/user.js';

class UserRepository {
  async create(userData) {
    return User.create(userData);
  }

  async findById(id) {
    return User.findByPk(id);
  }

  async findByEmail(email) {
    return User.findOne({ where: { email } });
  }

  async update(userInstance, updateData) {
    return userInstance.update(updateData);
  }
}

export default new UserRepository();
export { UserRepository };
