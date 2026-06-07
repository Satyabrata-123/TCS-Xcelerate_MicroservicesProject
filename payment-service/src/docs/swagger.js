import swaggerJsdoc from 'swagger-jsdoc';

const options = {
  definition: {
    openapi: '3.0.0',
    info: {
      title: 'Payment Service API',
      version: '1.0.0',
      description: 'Production-ready Payment Microservice API documentation with Idempotency support',
    },
    servers: [
      {
        url: 'http://localhost:5002',
        description: 'Local standalone service port',
      },
      {
        url: 'http://localhost:8080/payment',
        description: 'API Gateway routed endpoint',
      },
    ],
    components: {
      schemas: {
        Payment: {
          type: 'object',
          properties: {
            id: {
              type: 'string',
              format: 'uuid',
              example: 'b1407384-e113-4956-a5db-86c6b4e9d400',
            },
            orderId: {
              type: 'string',
              format: 'uuid',
              example: 'd3b07384-d113-4956-a5db-86c6b4e9d400',
            },
            amount: {
              type: 'string',
              example: '150.50',
            },
            currency: {
              type: 'string',
              example: 'USD',
            },
            paymentMethod: {
              type: 'string',
              example: 'CREDIT_CARD',
            },
            status: {
              type: 'string',
              enum: ['PENDING', 'SUCCESS', 'FAILED', 'REFUNDED'],
              example: 'SUCCESS',
            },
            idempotencyKey: {
              type: 'string',
              example: 'msg-id-1234567-abcdef',
            },
            errorMessage: {
              type: 'string',
              nullable: true,
              example: null,
            },
            refundReason: {
              type: 'string',
              nullable: true,
              example: null,
            },
            createdAt: {
              type: 'string',
              format: 'date-time',
              example: '2026-06-07T12:00:00.000Z',
            },
            updatedAt: {
              type: 'string',
              format: 'date-time',
              example: '2026-06-07T12:00:00.000Z',
            },
          },
        },
        ProcessPaymentInput: {
          type: 'object',
          required: ['orderId', 'amount', 'currency', 'paymentMethod'],
          properties: {
            orderId: {
              type: 'string',
              format: 'uuid',
              example: 'd3b07384-d113-4956-a5db-86c6b4e9d400',
            },
            amount: {
              type: 'number',
              format: 'float',
              example: 150.50,
            },
            currency: {
              type: 'string',
              example: 'USD',
            },
            paymentMethod: {
              type: 'string',
              example: 'CREDIT_CARD',
            },
            idempotencyKey: {
              type: 'string',
              description: 'Optional in request body if X-Idempotency-Key header is supplied.',
              example: 'msg-id-1234567-abcdef',
            },
          },
        },
        RefundInput: {
          type: 'object',
          properties: {
            reason: {
              type: 'string',
              example: 'Customer requested a refund',
            },
          },
        },
        ErrorResponse: {
          type: 'object',
          properties: {
            status: {
              type: 'string',
              example: 'fail',
            },
            message: {
              type: 'string',
              example: 'Idempotency key conflict',
            },
            errors: {
              type: 'array',
              items: {
                type: 'object',
                properties: {
                  field: {
                    type: 'string',
                    example: 'orderId',
                  },
                  message: {
                    type: 'string',
                    example: 'orderId is required',
                  },
                },
              },
            },
          },
        },
      },
      parameters: {
        IdempotencyKeyHeader: {
          name: 'Idempotency-Key',
          in: 'header',
          required: false,
          schema: {
            type: 'string',
          },
          description: 'Unique string to identify this transaction request and prevent double execution.',
          example: 'msg-id-1234567-abcdef',
        },
      },
    },
    paths: {
      '/api/payments': {
        post: {
          summary: 'Process a new payment',
          description: 'Submits a payment for authorization. To guarantee idempotency, provide the Idempotency-Key header.',
          tags: ['Payments'],
          parameters: [
            {
              $ref: '#/components/parameters/IdempotencyKeyHeader',
            },
          ],
          requestBody: {
            required: true,
            content: {
              'application/json': {
                schema: {
                  $ref: '#/components/schemas/ProcessPaymentInput',
                },
              },
            },
          },
          responses: {
            201: {
              description: 'Payment processed successfully',
              content: {
                'application/json': {
                  schema: {
                    type: 'object',
                    properties: {
                      status: { type: 'string', example: 'success' },
                      message: { type: 'string', example: 'Payment processed successfully' },
                      data: {
                        type: 'object',
                        properties: {
                          payment: { $ref: '#/components/schemas/Payment' },
                        },
                      },
                    },
                  },
                },
              },
            },
            200: {
              description: 'Duplicate request received. Returns original payment without reprocessing (Idempotent match)',
              content: {
                'application/json': {
                  schema: {
                    type: 'object',
                    properties: {
                      status: { type: 'string', example: 'success' },
                      message: { type: 'string', example: 'Payment retrieved from cache (Idempotent)' },
                      data: {
                        type: 'object',
                        properties: {
                          payment: { $ref: '#/components/schemas/Payment' },
                        },
                      },
                    },
                  },
                },
              },
            },
            400: {
              description: 'Validation error or missing idempotency key',
              content: {
                'application/json': {
                  schema: { $ref: '#/components/schemas/ErrorResponse' },
                },
              },
            },
            409: {
              description: 'Idempotency key reuse collision (request parameters do not match original payment)',
              content: {
                'application/json': {
                  schema: { $ref: '#/components/schemas/ErrorResponse' },
                },
              },
            },
          },
        },
      },
      '/api/payments/{id}': {
        get: {
          summary: 'Get payment details by ID',
          tags: ['Payments'],
          parameters: [
            {
              name: 'id',
              in: 'path',
              required: true,
              description: 'UUID of the payment to retrieve',
              schema: {
                type: 'string',
                format: 'uuid',
              },
            },
          ],
          responses: {
            200: {
              description: 'Payment details found',
              content: {
                'application/json': {
                  schema: {
                    type: 'object',
                    properties: {
                      status: { type: 'string', example: 'success' },
                      data: {
                        type: 'object',
                        properties: {
                          payment: { $ref: '#/components/schemas/Payment' },
                        },
                      },
                    },
                  },
                },
              },
            },
            404: {
              description: 'Payment not found',
              content: {
                'application/json': {
                  schema: { $ref: '#/components/schemas/ErrorResponse' },
                },
              },
            },
          },
        },
      },
      '/api/payments/{id}/refund': {
        post: {
          summary: 'Refund a successful payment',
          tags: ['Payments'],
          parameters: [
            {
              name: 'id',
              in: 'path',
              required: true,
              description: 'UUID of the payment to refund',
              schema: {
                type: 'string',
                format: 'uuid',
              },
            },
          ],
          requestBody: {
            required: false,
            content: {
              'application/json': {
                schema: {
                  $ref: '#/components/schemas/RefundInput',
                },
              },
            },
          },
          responses: {
            200: {
              description: 'Payment refunded successfully',
              content: {
                'application/json': {
                  schema: {
                    type: 'object',
                    properties: {
                      status: { type: 'string', example: 'success' },
                      message: { type: 'string', example: 'Payment has been refunded successfully' },
                      data: {
                        type: 'object',
                        properties: {
                          payment: { $ref: '#/components/schemas/Payment' },
                        },
                      },
                    },
                  },
                },
              },
            },
            400: {
              description: 'Payment already refunded or payment was not successful',
              content: {
                'application/json': {
                  schema: { $ref: '#/components/schemas/ErrorResponse' },
                },
              },
            },
            404: {
              description: 'Payment not found',
              content: {
                'application/json': {
                  schema: { $ref: '#/components/schemas/ErrorResponse' },
                },
              },
            },
          },
        },
      },
    },
  },
  apis: [],
};

const swaggerSpec = swaggerJsdoc(options);

export default swaggerSpec;
export { swaggerSpec };
