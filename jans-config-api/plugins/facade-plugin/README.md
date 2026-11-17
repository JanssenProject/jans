# facade-plugin

A Config API plugin that finds unusual behavior in authorization decisions.

It makes Cedarling authorize a set of requests, collects the signed TRACE records that Lock stores for those decisions, and trains the FACADE anomaly detector on them. FACADE then gives every action a score. A high score means "this principal does not normally do this".

**Contents**

1. [The idea in one minute](#the-idea-in-one-minute)
2. [Quick start](#quick-start)
3. [From `authorization.sample.json` to an anomaly score, step by step](#from-authorizationsamplejson-to-an-anomaly-score-step-by-step)
4. [What result to expect](#what-result-to-expect)
5. [The three jobs and their schedule](#the-three-jobs-and-their-schedule)
6. [Input: `authorization.json`](#input-authorizationjson)
7. [Output files](#output-files)
8. [Configuration](#configuration)
9. [Troubleshooting](#troubleshooting)
10. [Reference](#reference)

---

## The idea in one minute

FACADE learns what is normal. In the sample, ops engineers deploy workloads and support agents read customer records. Nobody crosses over. Then a few ops engineers read customer records. Each of those requests is allowed by policy, so no rule is broken, but it is out of character. FACADE should score those requests far above everything else.

```
 authorization.json                                        FACADE scores
        |                                                       ^
        v                                                       |
 [1 Send]  Cedarling authorizes the requests ----------> [3 Train] converts, trains, scores
           and sends a signed TRACE record                      ^
           for each decision to Lock                            |
                    |                                           |
                    v                                           |
              Lock Server  ---------- [2 Collect] fetches the records back
```

Three jobs run on timers, one hour apart by default. They are described in [The three jobs](#the-three-jobs-and-their-schedule).

## Quick start

**You need**

- A Janssen server with this plugin installed (see [Install](#install)).
- A Lock Server with TRACE enabled, set up as in [Lock Server](#lock-server).
- A client with the scopes `trace.admin`, `trace.write` and `trace.readonly`.
- For training: a FACADE checkout with a Python environment.

**1. Put the input in place.** The sample is inside the plugin jar:

```
mkdir -p /opt/jans/jetty/jans-config-api/custom/config/facade/input
unzip -p facade-plugin-<version>-distribution.jar facade/authorization.sample.json \
  > /opt/jans/jetty/jans-config-api/custom/config/facade/input/authorization.json
```

**2. Set the connection** as environment variables or system properties of Config API (see [Configuration](#configuration)):

```
facade.lock.base.url=https://your-lock-server
facade.lock.client.id=<client id>
FACADE_LOCK_CLIENT_SECRET=<client secret>     # never put the secret in a file
facade.dir=/path/to/facade                    # leave empty to skip training
```

**3. Restart Config API and wait.** With the default timers:

| Time after start | What happens |
|---|---|
| about 5 minutes | **Send**: Cedarling authorizes the requests. This takes a few minutes with the sample. |
| about 65 minutes | **Collect**: the records are fetched from Lock. |
| about 125 minutes | **Train**: FACADE is trained and scores the test period. |

**4. Read the result** in `/opt/jans/jetty/jans-config-api/custom/config/facade/output`:

- `logs/evaluate.log`: where the injected anomalies ranked, and the AUC.
- `logs/read_scores.log`: the 15 highest-scoring actions.
- `state.json`: what each job did last, and any error.

---

## From `authorization.sample.json` to an anomaly score, step by step

The sample file is `src/main/resources/facade/authorization.sample.json`. The numbers below are for that file as it is (seed 7). The plan is fixed by the seed, so a real run gives the same counts.

### Step 1. The plan: who does what

The send job reads the file and builds a list of **executions**. An execution is a short piece of teamwork, such as a deployment or a support shift.

- **10 users in two teams.** `ops` is Alice, Carol, Erin, Gina and Hank, all with the `admin` role. `support` is Bob, Dave, Ivy, Jack and Kim, with no role.
- **246 executions in total.** The first 200 are the **training** phase and the next 40 are the **test** phase. The last 6 are the **injected anomalies**.
- **Who is in an execution.** The plan works through each execution like this:
  1. It picks a team at random, `ops` or `support`.
  2. It picks 2, 3 or 4 different users of that team at random. This is the **core group**.
  3. With a probability of `cross_team_rate` (0.1, so about one execution in ten) it adds **one visitor**, a random user from the other team.
  4. All of them get the same `execution_id`, so an execution has 2 to 5 users, and nobody appears twice.

  For the sample's 240 normal executions that came out as: 209 with one team only (67 of 2 users, 69 of 3, 73 of 4) and 31 with a visitor. The visitor rate is random, so it lands near 10%, not on it. The 6 injected anomalies are separate executions of one user each.

  For example, `exec-0000` is Jack, Bob and Dave from `support`. `exec-0025` is Erin, Carol, Hank and Gina from `ops`, plus Ivy from `support` as the visitor.

  Why this matters: everyone who shares an `execution_id` becomes a "peer" of the others (see Step 5). FACADE learns from this who normally works with whom. The execution is also what Lock groups the TRACE records by.
- **What each user does.** Each user makes one batch call with 1 to 3 requests. `ops` users `Deploy` a workload. `support` users `ReadCustomer` for their own ticket. A user always does their own team's action, even when visiting the other team.
- **The anomalies.** Each of the 6 last executions has one `ops` user making a `ReadCustomer` request. The users are Alice, Carol, Erin, Gina, Hank, then Alice again.
- **Some requests are made to fail.** About 3% of requests carry a deliberate mistake: a deployment of the wrong workload, or a ticket assigned to someone else.

Every request is built from the templates in the file. One `support` request, for example:

```json
{
  "action": "AcmeOps::Action::\"ReadCustomer\"",
  "resource": { "kind": "customer-record", "customer": "customer-9", "environment": "prod", "workload": "none" },
  "context":  { "ticket_customer": "customer-9", "ticket_assignee": "Bob", "change_approved": false, "suspicious": false }
}
```

### Step 2. Cedarling decides

For each user's batch call, the job signs three tokens for that user (access, id and userinfo) with a local test issuer, and calls Cedarling. Cedarling checks every request against the policy store in `/opt/jans/jetty/jans-config-api/custom/config/input` (`facade.policystore.dir`). If that folder has no `metadata.json`, the `policy-store-acme` bundled in the plugin jar is used. The bundled one gives:

| Request | Allowed when |
|---|---|
| `Deploy` | The user has the `admin` role and `change_workload` equals the resource's workload. On `prod`, the change must also be approved and a human must be on call. |
| `ReadCustomer` | The resource's customer is the ticket's customer, and the ticket is assigned to the user. |

For the sample this gives **1,529 decisions**:

| | Allowed | Denied |
|---|---|---|
| `Deploy` (ops) | 750 | 32 |
| `ReadCustomer` (support) | 721 | 20 |
| `ReadCustomer` (the 6 injected anomalies) | 6 | 0 |
| **Total** | **1,477** | **52** |

The injected requests must be allowed. A denied request is not an action, so FACADE would never see it.

### Step 3. Each decision becomes a signed TRACE record in Lock

Cedarling signs one TRACE record per decision with an Ed25519 key that the job registered in Lock for this run, and sends it to Lock. A record says who, what, where and the outcome. These are the fields that matter later:

```json
{
  "record_id": "01a1e423-653b-79bc-9db9-bd7fff53e179",
  "trace": {
    "event_kind": "AUTHORIZATION_DECISION",
    "signed_at": 1794821685,
    "trace_execution_id": "exec-sim7-0000",
    "execution_authority": "https://gateway.example.org",
    "subject": { "workload_id": "Jack-inum-jack" },
    "event": { "decision": { "action": "AcmeOps::Action::\"ReadCustomer\"",
                             "resource_type": "AcmeOps::Resource", "outcome": "ALLOW" } }
  }
}
```

- `workload_id` is the user: `<given_name>-<inum>`.
- `trace_execution_id` ties together everyone in the same execution.

The job also saves `executions.json` (what it sent), `truth.json` (the record ids of the 6 anomalies) and `run.json` (the time the test phase started). This ends the **send** job.

### Step 4. The records are fetched back from Lock

An hour later, the **collect** job asks Lock for each execution (`GET /audit/trace/executions/{id}`) and saves the answer in `traces/<execution_id>.json`. It waits up to a minute for the expected number of records per execution. If Lock does not have them all yet, it tries again at its next run.

### Step 5. TRACE records become FACADE rows

The **train** job converts the traces to the two kinds of rows FACADE reads (`actions.jsonl` and `contexts.jsonl`, and the same data as TFRecords).

- **One action per allowed decision.** This is the thing being scored. The 52 denied decisions are left out (`skipped by outcome: 52` in `summary.txt`), which leaves **1,477 actions**:

  ```json
  {"type":"capability_use","id":"…/01a1e423-653b-79bc-9db9-bd7fff53e179:0",
   "principal":"Jack-inum-jack",
   "resource_id":"AcmeOps::Action::\"ReadCustomer\"|AcmeOps::Resource",
   "occurred_at":"2026-11-16T09:34:45Z"}
  ```

  The **principal** is the `workload_id`. The **capability** is `<action>|<resource type>`, here `ReadCustomer|AcmeOps::Resource`. FACADE sees only these two things for each action.

- **One context per user and execution.** It says who a user worked with: everyone in the same execution shares the same value of `execution`. This is how FACADE learns which principals normally work together (**758 contexts** for the sample):

  ```json
  {"type":"trace_peer","principal":"Bob-inum-bob","valid_from":"2026-11-16T09:34:45Z",
   "peer_attributes":[{"name":"execution","value":"https://gateway.example.org|exec-sim7-0000"}]}
  ```

### Step 6. Training and test periods

`run.json` gives the time the test phase started. Everything before it is training data. Everything after it is scored. For the sample this is about 60 seconds of training data and 14 seconds of test data, with FACADE looking at 5-second snapshots. The windows scale with the time the run took (the settings `snapshotSeconds`, `historySeconds` and so on stay at `auto`).

The 6 injected anomalies are in the test period. Before them, in 200 executions of training data, no `ops` user ever made a `ReadCustomer` request.

### Step 7. FACADE learns what is normal

Four FACADE commands run, in order, from the FACADE directory: `batch.dataset_maker_main` (builds training examples), `model.train_main` (trains the model), `batch.inference_main` (scores the test period), `batch.read_scores_main` (prints the top scores). Each writes a log in `logs/`.

What the model picks up from the training data, in plain words:

- Alice, Carol, Erin, Gina and Hank only ever `Deploy`.
- Bob, Dave, Ivy, Jack and Kim only ever `ReadCustomer`.
- People who share an execution are of one team, apart from the odd visitor.

### Step 8. Every action in the test period gets a score

The score says how unusual an action is for that principal and context. Low means expected. High means unexpected. The scores are in `scores.tfrecord`, and `logs/read_scores.log` shows the highest ones.

### Step 9. The check against the answer

Because the plugin injected the anomalies itself, it knows the answer. `evaluate_scores.py` looks up the 6 record ids from `truth.json` in the scores and writes `logs/evaluate.log`:

```
scored actions: 244, injected anomalies: 6, found in output: 6
  rank    1/244  score=0.1181  Erin-inum-erin    AcmeOps::Action::"ReadCustomer"|AcmeOps::Resource
  rank    2/244  score=0.1173  Gina-inum-gina    AcmeOps::Action::"ReadCustomer"|AcmeOps::Resource
  …
AUC (anomaly vs normal) = 1.000
```

Rank 1 is the highest score. An AUC of 1.0 means every anomaly scored higher than every normal action. 0.5 is no better than chance.

---

## What result to expect

### What FACADE should flag, and what it should leave alone

| Behavior | Should it score high? | Why |
|---|---|---|
| An `ops` user making `ReadCustomer` (the 6 injected requests) | **Yes** | New for that principal. All other `ops` activity is `Deploy`. |
| A normal `Deploy` by `ops`, or `ReadCustomer` by `support` | No | It is what they always do. |
| A `support` user visiting an `ops` execution | No | They do their own work, `ReadCustomer`. The visit is rare, but their action is normal. |
| The 52 denied requests | Not scored | A denial is not an action, so it never reaches FACADE. |

### What I measured

The send and collect jobs need a live Lock Server, which I could not use. So I checked the rest with the real code instead. Cedarling made the real decisions for the sample (the table in Step 2). I wrote a TRACE record for each one with the fields shown in Step 3 and timestamps spaced like the sample's 300 ms pause. Then I ran the plugin's own conversion, and a real FACADE training 20,000 examples, on them. I repeated this with different seeds. Treat it as a good indication, not as proof of a run through Lock.

| Run | Actions scored | Rank of the 6 anomalies | AUC | Anomaly scores | Highest normal score |
|---|---|---|---|---|---|
| Seed 7 | 244 | 1 to 6 | 1.000 | 0.110 to 0.118 | 0.024 |
| Seed 8, training 1 | 145 | 135 to 145 (the lowest) | 0.006 | 0.024 to 0.025 | 0.028 |
| Seed 8, training 2 (same data) | 145 | 1 to 6 | 1.000 | about 0.11 to 0.12 | not recorded |
| Seed 8, training 3 (same data) | 145 | 1 to 6 | 1.000 | about 0.11 to 0.12 | 0.025 |
| Seed 9 | 227 | 1 to 6 | 1.000 | 0.142 to 0.150 | 0.027 |

What to take from it:

- **A good run looks like this.** The 6 injected `ReadCustomer` actions by `ops` users take the top 6 places. Their scores, about 0.11 to 0.15, are four to six times the highest score of any normal action (about 0.03). There is a clear gap between the two groups. Alice appears twice because the 6 anomalies go round the 5 ops users.
- **Training is not repeatable.** The same data once trained a model that did not learn anything (seed 8, training 1: every score about 0.024, the anomalies at the bottom) and twice trained a good one. In 4 of 5 runs the result was good. Do not trust a single training run without looking at the scores.
- **The number of actions scored changes** (145 to 244), because it depends on where the 5-second snapshots fall in the short test period.
- **A real run will differ in the details.** Real signing and Lock add delay, so the run takes longer than the 74 seconds of pauses, and the scores will not match these digits.

### How to tell if a run went wrong

Open `logs/evaluate.log`. The run is good if the anomalies are at the top, and their scores are well above the others (about 0.1 against about 0.03). It is bad if the anomalies sit at the bottom with scores the same as everything else, as in seed 8, training 1. Train again: remove the `"train"` section from `state.json`, and the next train job runs on the same traces.

### What the check can and cannot tell you

The sample is an easy case: one clear difference between two groups, repeated in 200 executions. Finding it shows that the whole chain works: records are signed, stored, fetched, converted and scored. It does not show how well FACADE would catch subtle problems in your real traffic.

---

## The three jobs and their schedule

Three jobs run inside Config API. Each has its own timer and its own interval, one hour by default. They hand work to each other through `state.json` in the output directory.

| Job | What it does |
|---|---|
| **1. Send** | If the input directory has `authorization.json`, Cedarling authorizes the requests in it. Each decision becomes a signed TRACE record that Cedarling sends to Lock. The job saves the executions it sent in `executions.json`. |
| **2. Collect** | Fetches those records back from Lock into `traces/`. Lock takes time to store records, so if some are missing the job tries again on its next run (up to `facade.job.max.attempts`, default 3), then accepts what Lock has. |
| **3. Train** | Converts the collected traces to FACADE files, and trains and scores if a FACADE checkout is configured. It waits until the collect job has finished. |

By default the jobs are one interval apart, so the records are sent, one hour later collected, and one hour after that trained on:

| Job | First run | Then every |
|---|---|---|
| Send | `facade.send.initial.delay.seconds` after Config API starts (300) | `facade.send.interval.seconds` (3600) |
| Collect | one send interval after the first send (3900), or `facade.collect.initial.delay.seconds` | `facade.collect.interval.seconds` (3600) |
| Train | one collect interval after the first collect (7500), or `facade.train.initial.delay.seconds` | `facade.train.interval.seconds` (3600) |

If you change the send settings, the default first run of collect and train moves with them.

Each run of a job first checks whether it has anything to do, and does nothing if not:

- **Send** skips `authorization.json` when it is the same as at the last successful send (file name, size and modification time decide this). A new send clears the old traces, and collect and train then run for the new data. Set `facade.job.rerun.unchanged=true` to send on every run. That sends new synthetic records to Lock every hour.
- **Collect** skips a send it already collected.
- **Train** skips traces it already trained on. A failing training is tried again up to `facade.job.max.attempts` times, then the job waits for new data.
- Only one job runs at a time. A job that finds another running skips that run and tries again at its next interval.

`state.json` survives a restart of Config API. After a restart the timers start again from their initial delays.

**Without `authorization.json`**, the input directory can hold Lock trace files you already have. Then only the train job does work, and it trains on those files. See [Input files](#input-files-in-the-input-directory).

---

## Input: `authorization.json`

`authorization.sample.json` is a working example. Copy it to the input directory as `authorization.json` and change it.

| Field | Meaning |
|---|---|
| `execution_authority` | The trace `execution_authority`. |
| `client_id` | The `client_id` and `aud` of the generated tokens. |
| `traffic` | `seed`, `train_executions`, `test_executions`, `anomalies`, `pause_millis` (pause after each execution), `deny_rate`, `cross_team_rate`. |
| `customers`, `workloads`, `environments` | Customer count (`customer-1` to `customer-N`), workload names and environments that fill the templates. |
| `actions` | One entry per action, keyed by name: `action` (the Cedar action id), `resource` and `context` templates, and `deny_context` (context fields overwritten on the `deny_rate` share of requests). |
| `team_actions` | Which `actions` entry each team performs. |
| `anomaly` | The `team` and `action` (an `actions` name) of the injected anomalies. |
| `users` | `given_name`, `inum`, `roles` and `team` for each user. Each team in `team_actions` needs at least 2 users. The `workload_id` is `<given_name>-<inum>`. |

In the templates, `"${customer}"` is replaced by the value, and keeps its type when it is the whole string (so `"${is_prod}"` becomes a boolean). The variables are `given_name`, `inum`, `customer`, `workload`, `environment` and `is_prod`. An unknown variable stops the run.

The users sign in with tokens from a local test issuer that the job starts for the run on `http://localhost:<random port>`. The policy store is read from `facade.policystore.dir`. Put `metadata.json`, `schema.cedarschema` and `policies/` there; other files in the folder, such as `authorization.json`, are not packed. A `trusted-issuers.json` in it is ignored: the job generates one that trusts only the local issuer. With no `metadata.json` there, the bundled `policy-store-acme` is used.

Things to keep in mind when you change the file:

- The anomalies are only found if they differ from the normal behavior. The sample works because `ops` users never `ReadCustomer` in the training phase.
- An injected request must be **allowed** by the policy, or it never reaches FACADE.
- More `train_executions` gives FACADE more to learn from. More `pause_millis` makes the run longer, so FACADE gets longer windows.

### Input files in the input directory

| File | Meaning |
|---|---|
| `authorization.json` | The requests to authorize, as above. |
| `*.json`, `*.jsonl` | Lock execution responses (`GET /audit/trace/executions/{id}`), TRACE envelopes or bare TRACE assertions, if you already have traces. Only the train job uses them, and only when there is no `authorization.json`. |
| `run.json` (optional, with trace files) | `{"test_start": "<ISO instant>"}`. Splits training from scoring. Without it, the last 20% of the action time is scored. |
| `truth.json` (optional, with trace files) | Known anomalies, `{"anomalies": [{"record_id": "..."}]}`. Used to rank them in the scores. |

`run.json`, `truth.json`, `executions.json` and `authorization.json` are never read as traces. With `authorization.json`, the send job writes its own `run.json` and `truth.json` to the output directory.

## Output files

All in the output directory (`/opt/jans/jetty/jans-config-api/custom/config/facade/output` by default).

| File | Written by | Content |
|---|---|---|
| `state.json` | all jobs | The last result of each job (`SUCCESS`, `PARTIAL` or `FAILED`), with time, message and attempts. **Look here first.** |
| `executions.json`, `truth.json`, `run.json` | send | The executions that were sent, the injected anomalies, and the start of the test phase. Replaced on each send. |
| `traces/` | collect | The records fetched back from Lock, one file per execution. Replaced on each send. |
| `actions.jsonl`, `contexts.jsonl` | train | FACADE actions and contexts in readable form. |
| `action.tfrecord`, `context.tfrecord` | train | The same data as FACADE TFRecords. |
| `directive.textproto`, `config.textproto` | train | FACADE directive and model configuration. |
| `summary.txt` | train | Conversion counts and the time windows used. |
| `train.tfrecord`, `vocabs.tfrecord`, `model/` | FACADE | Training data, vocabulary and the trained model. |
| `scores.tfrecord` | FACADE | The anomaly score of every action in the test period. |
| `logs/read_scores.log` | FACADE | The 15 highest-scoring actions. |
| `logs/evaluate.log`, `evaluate_scores.py` | train | Where the injected anomalies ranked, and the AUC. Only with `truth.json`. |
| `logs/*.log` | train | The output of each FACADE command. |

## Configuration

Settings are in `facade.properties` inside the plugin jar. Any of them can be overridden with a system property or an environment variable (`facade.lock.client.secret` is `FACADE_LOCK_CLIENT_SECRET`). Those win over the file.

| Property | Default | Meaning |
|---|---|---|
| `facade.policystore.dir` | `/opt/jans/jetty/jans-config-api/custom/config/input` | Policy store folder (`metadata.json`, `schema.cedarschema`, `policies/`). Falls back to the bundled `policy-store-acme` if `metadata.json` is missing. |
| `facade.input.dir`, `facade.output.dir` | `/opt/jans/jetty/jans-config-api/custom/config/facade/input` and `…/output` | Directories. |
| `facade.job.enabled` | `true` | `false` turns all three jobs off. |
| `facade.send.initial.delay.seconds` | `300` | Delay before the first send. |
| `facade.send.interval.seconds` | `3600` | Time between sends. |
| `facade.collect.initial.delay.seconds` | send delay + send interval | Delay before the first collect. |
| `facade.collect.interval.seconds` | `3600` | Time between collects. |
| `facade.train.initial.delay.seconds` | collect delay + collect interval | Delay before the first train. |
| `facade.train.interval.seconds` | `3600` | Time between trainings. |
| `facade.job.max.attempts` | `3` | Tries for a collection or training that keeps failing, before the job waits for new data. |
| `facade.job.rerun.unchanged` | `false` | `true` makes the send job send even when `authorization.json` has not changed. |
| `facade.lock.base.url` | empty | Base URL of the Lock Server. Required for `authorization.json`. |
| `facade.auth.base.url` | same as `facade.lock.base.url` | Base URL of the Auth Server, for the token endpoint. |
| `facade.lock.client.id`, `facade.lock.client.secret` | empty | A client with the scopes `https://jans.io/oauth/lock/trace.admin`, `trace.write` and `trace.readonly`. Set the secret only through the environment or a system property, never in the file. |
| `facade.collect.timeout.seconds` | `60` | How long the collect job waits for each execution's records in Lock. |
| `facade.dir` | empty | FACADE checkout. Without it, the job only writes the converted files. |
| `facade.python` | `<facade.dir>/env/bin/python`, then `python3` | Python with FACADE's dependencies. |
| `facade.run` | `true` | `false` converts only. |
| `facade.trainingExamples` | `20000` | Training examples. |
| `facade.testFraction` | `0.2` | Share of the time range that is scored when there is no `run.json`. |
| `facade.snapshotSeconds`, `historySeconds`, `lookbackSeconds`, `dedupSeconds` | `auto` | Directive windows, scaled to the time span of the data. |
| `facade.actionKinds`, `facade.outcomes`, `facade.peerAttrs`, `facade.timeSource` | see file | Conversion options. |

The user that runs Config API needs read access to the input directory and write access to the output directory.

### Lock Server

Cedarling opens a new producer chain every time it starts, so the Lock `traceConfiguration` needs:

| Setting | Value | Without it |
|---|---|---|
| `chainRegistrationMode` | `FIRST_OBSERVED` | Every record is refused with `chain_not_registered`. |
| `defaultEvidenceDomainId` | a domain id, for example `default` | `403 client_not_bound` |
| `defaultAllowedProducerIds` | includes `facade-plugin` | `producer_not_allowed` |

Lock accepts a JWT issuer in a TRACE record only if it is `https`, or `http` with host `localhost`. The local test issuer uses `http://localhost:<port>`.

The job always verifies TLS certificates, and there is no option to turn that off. Cedarling uses its own connection to Lock, so the Lock certificate must be trusted there too.

## Troubleshooting

| What you see | Cause and fix |
|---|---|
| Nothing happens | Check `facade.job.enabled`, and that the input directory has `authorization.json` or trace files. Look at `state.json` and the Config API log (search for `FACADE`). |
| `state.json`: send `FAILED`, message about `facade.lock.base.url` | Set the Lock connection. See [Configuration](#configuration). |
| Send `FAILED`, `token request … HTTP 401` | Wrong client id or secret, or the client lacks the three trace scopes. |
| Send `FAILED`, certificate error | The Lock or Auth Server certificate is not trusted by Java. Add it to the truststore. |
| `chain_not_registered`, `client_not_bound`, `producer_not_allowed` | Lock setting missing. See [Lock Server](#lock-server). |
| Collect `PARTIAL` | Lock has not stored all records yet. The job tries again at its next run, and after `facade.job.max.attempts` accepts what Lock has. |
| Train `FAILED`: `too little trace data to train` | The traces cover too short a period. Raise `train_executions` and `test_executions`. |
| Training skipped | `facade.dir` is empty or `facade.run` is `false`. |
| Anomalies rank low, scores all alike | The model did not learn. See [How to tell if a run went wrong](#how-to-tell-if-a-run-went-wrong). |
| A job says "gave up after 3 attempts" | It stops until there is new data. Fix the cause, then send again by changing `authorization.json`, or remove that job's section from `state.json`. |

## Reference

### Conversion rules

- **Principal:** `trace.subject.workload_id`. If it is missing, the converter follows the `authorized_by`, `authorized` and `produced_effect` parents, then uses `trace.execution_authority`.
- **Action:** one per decision, with id `<producer_id>/<record_id>:<index>`, resource `<action>|<resource_type>`, and time `signed_at`. By default only `AUTHORIZATION_DECISION` records with outcome `ALLOW` or `SUCCESS`.
- **Context:** one per principal and execution, with the peer attribute `execution = <execution_authority>|<trace_execution_id>`.
- **Dropped:** records Lock marked `signature_valid=false` or `key_authorized=false`, `VOID` receipts, and duplicate `(producer, record_id)`.

### Limits

- **Time cannot be backdated.** Cedarling signs each record with the current time, so one send spans minutes, not days.
- **One chain.** One Cedarling instance writes all records of a send, so they share one producer chain.
- **Synthetic data.** The records come from generated requests, not from your real traffic.

### Install

Build the plugin (it needs JDK 17 or later, and sets `maven.compiler.release` to 17 in its `pom.xml`) and copy `target/facade-plugin-<version>-distribution.jar` to `/opt/jans/jetty/jans-config-api/custom/libs`, then add it to the Config API extra classpath like the other plugins. Restart Config API.
