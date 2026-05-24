# VNPAY Test Checklist

## A. Setup ENV

- `VNPAY_TMN_CODE`
- `VNPAY_HASH_SECRET`
- `VNPAY_PAY_URL`
- `VNPAY_RETURN_URL`
- `VNPAY_IPN_URL`

> For real sandbox callback testing, `VNPAY_IPN_URL` must be a public URL, for example via ngrok or a deployed domain. `localhost` cannot receive VNPay server callbacks.

## B. Seed Pricing Plans

Run `docs/sql/seed_pricing_plans.sql` manually in Supabase SQL Editor.

Expected plans:

- `BASIC`
- `PRO`
- `PREMIUM`

## C. Happy Path Flow

1. Login and copy Bearer token.
2. `GET /api/billing/pricing-plans`.
3. `POST /api/billing/payments/vnpay/create` with body `{ "pricingPlanId": "PRO" }`.
4. Open returned `paymentUrl` in browser.
5. Pay with VNPay sandbox.
6. Verify browser Return URL response.
7. Verify VNPay IPN callback reaches backend.
8. Check DB:
   - `payments.status = SUCCESS`
   - `subscriptions.status = ACTIVE`
   - `credit_credits.remaining_credits` increased
   - `credit_transactions` has `PURCHASE` row
9. `GET /api/billing/payments/me`.

## D. Fail Cases

- Invalid signature: expect IPN `RspCode = 97`.
- Invalid amount: expect IPN `RspCode = 04`.
- Order not found: expect IPN `RspCode = 01`.
- Duplicated success IPN: expect IPN `RspCode = 02`, no extra credits, no extra transaction.
