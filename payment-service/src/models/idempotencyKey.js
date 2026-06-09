import { DataTypes } from 'sequelize';
import sequelize from '../config/database.js';

const IdempotencyKey = sequelize.define('IdempotencyKey', {
  id: {
    type: DataTypes.UUID,
    defaultValue: DataTypes.UUIDV4,
    primaryKey: true,
  },
  idempotencyKey: {
    type: DataTypes.STRING,
    allowNull: false,
    unique: true,
    validate: {
      notEmpty: true,
    },
  },
  requestPath: {
    type: DataTypes.STRING,
    allowNull: false,
    validate: {
      notEmpty: true,
    },
  },
  requestHash: {
    type: DataTypes.STRING,
    allowNull: false,
    validate: {
      notEmpty: true,
    },
  },
  status: {
    type: DataTypes.ENUM('STARTED', 'RESOLVED'),
    defaultValue: 'STARTED',
    allowNull: false,
  },
  responseStatusCode: {
    type: DataTypes.INTEGER,
    allowNull: true,
  },
  responseBody: {
    type: DataTypes.JSON,
    allowNull: true,
  },
}, {
  tableName: 'idempotency_keys',
  timestamps: true,
  indexes: [
    {
      unique: true,
      fields: ['idempotencyKey'],
    },
  ],
});

export default IdempotencyKey;
export { IdempotencyKey };
