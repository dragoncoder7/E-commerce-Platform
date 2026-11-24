# Stripe setup and project readiness

## Setup status

- Requested products: Payments, Billing, Connect, Invoicing.
- Business URL: http://skydragon.icu.
- `codex plugin add stripe@openai-curated` failed with `plugin stripe was not found in marketplace openai-curated`, including a retry outside the filesystem sandbox.
- Stripe plugin skills are available in the current chat.
- Added the global MCP server with `codex mcp add stripe --url https://mcp.stripe.com`.
- OAuth authentication succeeded. The Stripe MCP tools became available in the current chat without a restart.
- Confirmed `stripe_implementation_planner` is available.
- Available account: `skyDragon sandbox`, test mode. Account selection is awaiting the user's response.
- The skills installation fallback was unnecessary and was not run.

This is a setup/readiness record, not a completed planner-generated integration plan. No Stripe resources or payment application code have been created.

## Repository findings

| Area | Observed implementation | Implication for integration |
| --- | --- | --- |
| Backend | Java 21, Maven, Spring Boot 3.3.5, Spring Cloud, MySQL/MyBatis, Redis and Nacos | Use the Java Stripe SDK with an injected `StripeClient`; integrate with existing service boundaries. |
| Frontend | Vue 3 and TypeScript ERP admin application | Buyer checkout and seller onboarding need explicit UI entry points. The root README describes the buyer storefront as planned. |
| Product data | `system-service` has product CRUD, `salePrice` as `BigDecimal`, and stock | Checkout needs server-authoritative price snapshots, currency rules and inventory reservations. |
| Orders/payments | No order/payment implementation found in the inspected Java source and SQL schemas | Establish durable order/payment state before accepting real payments. |
| Stripe | No Stripe SDK dependency, Checkout handler, connected-account mapping or webhook handler found in inspected Java/XML/TypeScript/Vue files | Treat this as a new Stripe integration, rather than a migration. |
| Gateway | Existing routes cover authentication, menus, dashboard, products and uploads | Payment API routes and signature-authenticated webhook routing will need explicit configuration. |
| Authentication | JWT utility exists; inspected gateway Java source contains only its application class; system service uses security crypto | Verify and implement actual authentication and per-user/order/seller authorization before exposing payment operations. Configuration properties alone do not prove enforcement. |
| Secret storage | `server/.env.local` is ignored; `server/.env` is untracked and not ignored | Use injected environment variables or an ignored local file for Stripe credentials. Do not put credentials in the existing unignored `.env`. |
| Website | The supplied HTTP URL returned a Tencent Cloud filing/access notice | The public page could not establish business model, seller roles, fees or payment responsibilities. |

Inspection covered the root README, Maven manifests, admin package manifest, product entity/controller, gateway configuration and Java file inventory, SQL schemas, and Stripe-related searches in application source. This is not a runtime or security audit.

## Inputs required by the planner

1. Confirm the Stripe sandbox to use.
2. Identify the funds flow: direct store, marketplace collecting and distributing payments, or software for independent stores accepting their own payments.
3. Establish what is sold, the business/account country, currencies and seller/customer countries.
4. Identify who subscribes and what they subscribe to; distinguish buyer subscriptions from seller platform subscriptions.
5. Establish whether invoices are standalone payment requests or documents for already-paid purchases.
6. For Connect, establish whether a cart can contain multiple sellers, when transfers occur, seller dashboard/onboarding requirements, fee structure, and refund/dispute responsibilities.

## Credential handling for the implementation

MCP OAuth authorizes the assistant's Stripe tools; it does not supply a runtime API key to the Spring application. Prefer a sandbox restricted API key with permissions matched to the selected flow. Configure the runtime key through `STRIPE_API_KEY` and webhook signing secrets through separate server-only environment variables. Keep all restricted/secret keys and webhook secrets out of Vue `VITE_*` variables, committed configuration, logs and chat messages.

No Stripe-named environment variables were present in the inspected shell. Existing local secret file contents were not read. Spring Boot does not automatically load arbitrary `.env` files; the launcher or deployment must inject their values.

## Validation to include after implementation

- Server rejects client-controlled amounts, unauthorized order/customer/seller IDs and unsupported currency/quantity combinations.
- Repeated checkout requests reuse the same payment attempt safely.
- Invalid webhook signatures fail; duplicate and out-of-order events cannot double-fulfill or regress a paid order.
- Delayed payment success and failure are handled, and a success-page redirect alone never fulfills an order.
- Subscription activation, renewal, payment failure and cancellation update entitlements correctly.
- Standalone invoice collection is reconciled without charging an already-paid order twice.
- Connect transfer readiness, refunds, transfer reversals and disputes match the selected funds flow.
- Sandbox end-to-end checkout and webhook delivery are exercised before live configuration.

For Billing, Checkout and Invoicing, evaluate Stripe Tax based on actual jurisdictions. Enable automatic tax only after verifying the responsible account's tax settings and applicable active registrations; setting the flag alone does not establish tax collection.

## Sources

- [Stripe MCP setup](https://docs.stripe.com/mcp)
- [Stripe Checkout](https://docs.stripe.com/payments/checkout)
- [Stripe webhooks](https://docs.stripe.com/webhooks)
- [Stripe webhook endpoint troubleshooting](https://docs.stripe.com/events/manage-webhook-endpoints)
- [Stripe Tax setup](https://docs.stripe.com/tax/set-up)
- [Restricted API keys](https://docs.stripe.com/keys/restricted-api-keys)
