# LLM-Assisted Medical Incident Triage System (Akka + Java)

## Project Summary

This project is a Java-based Akka actor system that simulates medical software incident triage. The system processes multiple incidents concurrently, analyzes each incident using deterministic safety rules and an LLM-style analysis component, validates the LLM output through a rule-based safety layer, escalates alerts based on severity, and records audit logs.

The project is designed as an IEC 62304-inspired class project. It is not real medical software and is not intended for clinical use. The goal is to demonstrate safe software design principles, actor-based concurrency, AI-assisted reasoning, traceability, and auditability in a simulated medical incident workflow.

---

## Scope Strategy

The project will be implemented in two layers:

1. Core 20-hour implementation
   - Required for the final class submission.
   - Focuses on a complete, runnable Akka actor workflow.
   - Uses a mock LLM provider for reliable demos and tests.

2. Optional stretch features
   - Implemented only if time remains.
   - Includes real LLM API integration, simple persistence, and a user interface.

This keeps the project achievable while still leaving room for higher-impact features.

---

## Core Goals

- Demonstrate the Akka actor model using Java.
- Process multiple incidents concurrently.
- Create one case actor per incident.
- Run risk, history, and policy analysis in parallel.
- Use a mock LLM provider to generate structured analysis.
- Validate LLM output with deterministic safety rules.
- Prevent the LLM from overriding critical safety rules.
- Escalate incidents based on final severity.
- Maintain structured audit logs.
- Provide lightweight IEC 62304-inspired documentation.
- Include focused tests for safety rules and actor workflow.

---

## Non-Goals for the Core Version

The first version will not include:

- Real clinical data.
- Real autonomous medical decision-making.
- Production regulatory compliance.
- Full IEC 62304 documentation package.
- Full database-backed persistence.
- Authentication or authorization.
- Complex frontend dashboard.
- Deployment to cloud infrastructure.

These are intentionally excluded to keep the final project feasible within the available time.

---

## System Architecture

### Required Actors

#### 1. IncidentSimulatorActor

- Generates simulated medical software incidents.
- Sends incidents into the system for processing.
- Example incident types:
  - `oxygen_drop`
  - `sensor_dropout`
  - `dose_sync_failure`

#### 2. IngestionActor

- Validates incoming incident events.
- Normalizes fields such as severity and event type.
- Adds metadata such as timestamp and event ID if missing.
- Rejects malformed incidents.

#### 3. CaseManagerActor

- Receives normalized incidents.
- Creates one `IncidentCaseActor` per incident.
- Tracks active cases.

#### 4. IncidentCaseActor

- Coordinates processing for a single incident.
- Sends parallel requests to:
  - `RiskActor`
  - `HistoryActor`
  - `PolicyActor`
- Combines incident, risk, history, and policy context.
- Sends combined context to `LLMAnalysisActor`.
- Sends the LLM result and deterministic context to `DecisionActor`.

#### 5. RiskActor

- Applies deterministic risk rules.
- Produces a risk level and explanation.

Example rule:

```text
oxygen_drop + high severity -> CRITICAL
```

#### 6. HistoryActor

- Checks recent in-memory incident history.
- Detects repeated failures.
- Example:

```text
sensor_dropout occurred 4 times in the last simulated time window -> repeated failure
```

#### 7. PolicyActor

- Applies deterministic safety policies.
- Example policies:
  - Critical incidents require human review.
  - High-risk incidents cannot be auto-dismissed.
  - Repeated monitoring failures require escalation.

#### 8. LLMAnalysisActor

- Sends combined context to an `LlmProvider`.
- Uses `MockLlmProvider` in the required version.
- Returns structured output:
  - analysis
  - recommended action
  - confidence

The LLM is used for explanation and recommendation, not as the final safety authority.

#### 9. DecisionActor

- Validates the LLM output.
- Applies deterministic fallback behavior if the LLM output is invalid or unavailable.
- Ensures LLM recommendations cannot downgrade deterministic risk.
- Produces the final triage decision.

Example safety behavior:

