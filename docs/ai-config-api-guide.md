# Developer Integration Guide: AI Configuration API Contract

This guide helps both backend developers (`BARCAN-TAG-02`) and frontend developers (`BARCAN-TAG-11`) build against the shared API contract defined in [`docs/openapi-ai-config.yaml`](openapi-ai-config.yaml) in parallel, preventing contract drift and ensuring full alignment.

---

## 1. Context and Core Requirements

### A. Update Persona / System Prompt
* **Endpoint:** `PUT /api/v1/ai-config/system-prompt` (or `PUT /api/v1/ai-config`)
* **Acceptance Criteria:** The endpoint accepts system prompt text (up to 8000 characters).
* **Payload Shape:**
  ```json
  {
    "systemPrompt": "You are a highly analytical AI core. Be clinical and precise."
  }
  ```

### B. Intent & Stop-Trigger Rules
* **Endpoint:** `PUT /api/v1/ai-config/intent-rules` (or `PUT /api/v1/ai-config`)
* **Acceptance Criteria:** These rules are structured as an array of JSON objects.
* **Payload Shape:**
  ```json
  [
    {
      "id": "rule-101",
      "intentName": "payment_failure",
      "keywords": ["payment failed", "error 402", "unauthorized"],
      "action": "HUMAN_TAKEOVER",
      "priority": 1,
      "enabled": true
    },
    {
      "id": "rule-102",
      "intentName": "greeting",
      "keywords": ["hi", "hello", "hey"],
      "action": "AUTO_RESPOND",
      "priority": 2,
      "enabled": true
    }
  ]
  ```

---

## 2. Parallel Development Guidelines (Avoiding Drift)

To ensure smooth integration when merging parallel branches, keep these guidelines in mind:

### Frontend Team (`BARCAN-TAG-11`):
1. **Mock Responses:** Write tests or mock API clients using the exact payload formats defined in `docs/openapi-ai-config.yaml`.
2. **Strict Typings:** Map the OpenAPI schema definitions directly to TypeScript/Svelte types. Do not use generic `any` types for `intentRules`; ensure it maps to `{ id: string, intentName: string, keywords: string[], action: 'HUMAN_TAKEOVER' | 'STOP_GENERATION' | 'AUTO_RESPOND', priority: number, enabled: boolean }[]`.
3. **Validation Guards:** Validate the system prompt length locally (< 8000 characters) before sending updates to the backend.

### Backend Team (`BARCAN-TAG-02`):
1. **DTO Mapping:** Create Java/Kotlin DTOs corresponding to the OpenAPI definitions. Ensure validation annotations like `@NotBlank`, `@Size(max = 8000)`, and `@NotNull` are present.
2. **Transactional & Optimistic Locking:** If multiple operators configure settings simultaneously, enforce conditional/optimistic updates on the configuration entities to prevent overwriting parallel changes.
3. **Database Schema:** Ground the configuration storage in a table structure mapping to these DTOs. If database migrations are needed, remember to use Flyway with the pre-allocated version specified in the project context.

---

## 3. Mock Payload Example (Full Configuration)

**GET /api/v1/ai-config**
```json
{
  "systemPrompt": "You are a highly analytical AI core developed for Synthetic Logic.\nYour tone is clinical, precise, and devoid of unnecessary emotive flourishes.\nPrioritize technical accuracy above all else.",
  "stopTriggers": [
    "ERR_TIMEOUT",
    "root_access",
    "kernel_panic",
    "[REDACTED]"
  ],
  "intentRules": [
    {
      "id": "rule-101",
      "intentName": "payment_failure",
      "keywords": ["payment failed", "error 402", "unauthorized"],
      "action": "HUMAN_TAKEOVER",
      "priority": 1,
      "enabled": true
    }
  ]
}
```
