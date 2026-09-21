# Use Case: Check Connections

## Overview

**Use Case ID:** UC-037  
**Use Case Name:** Check Connections  
**Primary Actor:** System Administrator  
**Goal:** Check each outbound dependency of the tool with a bounded probe, so that an unreachable identity provider or telemetry collector is named here rather than inferred from a login that never completes.  
**Status:** Tested  
**Requirements:** [FR-045, NFR-015](../requirements.md)

## Preconditions

- The administrator is authenticated and holds the `elicit_admin` role (UC-001). That role grants no authoring access (UC-002).

## Main Success Scenario

1. The administrator opens Connections from the System section.
2. The system lists every outbound target it knows: the identity provider's discovery document and the telemetry collector, each read from the same configuration the runtime uses.
3. The administrator chooses Check all, or Check on one row.
4. For the identity provider the system fetches the discovery document and looks for an issuer; for the collector it opens and closes a connection; each records whether an answer came back and how long it took.
5. The system marks each target reachable or unreachable, with the detail beside it.
6. The administrator reads the result to find the dependency that is down or misaddressed.

## Alternative Flows

### A1: A target does not answer in time

**Trigger:** No answer within the bounded time (step 4)  
**Flow:**

1. The system marks the target unreachable with "timed out" and the limit, and moves on to the next target.
2. Use case continues at step 5.

### A2: The identity provider answers but not with a discovery document

**Trigger:** The discovery address answers without an issuer (step 4)  
**Flow:**

1. The system marks the provider reachable but misaddressed, showing the address it used.
2. Use case continues at step 5.

### A3: Nothing is configured

**Trigger:** Neither target has an address (step 2)  
**Flow:**

1. The system says no outbound targets are configured.
2. Use case ends.

## Postconditions

### Success Postconditions

- Each target carries a fresh result; nothing is stored and no target's state is changed.

### Failure Postconditions

- None; unreachable targets are reported, not retried.

## Business Rules

### BR-001: Probes are harmless

A web target is probed with a read-only request and redirects are not followed. A socket target is only connected to and closed.

### BR-002: Probes are bounded

Every probe gives up after five seconds (NFR-015). Checking all targets never takes longer than the sum of those limits, and one dead target never hides the other.

---

## Reference

Traces to FR-045. Implemented by `ConnectionChecks` and `SystemConnectionsView`. Verified by
`ConnectionChecksTest` and `SystemViewsRenderTest`. The identity-provider address is the one whose
resolution rules are explained in the umbrella `docker-compose.yml` (it must resolve from the container
and from the browser).