```text
If RiskActor says CRITICAL, the final decision must require escalation,
even if the LLM recommends a lower-priority action.
```

#### 10. EscalationActor

- Routes alerts based on final severity.

| Final Severity | Escalation Target |
|---|---|
| Low | Nurse |
| Medium | General Doctor |
| High | Specialist |
| Critical | Senior Doctor |

#### 11. AuditActor

- Records structured audit events.
- Writes audit logs to console and/or JSONL file.

Audit logs include:

- incident ID
- incident type
- risk result
- history result
- policy result
- LLM output
- final decision
- escalation target
- timestamp

---

## Optional Actors / Interfaces

### UserQueryActor

Optional if time remains.

Allows a user to ask:

- Why was this incident marked critical?
- What action was taken for this event ID?
- What happened recently?

For the first version, this can be implemented as a simple console prompt rather than a full web UI.

---

## LLM Design

The LLM component is intentionally placed behind an interface:

```java
public interface LlmProvider {
    CompletionStage<LlmAnalysis> analyze(IncidentContext context);
}
```

Required implementation:

```text
MockLlmProvider
```

Optional implementation:

```text
OpenAiLlmProvider or another real API provider
```

This makes the project reliable for demos while still allowing real LLM integration later.

### Why the LLM Receives Combined Context

The LLM should not analyze only the raw incident. It receives combined context from:

- the original incident,
- deterministic risk analysis,
- recent incident history,
- safety policy results.

This lets the LLM produce a useful human-readable explanation.

Example combined context:

```json
{
  "incident": {
    "eventType": "oxygen_drop",
    "severity": "high",
    "details": "SpO2 < 88% for 40s"
  },
  "risk": {
    "riskLevel": "CRITICAL",
    "reason": "Sustained oxygen desaturation"
  },
  "history": {
    "similarIncidents": 3,
    "repeatedFailure": true
  },
  "policy": {
    "requiresHumanReview": true,
    "autoDismissAllowed": false
  }
}
```

Example LLM output:

```json
{
  "analysis": "The incident indicates sustained oxygen desaturation with repeated recent occurrences.",
  "recommendedAction": "Escalate to senior doctor and verify patient condition immediately.",
  "confidence": 0.88
}
```

The `DecisionActor` still makes the final validated decision using deterministic safety rules.

---

## Concurrency Design

### Multiple Incidents

Each incident gets its own `IncidentCaseActor`.

```text
Incident A -> IncidentCaseActor A
Incident B -> IncidentCaseActor B
Incident C -> IncidentCaseActor C
```

This demonstrates actor isolation and concurrent case processing.

### Parallel Analysis Per Incident

Each `IncidentCaseActor` requests analysis from:

- `RiskActor`
- `HistoryActor`
- `PolicyActor`

These analyses run concurrently. The case actor combines the results once all required responses are received.

---

## System Flow

1. `IncidentSimulatorActor` generates a simulated incident.
2. `IngestionActor` validates and normalizes the incident.
3. `CaseManagerActor` creates an `IncidentCaseActor`.
4. `IncidentCaseActor` requests risk, history, and policy analysis in parallel.
5. `IncidentCaseActor` combines the analysis results.
6. `LLMAnalysisActor` generates a structured explanation and recommendation.
7. `DecisionActor` validates the LLM output against deterministic safety rules.
8. `EscalationActor` routes the alert.
9. `AuditActor` logs the full workflow.

---

## Example Incident

```json
{
  "eventId": "EVT-1001",
  "system": "VitalsMonitor",
  "eventType": "oxygen_drop",
  "severity": "high",
  "details": "SpO2 < 88% for 40s"
}
```

---

## Example Final Decision

```json
{
  "eventId": "EVT-1001",
  "finalSeverity": "CRITICAL",
  "finalAction": "ESCALATE",
  "escalationTarget": "Senior Doctor",
  "humanReviewRequired": true,
  "llmAccepted": true,
  "reason": "Deterministic risk rules classified oxygen_drop with high severity as critical."
}
```

---

## Audit Log Format

The core version will write structured JSONL audit records.

