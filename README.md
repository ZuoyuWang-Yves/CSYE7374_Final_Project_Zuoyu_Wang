# Medical Incident Triage Demo

This project is a Java Akka Typed demo for medical incident triage. It takes incident reports, pushes them through an actor pipeline, writes audit traces to `logs/audit.jsonl`, and can also show the results in a small local browser UI.

The main goal is to show actor-based workflow, rule-based safety checks, and optional OpenAI support in one class project. It is a demo project, not real clinical software.

## Project context

This project was built as an IEC 62304-inspired class project. The idea is not to claim real medical software compliance, but to show a safer software design style in a small demo system.

In this project, that means:

- keeping the workflow modular with actors
- separating deterministic safety rules from LLM output
- making decisions traceable through audit logs
- testing the main rule paths and actor flow

So the project is inspired by IEC 62304 thinking around traceability, risk control, and software structure, but it is not a full IEC 62304 documentation package or a production medical device system.

## What it does

- Accepts raw incident input
- Normalizes the incident data
- Creates one case actor per incident
- Runs risk and history checks
- Applies policy rules
- Adds LLM analysis with either a mock provider or OpenAI
- Makes a final decision and routes escalation
- Saves trace and audit events to JSONL

## Main flow

Main actor chain:

`RootActor -> IngestionActor -> CaseManagerActor -> IncidentCaseActor`

Per-incident flow:

`RiskActor + HistoryActor -> PolicyActor -> LLMAnalysisActor -> DecisionActor -> EscalationActor -> AuditActor`

## Architecture summary

The actor system is split into small parts so each one has one job:

- `IncidentSimulatorActor`: sends demo incidents
- `IngestionActor`: validates and normalizes incoming incidents
- `CaseManagerActor`: creates one case actor per incident
- `IncidentCaseActor`: coordinates one incident from start to finish
- `RiskActor`: applies deterministic risk rules
- `HistoryActor`: checks recent incident history
- `PolicyActor`: applies policy rules
- `LLMAnalysisActor`: gets explanation/recommendation from the selected provider
- `DecisionActor`: makes the final validated decision
- `EscalationActor`: chooses who the incident should be routed to
- `AuditActor`: writes trace and audit events

Each incident gets its own `IncidentCaseActor`, so multiple incidents can be processed at the same time without mixing their state together.

## Safety and LLM approach

The project does not let the LLM act as the final authority. The LLM is there for analysis and recommendation, but the final decision still goes through deterministic logic.

Main safety ideas:

- risk rules are deterministic
- policy checks are deterministic
- invalid or missing LLM output falls back to rule-based behavior
- the final decision cannot ignore critical safety conditions
- every important step is written to the audit log

This is the main reason the project uses both rule-based logic and an LLM layer instead of relying only on one or the other.

## LLM modes

The LLM part is behind the `LlmProvider` interface.

- `MockLlmProvider`: default option for stable demos and tests
- `OpenAiLlmProvider`: built-in real API option

The OpenAI mode is optional. You can run the project fully in mock mode and still get the full actor workflow.

## Project layout

- `src/main/java/org/example/actor`: actor workflow
- `src/main/java/org/example/service`: rules, registry, LLM providers, and trace helpers
- `src/main/java/org/example/model`: shared records and enums
- `src/main/java/org/example/ui`: local HTTP server
- `src/main/resources/ui/index.html`: browser UI
- `src/test/java`: unit and actor flow tests

## Run modes

The app reads flags in `Main` through `ApplicationConfig`.

- `--demo`: runs the built-in sample incidents
- `--server`: starts the local UI server
- `--mock`: uses the mock LLM provider
- `--openai`: uses the OpenAI provider
- `--port=8080`: sets the UI port

Environment variables:

- `APP_MODE`
- `LLM_MODE`
- `UI_PORT`
- `OPENAI_API_KEY`
- `OPENAI_MODEL`
- `OPENAI_RESPONSES_ENDPOINT`

Default behavior is server mode with the mock LLM on port `8080`.

## How to run

This is a Maven project, and `pom.xml` is currently set to Java `26`.

Useful command:

```bash
mvn test
```

Run `org.example.Main` from IntelliJ with one of these argument sets:

```text
--demo --mock
--server --mock
--server --openai --port=8080
```

If you use `--openai`, set `OPENAI_API_KEY` first.

## Output

- Audit log: `logs/audit.jsonl`
- Local UI: `http://localhost:8080` by default

## Example incident

```json
{
  "eventId": "EVT-1001",
  "system": "VitalsMonitor",
  "eventType": "oxygen_drop",
  "severity": "high",
  "details": "SpO2 < 88% for 40s"
}
```

## Example final result

Typical outputs include:

- final severity
- final action
- escalation target
- whether the LLM output was accepted
- audit trace for the whole workflow

For example, a high-risk oxygen drop incident may end with escalation instead of monitor-only behavior because the deterministic rules still control the final decision.

## Testing

The test folder covers both rule logic and actor behavior.

Main test areas:

- risk rules
- policy rules
- decision fallback behavior
- escalation rules
- actor message flow
- audit event writing

The goal here is to show that the important safety behavior is checked, not to claim full regulatory-level test coverage.

## Scope notes

This repo focuses on the core workflow. It does not try to include:

- real clinical data
- production deployment
- full authentication/authorization
- full IEC 62304 document set
- full database-backed persistence
- a large production frontend

## Notes

- `RootActor.java` wires the whole system together.
- The audit log is useful for checking actor message order.
- Tests cover both rules and actor flow.
