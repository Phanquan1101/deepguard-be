# SePay Test Checklist - DeepGuard

## 1. Environment

- Backend local:
  `http://localhost:8080`

- Ngrok:
  `https://<ngrok-domain>`

- SePay webhook URL:
  `https://<ngrok-domain>/api/billing/payments/sepay/webhook`

- Required ENV:
  `SEPAY_WEBHOOK_SECRET`
  `SEPAY_BANK_CODE`
  `SEPAY_BANK_ACCOUNT_NO`
  `SEPAY_BANK_ACCOUNT_NAME`
  `SEPAY_QR_TEMPLATE`
  `SEPAY_QR_BASE_URL`
  `SEPAY_TRANSFER_CONTENT_PREFIX`

## 2. Dashboard SePay

- Bank account connected
- Webhook created
- Event type: money in
- Format: JSON
- Security: HMAC-SHA256
- Webhook URL points to current ngrok URL

## 3. Happy Path

1. Run backend.
2. Run ngrok.
3. Login user.
4. GET `/api/billing/pricing-plans`.
5. POST `/api/billing/payments/sepay/create`.
6. Copy `qrUrl`.
7. Scan QR with banking app.
8. Transfer exact amount.
9. Transfer content must contain `transactionCode`.
10. Wait for SePay webhook.
11. Check DB:
    - `payments.status = SUCCESS`
    - `subscriptions.status = ACTIVE`
    - `credit_credits.remaining_credits` increased
    - `credit_transactions` has `PURCHASE`
12. GET `/api/billing/payments/{paymentId}`.
13. Expected `status = SUCCESS`.

## 4. Duplicate Webhook

- Resend webhook or repeat same payload if possible.
- Expected:
  - No extra credits added
  - No duplicate CreditTransaction
  - Response Already processed or Success

## 5. Invalid Signature

- Send webhook with wrong signature.
- Expected:
  - HTTP 401 or `code=97`
  - Payment remains PENDING
  - No credit added

## 6. Amount Mismatch

- Payment amount is 50000 but webhook amount is 10000.
- Expected:
  - Payment remains PENDING
  - Subscription remains PENDING
  - No credit added

## 7. Wrong Account Number

- Webhook accountNumber differs from `SEPAY_BANK_ACCOUNT_NO`.
- Expected:
  - No payment update
  - Warning log

## 8. FE Polling Suggestion

After create SePay payment:
- FE displays `qrUrl`
- FE calls GET `/api/billing/payments/{paymentId}` every 3-5 seconds
- If `status == SUCCESS`:
  show success page
- If `status == PENDING`:
  keep waiting
- If `status == FAILED/CANCELLED`:
  show failed page
