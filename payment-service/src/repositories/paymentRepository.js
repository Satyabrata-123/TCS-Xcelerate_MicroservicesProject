import Payment from '../models/payment.js';

class PaymentRepository {
  async create(paymentData, transaction = null) {
    return Payment.create(paymentData, { transaction });
  }

  async findById(id) {
    return Payment.findByPk(id);
  }

  async findByIdempotencyKey(idempotencyKey) {
    return Payment.findOne({ where: { idempotencyKey } });
  }

  async update(paymentInstance, updateData, transaction = null) {
    return paymentInstance.update(updateData, { transaction });
  }
}

export default new PaymentRepository();
export { PaymentRepository };