Example:

```json
{
  "timestamp": "2026-04-20T14:31:22Z",
  "eventId": "EVT-1001",
  "actor": "DecisionActor",
  "action": "FINAL_DECISION_CREATED",
  "riskLevel": "CRITICAL",
  "llmRecommendation": "Escalate to senior doctor",
  "finalDecision": "ESCALATE",
  "escalationTarget": "Senior Doctor",
  "policyApplied": ["NO_AUTO_DISMISS_CRITICAL"]
}
```

---

## Lightweight IEC 62304-Inspired Documentation

The project will include a small `docs/` folder with concise documentation:

```text
docs/
  software-requirements.md
  risk-management.md
  traceability-matrix.md
  soup-inventory.md
```

### Documentation Purpose

#### software-requirements.md

Defines the system requirements, such as:

- The system shall process multiple incidents concurrently.
- The system shall escalate critical incidents.
- The system shall reject invalid LLM output.
- The system shall maintain an audit log.

#### risk-management.md

Defines simulated safety risks and controls, such as:

- Risk: LLM recommends unsafe downgrade.
- Control: DecisionActor prevents downgrading deterministic risk.

#### traceability-matrix.md

Maps requirements to implementation and tests.

Example:

```text
REQ-003 -> DecisionActor -> DecisionActorTest
```

#### soup-inventory.md

Documents third-party software used by the project, such as:

- Java JDK
- Akka
- Jackson
- JUnit
- logging library
- optional LLM API client

---

## Testing Plan

The core version should include focused tests for:

- risk classification rules,
- policy rules,
- decision validation,
- LLM fallback behavior,
- escalation routing,
- actor message flow,
- audit event creation.

The goal is not exhaustive regulatory-level testing. The goal is to show that the safety-critical behavior is verified.

---

## Optional Stretch Features

These features can be implemented if the core system is finished early.

### 1. Real LLM API Provider

Add a real provider behind the existing `LlmProvider` interface.

Example:

```text
LlmProvider
  MockLlmProvider
  OpenAiLlmProvider
```

The real provider should still return the same structured `LlmAnalysis` object used by the mock provider.

### 2. Simple User Interface

Possible options:

- console prompt interface,
- simple command menu,
- minimal web page,
- simple JavaFX view.

Recommended first UI:

```text
> submit oxygen_drop high "SpO2 < 88% for 40s"
> status EVT-1001
> why EVT-1001
> recent
```

### 3. SQLite Persistence

Add a local SQLite database file for incident and decision history.

Example:

```text
data/incidents.db
```

SQLite is preferred over Docker PostgreSQL for this project because it is free, local, simple, and does not require a running database server.

### 4. Docker PostgreSQL Persistence

Use only if there is extra time and a stronger database demo is needed.

This is lower priority than the real LLM provider because the class focus is Akka actors and AI.

### 5. CSV or Excel Export

Export incident history or audit logs for review.

This should be treated as export functionality, not as the main database.

### 6. More Advanced Query Actor

Expand `UserQueryActor` to answer audit questions:

- Why was an incident escalated?
- Which policies were applied?
- Did the LLM output get accepted or rejected?
- What recent incidents affected the decision?

---

## Recommended Implementation Priority

1. Core actor workflow.
2. Mock LLM provider.
3. Decision validation and safety fallback.
4. Audit logging.
5. Tests.
6. Lightweight documentation.
7. Real LLM API provider, if time remains.
8. Console user interface, if time remains.
9. SQLite persistence, if time remains.
10. Web UI or Docker database, only if everything else is complete.

---

## Resume Positioning

Suggested resume description:

```text
Built an IEC 62304-inspired Java/Akka medical incident triage simulation that processes concurrent safety events, applies deterministic risk controls, validates LLM recommendations, enforces human escalation policies, and maintains auditable traceability from requirements to tests.
```

Another version:

```text
Designed an actor-based AI safety workflow using Akka Typed, mock/optional real LLM providers, rule-based risk validation, structured audit logs, and lightweight medical software lifecycle documentation.
```
