// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Signs the drafts the authorize path hands over and sends them to Lock.
//!
//! Batches form on their own: the emitter takes what the channel holds, up to
//! the bulk limits, signs it and sends it at once, and while a request is in
//! flight the next batch gathers in the channel. Only what goes into a request
//! is signed, so at most one batch, plus records waiting for a resend, is signed
//! but not yet accepted. While Lock is down the channel fills up and the
//! authorize path stops handing records over, so the chain gets no gap.

use std::borrow::Borrow;
use std::collections::{HashMap, VecDeque};
use std::num::NonZeroUsize;
use std::sync::{Arc, Mutex, PoisonError};

use bytes::Bytes;
use jans_trace_core::Limits;
use jans_trace_core::assertion::{SignedAssertion, batch_body};
use jans_trace_core::signature::TraceSigner;
use tokio::sync::mpsc;
use tokio::task::JoinError;
use tokio_util::sync::CancellationToken;
use uuid7::Uuid;

use super::ProducerId;
use super::chain::{AppendError, ProducerChain};
use super::record::RecordDraft;
use super::status::EmitterEvent;
use crate::authz::metrics::MetricsCollector;
use crate::bootstrap_config::TraceConfig;
use crate::common::app_types::PdpID;
use crate::http::{JoinHandle, spawn_task};
use crate::http_utils::Backoff;
use crate::lock::{BulkItemResult, BulkSink, PostBulkError, WORKER_HTTP_RETRY_DUR};
use crate::log::interface::LogWriter;
use crate::log::{BaseLogEntry, LogEntry, LogLevel, Logger};

/// Lock error ids of a record Lock failed to store: the same bytes may succeed later.
const TRANSIENT_ERRORS: [&str; 2] = ["storage_failure", "crypto_unavailable"];
/// Lock error id for a record of a chain whose genesis record Lock has not accepted.
const CHAIN_NOT_REGISTERED: &str = "chain_not_registered";

/// What the emitter needs; the producer identity is the one every record is signed with.
pub(crate) struct EmitterParams {
    pub(crate) producer_id: ProducerId,
    pub(crate) pdp_id: PdpID,
    pub(crate) signer: Arc<dyn TraceSigner>,
    pub(crate) sink: Arc<dyn BulkSink>,
    pub(crate) bulk_max_records: NonZeroUsize,
    pub(crate) bulk_max_bytes: NonZeroUsize,
    /// Retries of one request, and resends of one record, before it is given up.
    pub(crate) max_retries: u32,
    pub(crate) log: Logger,
    pub(crate) metrics: Arc<MetricsCollector>,
}

/// Why the emitter did not start.
#[derive(Debug, Clone, PartialEq, Eq, thiserror::Error)]
pub enum EmitterInitError {
    /// Checked here as well as when the configuration is read, so a
    /// `BootstrapConfig` built directly cannot skip it.
    #[error(
        "`CEDARLING_TRACE_BULK_MAX_BYTES` must be at least {min}: a smaller bulk request \
         cannot carry the largest record Lock accepts"
    )]
    BulkMaxBytesTooSmall {
        /// The smallest limit that carries the largest record.
        min: usize,
    },
}

/// The running emitter. Dropping it stops the emitter, which still sends what
/// it holds if the runtime lives on; [`Self::shut_down`] waits for that.
pub(crate) struct EmitterHandle {
    cancel: CancellationToken,
    join: Mutex<Option<JoinHandle<()>>>,
}

impl EmitterHandle {
    /// Stops the emitter once it has sent the records it holds and the ones
    /// still in the channel. Takes `&self` so it works through the `Arc` that
    /// clones of `Cedarling` share; a second call returns at once.
    pub(crate) async fn shut_down(&self) {
        self.cancel.cancel();
        let join = self
            .join
            .lock()
            .unwrap_or_else(PoisonError::into_inner)
            .take();
        if let Some(join) = join {
            join.await_result().await;
        }
    }
}

impl Drop for EmitterHandle {
    fn drop(&mut self) {
        self.cancel.cancel();
    }
}

/// Starts the emitter on the drafts that arrive through `rx`.
pub(crate) fn spawn_emitter(
    params: EmitterParams,
    rx: mpsc::Receiver<RecordDraft>,
) -> Result<EmitterHandle, EmitterInitError> {
    let min = TraceConfig::MIN_BULK_MAX_BYTES;
    if params.bulk_max_bytes.get() < min {
        return Err(EmitterInitError::BulkMaxBytesTooSmall { min });
    }
    let cancel = CancellationToken::new();
    let chain = ProducerChain::open(params.producer_id.clone(), params.pdp_id);
    let emitter = Emitter::new(params, &chain);
    let join = spawn_task(emitter.run(chain, rx, cancel.clone()));
    Ok(EmitterHandle {
        cancel,
        join: Mutex::new(Some(join)),
    })
}

/// A signed record waiting to be sent, or sent again.
struct Pending {
    assertion: SignedAssertion,
    /// `request_id` of the decision, so the record's logs sit with it.
    record_id: Uuid,
    chain_id: Uuid,
    sequence: u64,
    /// Sends Lock answered with an error that allows a resend.
    attempts: u32,
}

impl Borrow<SignedAssertion> for Pending {
    fn borrow(&self) -> &SignedAssertion {
        &self.assertion
    }
}

/// What Lock did with the genesis record of a chain. Lock registers a chain
/// from that record and refuses the chain's other records with
/// `chain_not_registered` until it does.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
enum GenesisState {
    /// Not accepted yet: sent, waiting to be sent, or waiting for a resend.
    Pending,
    /// Accepted, so Lock has registered the chain.
    Accepted,
    /// Lost for good, so Lock will never register the chain: it is replaced
    /// before more records are signed into it.
    Lost,
    /// Accepted, but Lock later refused a record with `chain_not_registered`:
    /// it no longer knows the chain (a restored database, the client moved to
    /// another evidence domain), so the chain is replaced like a lost one.
    Unregistered,
}

/// The next bulk request: its body and the records joined into it, in order.
struct BulkRequest {
    body: Bytes,
    records: Vec<Pending>,
}

/// How a round treats a request or record that failed for a reason that may pass.
#[derive(Clone, Copy)]
enum Mode<'a> {
    /// Retries up to `max_retries`; the token cuts waits short.
    Running(&'a CancellationToken),
    /// Shutting down: each batch goes once, without retries or pauses, so
    /// stopping while Lock is down does not take as long as the retries.
    Stopping,
}

/// How a bulk request ended.
enum Sent {
    /// Lock answered for each record.
    Results(Vec<BulkItemResult>),
    /// Every record of the request is lost, already counted and logged.
    Lost,
    /// Stopping was requested while waiting to retry; the records stay.
    Interrupted,
}

struct Emitter {
    producer_id: ProducerId,
    pdp_id: PdpID,
    signer: Arc<dyn TraceSigner>,
    sink: Arc<dyn BulkSink>,
    max_records: usize,
    max_bytes: usize,
    max_retries: u32,
    log: Logger,
    metrics: Arc<MetricsCollector>,
    /// Lock's record limits for signing, with the configured bulk bounds.
    limits: Limits,
    /// Id of the chain new records are signed into.
    chain_id: Uuid,
    /// What Lock did with the genesis record of the current chain.
    genesis: GenesisState,
    /// Signed records in chain order.
    pending: VecDeque<Pending>,
}

impl Emitter {
    /// An emitter that starts signing into `chain`.
    fn new(params: EmitterParams, chain: &ProducerChain) -> Self {
        let limits = Limits::default()
            .with_max_batch_records(params.bulk_max_records.get())
            .with_max_batch_bytes(params.bulk_max_bytes.get());
        let emitter = Self {
            producer_id: params.producer_id,
            pdp_id: params.pdp_id,
            signer: params.signer,
            sink: params.sink,
            max_records: params.bulk_max_records.get(),
            max_bytes: params.bulk_max_bytes.get(),
            max_retries: params.max_retries,
            log: params.log,
            metrics: params.metrics,
            limits,
            chain_id: chain.chain_id(),
            genesis: GenesisState::Pending,
            pending: VecDeque::new(),
        };
        emitter.chain_opened();
        emitter
    }

    async fn run(
        mut self,
        mut chain: ProducerChain,
        mut rx: mpsc::Receiver<RecordDraft>,
        cancel: CancellationToken,
    ) {
        while !cancel.is_cancelled() {
            let mut drafts = Vec::new();
            if self.pending.is_empty() {
                tokio::select! {
                    biased;
                    () = cancel.cancelled() => break,
                    draft = rx.recv() => match draft {
                        Some(draft) => drafts.push(draft),
                        // Every recorder is gone, so nothing more will arrive.
                        None => break,
                    },
                }
            }
            while drafts.len() < self.room() {
                let Ok(draft) = rx.try_recv() else { break };
                drafts.push(draft);
            }
            chain = self.round(chain, drafts, Mode::Running(&cancel)).await;
        }

        // Stopping: send everything once; what Lock does not take is dropped.
        rx.close();
        let mut rest = Vec::new();
        while let Ok(draft) = rx.try_recv() {
            rest.push(draft);
        }
        let mut rest = rest.into_iter();
        while !self.pending.is_empty() || rest.len() > 0 {
            let drafts = rest.by_ref().take(self.room()).collect();
            chain = self.round(chain, drafts, Mode::Stopping).await;
        }
    }

    /// Drafts the next request still has room for.
    fn room(&self) -> usize {
        self.max_records.saturating_sub(self.pending.len())
    }

    /// Signs `drafts`, sends one request and applies Lock's answer.
    async fn round(
        &mut self,
        chain: ProducerChain,
        drafts: Vec<RecordDraft>,
        mode: Mode<'_>,
    ) -> ProducerChain {
        let mut chain = self.sign(chain, drafts).await;
        let mut resend = false;
        if let Some(request) = self.next_request() {
            match self.send(&request.body, request.records.len(), mode).await {
                Sent::Results(results) => resend = self.settle(request.records, results, mode),
                Sent::Lost => request
                    .records
                    .iter()
                    .for_each(|record| self.forget(record)),
                Sent::Interrupted => self.put_back(request.records),
            }
        }
        if matches!(
            self.genesis,
            GenesisState::Lost | GenesisState::Unregistered
        ) {
            chain = self.replace_chain();
        }
        if resend {
            self.pause(mode).await;
        }
        chain
    }

    async fn sign(&mut self, chain: ProducerChain, drafts: Vec<RecordDraft>) -> ProducerChain {
        if drafts.is_empty() {
            return chain;
        }
        let chain_id = chain.chain_id();
        let first = chain.next_sequence();
        let record_ids: Vec<Uuid> = drafts.iter().map(|draft| draft.record_id).collect();
        let signed =
            sign_batch_offloaded(chain, drafts, Arc::clone(&self.signer), self.limits).await;
        let SignedBatch { chain, results } = match signed {
            Ok(signed) => signed,
            Err(err) => {
                self.log(
                    LogLevel::ERROR,
                    None,
                    format!(
                        "{err}; its {} TRACE records and the producer chain {chain_id} are lost",
                        record_ids.len()
                    ),
                );
                return self.replace_chain();
            },
        };
        let mut sequence = first;
        for (record_id, result) in record_ids.into_iter().zip(results) {
            match result {
                Ok(assertion) => {
                    self.pending.push_back(Pending {
                        assertion,
                        record_id,
                        chain_id,
                        sequence,
                        attempts: 0,
                    });
                    sequence += 1;
                },
                Err(err) => {
                    self.log(
                        LogLevel::WARN,
                        Some(record_id),
                        format!("TRACE record not signed: {err}"),
                    );
                    self.count(EmitterEvent::SignFailed, 1);
                },
            }
        }
        chain
    }

    /// Takes the records of the next request from the front of `pending`, as
    /// many as fit the bulk limits, and joins them into its body.
    fn next_request(&mut self) -> Option<BulkRequest> {
        let mut batch = Vec::new();
        // `[` and `]`, then each record and the comma before all but the first.
        let mut bytes = 2;
        while batch.len() < self.max_records
            && let Some(next) = self.pending.front()
        {
            let added = next.assertion.as_str().len() + usize::from(!batch.is_empty());
            if bytes + added > self.max_bytes {
                break;
            }
            bytes += added;
            batch.extend(self.pending.pop_front());
        }
        if batch.is_empty() {
            // The bulk limit leaves room for the largest record Lock takes, so
            // this is a record Lock would refuse anyway; it cannot block the rest.
            let too_large = self.pending.pop_front()?;
            self.log(
                LogLevel::ERROR,
                Some(too_large.record_id),
                format!(
                    "TRACE record of {} bytes does not fit a bulk request of \
                     `CEDARLING_TRACE_BULK_MAX_BYTES` {}; it is lost",
                    too_large.assertion.as_str().len(),
                    self.max_bytes
                ),
            );
            self.lose(&[too_large]);
            return None;
        }
        match batch_body(&batch, &self.limits) {
            Ok(body) => Some(BulkRequest {
                body: Bytes::from(body),
                records: batch,
            }),
            Err(err) => {
                self.log(
                    LogLevel::ERROR,
                    None,
                    format!(
                        "TRACE bulk body not built, its {} records are lost: {err}",
                        batch.len()
                    ),
                );
                self.lose(&batch);
                None
            },
        }
    }

    /// Posts `body`, retrying a failed request with the same bytes.
    async fn send(&mut self, body: &Bytes, records: usize, mode: Mode<'_>) -> Sent {
        let mut backoff = Backoff::new_exponential(WORKER_HTTP_RETRY_DUR, Some(self.max_retries));
        loop {
            let err = match self.sink.post_bulk(body).await {
                Ok(results) => return Sent::Results(results),
                Err(err @ PostBulkError::Rejected { .. }) => {
                    self.log(
                        LogLevel::ERROR,
                        None,
                        format!("{err}; its {records} TRACE records are lost"),
                    );
                    self.count(EmitterEvent::Rejected, records);
                    return Sent::Lost;
                },
                Err(err @ PostBulkError::Retryable(_)) => err,
            };
            let retry = match mode {
                Mode::Running(cancel) => tokio::select! {
                    retry = backoff.snooze() => retry.is_ok(),
                    () = cancel.cancelled() => return Sent::Interrupted,
                },
                Mode::Stopping => {
                    self.log(
                        LogLevel::WARN,
                        None,
                        format!("{err}; Cedarling is shutting down, its {records} TRACE records are dropped"),
                    );
                    self.count(EmitterEvent::Dropped, records);
                    return Sent::Lost;
                },
            };
            if !retry {
                self.log(
                    LogLevel::WARN,
                    None,
                    format!("{err}; retries exhausted, its {records} TRACE records are dropped"),
                );
                self.count(EmitterEvent::Dropped, records);
                return Sent::Lost;
            }
        }
    }

    /// Applies Lock's answer for each record of `batch`. Returns whether a
    /// record goes back for a resend.
    fn settle(
        &mut self,
        batch: Vec<Pending>,
        results: Vec<BulkItemResult>,
        mode: Mode<'_>,
    ) -> bool {
        let mut results: HashMap<usize, BulkItemResult> = results
            .into_iter()
            .map(|result| (result.index, result))
            .collect();
        let mut resend = Vec::new();
        for (index, mut record) in batch.into_iter().enumerate() {
            let Some(result) = results.remove(&index) else {
                self.log(
                    LogLevel::ERROR,
                    Some(record.record_id),
                    "Lock Server returned no result for the TRACE record; it is lost".to_string(),
                );
                self.lose(&[record]);
                continue;
            };
            if result.accepted {
                self.accepted(&record, &result.flags);
                continue;
            }
            let error = result.error.as_deref().unwrap_or("none");
            let unregistered = error == CHAIN_NOT_REGISTERED;
            let resendable =
                TRANSIENT_ERRORS.contains(&error) || (unregistered && self.awaits_genesis(&record));
            if !resendable {
                // Lock opens a chain from its genesis record only in this mode.
                let hint = if unregistered && self.is_current_genesis(&record) {
                    "; Lock Server needs `chainRegistrationMode: FIRST_OBSERVED`"
                } else {
                    ""
                };
                self.log(
                    LogLevel::ERROR,
                    Some(record.record_id),
                    format!(
                        "Lock Server rejected the TRACE record (error: {error}, reason: {}){hint}",
                        result.reason.as_deref().unwrap_or("none")
                    ),
                );
                self.count(EmitterEvent::Rejected, 1);
                if unregistered {
                    self.unregistered(&record);
                }
                self.forget(&record);
                continue;
            }
            record.attempts += 1;
            if matches!(mode, Mode::Stopping) {
                self.log(
                    LogLevel::WARN,
                    Some(record.record_id),
                    format!("TRACE record dropped at shutdown (error: {error})"),
                );
                self.lose(&[record]);
            } else if record.attempts > self.max_retries {
                self.log(
                    LogLevel::WARN,
                    Some(record.record_id),
                    format!(
                        "TRACE record dropped after {} resends (error: {error})",
                        self.max_retries
                    ),
                );
                self.lose(&[record]);
            } else {
                resend.push(record);
            }
        }
        let any = !resend.is_empty();
        // Resent records come before the rest, which was signed after them.
        for record in resend.into_iter().rev() {
            self.pending.push_front(record);
        }
        any
    }

    fn accepted(&mut self, record: &Pending, flags: &[&str]) {
        self.count(EmitterEvent::Accepted, 1);
        if self.is_current_genesis(record) {
            self.genesis = GenesisState::Accepted;
        }
        self.log(
            LogLevel::DEBUG,
            Some(record.record_id),
            format!(
                "Lock Server accepted the TRACE record: sequence_number {}, content_digest {}",
                record.sequence,
                record.assertion.content_digest()
            ),
        );
        if !flags.is_empty() {
            self.log(
                LogLevel::WARN,
                Some(record.record_id),
                format!("Lock Server flagged the TRACE record: {}", flags.join(", ")),
            );
        }
    }

    /// Whether `record` follows the genesis record of the current chain, which
    /// Lock has not registered yet. Lock refusing the genesis record itself
    /// with `chain_not_registered` never registers the chain.
    fn awaits_genesis(&self, record: &Pending) -> bool {
        record.chain_id == self.chain_id
            && self.genesis != GenesisState::Accepted
            && !self.is_current_genesis(record)
    }

    /// Notes a `chain_not_registered` refusal of a record of the current chain
    /// after Lock had registered it.
    fn unregistered(&mut self, record: &Pending) {
        if record.chain_id == self.chain_id && self.genesis == GenesisState::Accepted {
            self.genesis = GenesisState::Unregistered;
        }
    }

    fn is_current_genesis(&self, record: &Pending) -> bool {
        record.chain_id == self.chain_id && record.sequence == 1
    }

    /// Counts records given up without an answer from Lock.
    fn lose(&mut self, records: &[Pending]) {
        self.count(EmitterEvent::Dropped, records.len());
        for record in records {
            self.forget(record);
        }
    }

    /// Notes a record that will never be accepted.
    fn forget(&mut self, record: &Pending) {
        if self.is_current_genesis(record) {
            self.genesis = GenesisState::Lost;
        }
    }

    fn put_back(&mut self, batch: Vec<Pending>) {
        for record in batch.into_iter().rev() {
            self.pending.push_front(record);
        }
    }

    /// Opens a chain in place of the current one, which Lock does not know:
    /// it would refuse every other record of it. Those records are dropped
    /// rather than signed again.
    fn replace_chain(&mut self) -> ProducerChain {
        let lost = self.chain_id;
        let before = self.pending.len();
        self.pending.retain(|record| record.chain_id != lost);
        let dropped = before - self.pending.len();
        let cause = match self.genesis {
            GenesisState::Unregistered => "is no longer registered in Lock Server",
            _ => "lost its genesis record",
        };
        self.log(
            LogLevel::WARN,
            None,
            format!(
                "TRACE producer chain {lost} {cause}; {dropped} more of its records are \
                 dropped and a new chain is opened"
            ),
        );
        self.count(EmitterEvent::Dropped, dropped);
        self.count(EmitterEvent::ChainOpened, 1);
        self.open_chain()
    }

    fn open_chain(&mut self) -> ProducerChain {
        let chain = ProducerChain::open(self.producer_id.clone(), self.pdp_id);
        self.chain_id = chain.chain_id();
        self.genesis = GenesisState::Pending;
        self.chain_opened();
        chain
    }

    /// Admins and tests find the chain's records in Lock by this id.
    fn chain_opened(&self) {
        self.log(
            LogLevel::INFO,
            None,
            format!(
                "TRACE producer chain opened: producer_chain_id {}",
                self.chain_id
            ),
        );
    }

    /// Waits before resending records Lock failed to store.
    async fn pause(&self, mode: Mode<'_>) {
        if let Mode::Running(cancel) = mode {
            tokio::select! {
                () = tokio::time::sleep(WORKER_HTTP_RETRY_DUR) => {},
                () = cancel.cancelled() => {},
            }
        }
    }

    fn count(&self, event: EmitterEvent, records: usize) {
        if records > 0 {
            self.metrics.record_trace_emitter(event, records);
        }
    }

    fn log(&self, level: LogLevel, record_id: Option<Uuid>, message: String) {
        self.log.log_any(
            LogEntry::new(BaseLogEntry::new_system_opt_request_id(level, record_id))
                .set_message(message),
        );
    }
}

/// What a signing task hands back: the chain, moved past every signed record,
/// and one result per draft, in draft order.
struct SignedBatch {
    chain: ProducerChain,
    results: Vec<Result<SignedAssertion, AppendError>>,
}

/// The signing task panicked, and the chain it held is gone with it.
#[derive(Debug, thiserror::Error)]
#[error("TRACE signing task failed: {0}")]
struct SignTaskPanicked(JoinError);

/// Signs `drafts` in order on `chain`, off the async runtime: a batch of a
/// hundred records is a few milliseconds of CPU, which would stall the
/// authorization requests sharing the runtime. The chain moves into the task
/// and comes back.
///
/// Native only, as TRACE is off on WASM. When it comes there, a `cfg(wasm32)`
/// branch signs inline and calls `tokio::task::yield_now()` every n records,
/// counted rather than timed, as `std::time::Instant` is not available on
/// `wasm32-unknown-unknown`.
async fn sign_batch_offloaded(
    mut chain: ProducerChain,
    drafts: Vec<RecordDraft>,
    signer: Arc<dyn TraceSigner>,
    limits: Limits,
) -> Result<SignedBatch, SignTaskPanicked> {
    tokio::task::spawn_blocking(move || {
        let results = drafts
            .iter()
            .map(|draft| chain.append(draft, signer.as_ref(), &limits))
            .collect();
        SignedBatch { chain, results }
    })
    .await
    .map_err(SignTaskPanicked)
}

#[cfg(test)]
mod tests {
    use std::collections::{HashSet, VecDeque};
    use std::num::NonZeroUsize;
    use std::sync::{Arc, Mutex};
    use std::thread::ThreadId;
    use std::time::Duration;

    use async_trait::async_trait;
    use jans_trace_core::assertion::{SignedAssertion, VerificationContext, parse_batch};
    use jans_trace_core::inputs::compute_bundle_hash;
    use jans_trace_core::signature::{Ed25519Signer, SignerError, TraceSigner};
    use jans_trace_core::{Digest, Limits};
    use serde_json::Value;
    use tokio::sync::{mpsc, watch};

    use super::*;
    use crate::authz::TraceContext;
    use crate::authz::metrics::{MetricsCollector, MetricsMode};
    use crate::bootstrap_config::TraceConfig;
    use crate::common::app_types::PdpID;
    use crate::common::policy_store::test_utils::fixtures;
    use crate::lock::{BulkItemResult, BulkSink, PostBulkError};
    use crate::log::{TEST_LOGGER, gen_uuid7};
    use crate::trace::record::{DecisionFacts, Outcome, PolicyFacts, RecordDraft, TokenRef};
    use crate::trace::test_utils::{CjarStore, cjar_store};
    use crate::trace::{ProducerKey, TraceRecordStatus, TraceRecorder};

    /// RFC 8410 section 10.3 example key: public test vector, not a secret.
    const KEY_PEM: &str = include_str!("../../../test_files/trace/producer_ed25519.pem");
    const PRODUCER: &str = "cedarling-fleet-1";
    const KID: &str = "fleet-key-1";

    /// How the fake Lock answers one bulk request.
    enum Reply {
        AcceptAll,
        Retryable,
        /// The whole request refused, as Lock does with `403`.
        Rejected,
        /// Per record: `None` accepted, `Some(id)` rejected with that error id.
        PerRecord(Vec<Option<&'static str>>),
    }

    /// Answers each request with the next scripted reply, then accepts all.
    struct FakeLock {
        replies: Mutex<VecDeque<Reply>>,
        bodies: watch::Sender<Vec<String>>,
    }

    impl FakeLock {
        fn new(replies: impl IntoIterator<Item = Reply>) -> Arc<Self> {
            Arc::new(Self {
                replies: Mutex::new(replies.into_iter().collect()),
                bodies: watch::Sender::new(Vec::new()),
            })
        }

        /// Request bodies received so far, once `done` holds for them.
        async fn bodies_when(&self, done: impl Fn(&[String]) -> bool) -> Vec<String> {
            let mut rx = self.bodies.subscribe();
            let wait = rx.wait_for(|bodies| done(bodies));
            // Longer than any backoff here, under paused time as in real time.
            tokio::time::timeout(Duration::from_secs(60), wait)
                .await
                .expect("the emitter should send the expected requests")
                .expect("the fake outlives the wait")
                .clone()
        }

        async fn bodies(&self, count: usize) -> Vec<String> {
            self.bodies_when(|bodies| bodies.len() >= count).await
        }
    }

    #[async_trait]
    impl BulkSink for FakeLock {
        async fn post_bulk(&self, body: &Bytes) -> Result<Vec<BulkItemResult>, PostBulkError> {
            let body = std::str::from_utf8(body).expect("a bulk body is UTF-8");
            let count = records(body).len();
            self.bodies
                .send_modify(|bodies| bodies.push(body.to_string()));
            let reply = self
                .replies
                .lock()
                .expect("fake lock is not poisoned")
                .pop_front()
                .unwrap_or(Reply::AcceptAll);
            let item = |index, error: Option<&str>| BulkItemResult {
                index,
                accepted: error.is_none(),
                error: error.map(str::to_string),
                reason: None,
                flags: Vec::new(),
            };
            match reply {
                Reply::AcceptAll => Ok((0..count).map(|index| item(index, None)).collect()),
                Reply::Retryable => Err(PostBulkError::Retryable("Lock is down".to_string())),
                Reply::Rejected => Err(PostBulkError::Rejected {
                    status: 403,
                    error: Some("access_denied".to_string()),
                    reason: None,
                }),
                Reply::PerRecord(errors) => Ok(errors
                    .into_iter()
                    .enumerate()
                    .map(|(index, error)| item(index, error))
                    .collect()),
            }
        }
    }

    /// Signs with the test key and remembers the thread of every signature.
    struct ThreadRecordingSigner {
        inner: Ed25519Signer,
        threads: Mutex<Vec<ThreadId>>,
    }

    impl TraceSigner for ThreadRecordingSigner {
        fn kid(&self) -> &str {
            self.inner.kid()
        }

        fn sign(&self, message: &[u8]) -> Result<[u8; 64], SignerError> {
            self.threads
                .lock()
                .expect("thread list is not poisoned")
                .push(std::thread::current().id());
            self.inner.sign(message)
        }
    }

    fn test_signer() -> Ed25519Signer {
        Ed25519Signer::from_pkcs8_pem(KID, KEY_PEM).expect("test key should parse")
    }

    fn producer_key() -> ProducerKey {
        let kid = KID.parse().expect("test kid should parse");
        ProducerKey::from_pem(KEY_PEM, Some(&kid)).expect("test key should parse")
    }

    fn verification() -> VerificationContext {
        VerificationContext {
            expected_producer: PRODUCER.to_string(),
            expected_kid: KID.to_string(),
            public_key: test_signer().public_key(),
        }
    }

    fn metrics() -> Arc<MetricsCollector> {
        Arc::new(MetricsCollector::new(MetricsMode::Local))
    }

    fn params(lock: Arc<FakeLock>, metrics: Arc<MetricsCollector>) -> EmitterParams {
        EmitterParams {
            producer_id: PRODUCER.parse().expect("test producer id should parse"),
            pdp_id: PdpID::new(),
            signer: Arc::new(producer_key()),
            sink: lock,
            bulk_max_records: NonZeroUsize::new(100).expect("non-zero"),
            bulk_max_bytes: NonZeroUsize::new(4_194_304).expect("non-zero"),
            max_retries: 3,
            log: TEST_LOGGER.clone(),
            metrics,
        }
    }

    fn draft_with(tokens: Vec<TokenRef>) -> RecordDraft {
        let trace = TraceContext::new(
            "exec-1".to_string(),
            "https://gateway.example.com".to_string(),
            "workload-1".to_string(),
        );
        let decision = DecisionFacts {
            action: "Jans::Action::\"Read\"".to_string(),
            resource_type: "Jans::Issue".to_string(),
            outcome: Outcome::Allow,
            tokens: tokens.into(),
        };
        let policy = Arc::new(PolicyFacts {
            store_id: "store-1".to_string(),
            store_version: "1.0.0".to_string(),
            language_version: "4.4".to_string(),
            bundle_hash: compute_bundle_hash(b"archive"),
        });
        RecordDraft::new(
            gen_uuid7(),
            1_781_138_542,
            &Arc::new(trace),
            decision,
            policy,
        )
        .expect("test draft builds")
    }

    fn draft() -> RecordDraft {
        draft_with(Vec::new())
    }

    /// As many tokens with the longest `jti` as Lock takes, so a few records
    /// fill a bulk request.
    fn large_draft() -> RecordDraft {
        let tokens = (0..32)
            .map(|index| {
                let jti = format!("{index:0>256}");
                TokenRef::new(
                    "https://idp.example.com",
                    "Jans::Access_Token",
                    Some(&jti),
                    "raw",
                )
                .expect("test token ref should build")
            })
            .collect();
        draft_with(tokens)
    }

    /// A running emitter and the sender that feeds it; holding the sender keeps
    /// the emitter waiting for more drafts.
    struct Running {
        tx: mpsc::Sender<RecordDraft>,
        handle: EmitterHandle,
    }

    /// An emitter fed by a channel of `capacity` that already holds `drafts`.
    fn start(
        params: EmitterParams,
        capacity: usize,
        drafts: impl IntoIterator<Item = RecordDraft>,
    ) -> Running {
        let (tx, rx) = mpsc::channel(capacity);
        for draft in drafts {
            tx.try_send(draft).expect("the channel has room");
        }
        let handle = spawn_emitter(params, rx).expect("the emitter should start");
        Running { tx, handle }
    }

    fn records(body: &str) -> Vec<SignedAssertion> {
        parse_batch(body, &Limits::default())
            .expect("the body is a bulk array")
            .into_iter()
            .map(|record| record.expect("each record parses"))
            .collect()
    }

    /// Where a record sits in its producer chain.
    struct ChainPosition {
        chain_id: String,
        sequence: u64,
        prev_record_hash: String,
    }

    fn position(record: &SignedAssertion) -> ChainPosition {
        let json: Value = serde_json::from_str(record.as_str()).expect("a record is JSON");
        let chain = &json["producer_chain"];
        ChainPosition {
            chain_id: chain["producer_chain_id"]
                .as_str()
                .expect("chain id")
                .to_string(),
            sequence: chain["sequence_number"].as_u64().expect("sequence number"),
            prev_record_hash: chain["prev_record_hash"]
                .as_str()
                .expect("prev hash")
                .to_string(),
        }
    }

    fn counter(metrics: &MetricsCollector, key: &str) -> Option<i64> {
        metrics
            .snapshot_and_reset()
            .operational_stats
            .get(key)
            .copied()
    }

    #[tokio::test]
    async fn drafts_form_one_chain() {
        let lock = FakeLock::new([]);
        let emitter = start(
            params(lock.clone(), metrics()),
            4,
            [draft(), draft(), draft()],
        );

        let bodies = lock.bodies(1).await;
        let records = records(&bodies[0]);
        assert_eq!(records.len(), 3, "the three drafts go in one request");
        let chain_id = position(&records[0]).chain_id;
        let mut prev = Digest::GENESIS.to_string();
        for (expected, record) in (1..).zip(&records) {
            record
                .verify(&verification())
                .expect("each record verifies with the producer key");
            let position = position(record);
            assert_eq!(position.chain_id, chain_id, "all records are in one chain");
            assert_eq!(
                position.sequence, expected,
                "sequence numbers follow the draft order"
            );
            assert_eq!(
                position.prev_record_hash, prev,
                "each record links to the one before"
            );
            prev = record.content_digest().to_string();
        }
        emitter.handle.shut_down().await;
    }

    #[tokio::test]
    async fn batch_respects_max_records() {
        let lock = FakeLock::new([]);
        let mut params = params(lock.clone(), metrics());
        params.bulk_max_records = NonZeroUsize::new(2).expect("non-zero");
        let emitter = start(params, 5, std::iter::repeat_with(draft).take(5));

        let sizes: Vec<usize> = lock
            .bodies(3)
            .await
            .iter()
            .map(|body| records(body).len())
            .collect();
        assert_eq!(sizes, [2, 2, 1], "no request carries more than two records");
        emitter.handle.shut_down().await;
    }

    #[tokio::test]
    async fn batch_respects_max_bytes() {
        let lock = FakeLock::new([]);
        let mut params = params(lock.clone(), metrics());
        let max_bytes = TraceConfig::MIN_BULK_MAX_BYTES;
        params.bulk_max_bytes = NonZeroUsize::new(max_bytes).expect("non-zero");
        let drafts: Vec<_> = std::iter::repeat_with(large_draft).take(30).collect();
        let record_ids: HashSet<String> = drafts
            .iter()
            .map(|draft| draft.record_id.to_string())
            .collect();
        let emitter = start(params, 30, drafts);

        let bodies = lock
            .bodies_when(|bodies| {
                bodies.iter().map(|body| records(body).len()).sum::<usize>() >= 30
            })
            .await;
        assert!(bodies.len() > 1, "30 large records do not fit one request");
        for body in &bodies {
            assert!(
                body.len() <= max_bytes,
                "a body of {} bytes is over the limit of {max_bytes}",
                body.len()
            );
        }
        let sent: HashSet<String> = bodies
            .iter()
            .flat_map(|body| records(body))
            .map(|record| record.identity().record_id().to_string())
            .collect();
        assert_eq!(sent, record_ids, "every record is sent once");
        emitter.handle.shut_down().await;
    }

    #[tokio::test(start_paused = true)]
    async fn retryable_request_resends_same_bytes() {
        let lock = FakeLock::new([Reply::Retryable]);
        let emitter = start(params(lock.clone(), metrics()), 4, [draft(), draft()]);

        let bodies = lock.bodies(2).await;
        assert_eq!(bodies[1], bodies[0], "the retry sends the same bytes");
        emitter.handle.shut_down().await;
    }

    #[tokio::test(start_paused = true)]
    async fn record_storage_failure_resends_that_record_only() {
        let lock = FakeLock::new([Reply::PerRecord(vec![None, Some("storage_failure")])]);
        let emitter = start(params(lock.clone(), metrics()), 4, [draft(), draft()]);

        let bodies = lock.bodies(2).await;
        let first = records(&bodies[0]);
        let second = records(&bodies[1]);
        assert_eq!(second.len(), 1, "only the failed record is sent again");
        assert_eq!(
            second[0].as_str(),
            first[1].as_str(),
            "the failed record goes again with the same bytes"
        );
        emitter.handle.shut_down().await;
    }

    #[tokio::test(start_paused = true)]
    async fn rejected_record_is_not_resent() {
        let lock = FakeLock::new([Reply::PerRecord(vec![None, Some("invalid_signature")])]);
        let metrics = metrics();
        let emitter = start(params(lock.clone(), metrics.clone()), 4, [draft(), draft()]);

        let rejected = records(&lock.bodies(1).await[0])[1].as_str().to_string();
        emitter
            .tx
            .send(draft())
            .await
            .expect("the emitter is running");
        let bodies = lock.bodies(2).await;
        let second = records(&bodies[1]);
        assert_eq!(second.len(), 1, "only the new record is sent");
        assert_ne!(
            second[0].as_str(),
            rejected,
            "a rejected record is not sent again"
        );
        assert_eq!(
            counter(&metrics, "trace.rejected"),
            Some(1),
            "the rejected record is counted"
        );
        emitter.handle.shut_down().await;
    }

    #[tokio::test(start_paused = true)]
    async fn genesis_transient_failure_keeps_tail() {
        let lock = FakeLock::new([Reply::PerRecord(vec![
            Some("storage_failure"),
            Some("chain_not_registered"),
            Some("chain_not_registered"),
        ])]);
        let emitter = start(
            params(lock.clone(), metrics()),
            4,
            [draft(), draft(), draft()],
        );

        let bodies = lock.bodies(2).await;
        assert_eq!(
            bodies[1], bodies[0],
            "the genesis record and the tail go again in order, with the same bytes"
        );
        emitter.handle.shut_down().await;
    }

    #[tokio::test(start_paused = true)]
    async fn genesis_lost_opens_new_chain() {
        let lock = FakeLock::new([Reply::PerRecord(vec![
            Some("invalid_signature"),
            Some("chain_not_registered"),
            Some("chain_not_registered"),
        ])]);
        let metrics = metrics();
        let emitter = start(
            params(lock.clone(), metrics.clone()),
            4,
            [draft(), draft(), draft()],
        );

        let old_chain = position(&records(&lock.bodies(1).await[0])[0]).chain_id;
        emitter
            .tx
            .send(draft())
            .await
            .expect("the emitter is running");
        let bodies = lock.bodies(2).await;
        let second = records(&bodies[1]);
        assert_eq!(
            second.len(),
            1,
            "the tail of the lost chain is not sent again"
        );
        let position = position(&second[0]);
        assert_ne!(
            position.chain_id, old_chain,
            "the next record starts a new chain"
        );
        assert_eq!(
            position.sequence, 1,
            "the new chain starts at its genesis record"
        );
        assert_eq!(
            position.prev_record_hash,
            Digest::GENESIS.to_string(),
            "a genesis record links to nothing"
        );
        let stats = metrics.snapshot_and_reset().operational_stats;
        assert_eq!(
            stats.get("trace.chains_opened"),
            Some(&1),
            "one chain replaced"
        );
        assert_eq!(stats.get("trace.rejected"), Some(&1), "the genesis record");
        assert_eq!(
            stats.get("trace.dropped"),
            Some(&2),
            "the tail of the lost chain"
        );
        emitter.handle.shut_down().await;
    }

    #[tokio::test(start_paused = true)]
    async fn genesis_chain_not_registered_is_final() {
        // Lock in `PRE_REGISTERED` mode refuses the genesis record itself.
        let lock = FakeLock::new([Reply::PerRecord(vec![
            Some("chain_not_registered"),
            Some("chain_not_registered"),
        ])]);
        let metrics = metrics();
        let emitter = start(params(lock.clone(), metrics.clone()), 4, [draft(), draft()]);

        let old_chain = position(&records(&lock.bodies(1).await[0])[0]).chain_id;
        emitter
            .tx
            .send(draft())
            .await
            .expect("the emitter is running");
        let bodies = lock.bodies(2).await;
        let second = records(&bodies[1]);
        assert_eq!(second.len(), 1, "the refused chain is not sent again");
        let position = position(&second[0]);
        assert_ne!(
            position.chain_id, old_chain,
            "the next record starts a new chain"
        );
        assert_eq!(position.sequence, 1, "the new chain starts at its genesis");
        let stats = metrics.snapshot_and_reset().operational_stats;
        assert_eq!(stats.get("trace.rejected"), Some(&1), "the genesis record");
        assert_eq!(
            stats.get("trace.dropped"),
            Some(&1),
            "the tail of the chain"
        );
        assert_eq!(
            stats.get("trace.chains_opened"),
            Some(&1),
            "one chain replaced"
        );
        emitter.handle.shut_down().await;
    }

    #[tokio::test(start_paused = true)]
    async fn chain_not_registered_after_genesis_accepted_opens_new_chain() {
        // Lock accepted the genesis record, then lost the chain's registration.
        let lock = FakeLock::new([
            Reply::AcceptAll,
            Reply::PerRecord(vec![Some("chain_not_registered")]),
        ]);
        let metrics = metrics();
        let emitter = start(params(lock.clone(), metrics.clone()), 4, [draft()]);

        let old_chain = position(&records(&lock.bodies(1).await[0])[0]).chain_id;
        emitter
            .tx
            .send(draft())
            .await
            .expect("the emitter is running");
        let refused = records(&lock.bodies(2).await[1])[0].as_str().to_string();
        emitter
            .tx
            .send(draft())
            .await
            .expect("the emitter is running");
        let bodies = lock.bodies(3).await;
        let third = records(&bodies[2]);
        assert_eq!(third.len(), 1, "only the new record is sent");
        assert_ne!(
            third[0].as_str(),
            refused,
            "the refused record is not resent"
        );
        let position = position(&third[0]);
        assert_ne!(
            position.chain_id, old_chain,
            "a chain Lock no longer knows is replaced"
        );
        assert_eq!(position.sequence, 1, "the new chain starts at its genesis");
        let stats = metrics.snapshot_and_reset().operational_stats;
        assert_eq!(stats.get("trace.rejected"), Some(&1), "the refused record");
        assert_eq!(
            stats.get("trace.chains_opened"),
            Some(&1),
            "one chain replaced"
        );
        emitter.handle.shut_down().await;
    }

    #[tokio::test(start_paused = true)]
    async fn lock_down_backpressure() {
        let lock = FakeLock::new([Reply::AcceptAll, Reply::Retryable, Reply::Retryable]);
        let metrics = metrics();
        let mut params = params(lock.clone(), metrics.clone());
        params.max_retries = 1;
        let (tx, rx) = mpsc::channel(4);
        let handle = spawn_emitter(params, rx).expect("the emitter should start");
        let CjarStore { store, .. } = cjar_store(&fixtures::minimal_valid());
        let recorder = TraceRecorder::new(Some(tx), TEST_LOGGER.clone(), metrics);
        let trace = Arc::new(TraceContext::new(
            "exec-1".to_string(),
            "https://gateway.example.com".to_string(),
            "workload-1".to_string(),
        ));
        let record = || {
            recorder.record(gen_uuid7(), Some(&trace), &store, || {
                Ok(DecisionFacts {
                    action: "TestApp::Action::\"read\"".to_string(),
                    resource_type: "TestApp::Resource".to_string(),
                    outcome: Outcome::Allow,
                    tokens: Arc::from([]),
                })
            })
        };

        assert_eq!(record(), TraceRecordStatus::Emitted, "Lock is up");
        lock.bodies(1).await;
        assert_eq!(record(), TraceRecordStatus::Emitted, "Lock goes down");
        lock.bodies(2).await;
        // The emitter waits to retry, so nothing takes from the channel.
        for _ in 0..4 {
            assert_eq!(record(), TraceRecordStatus::Emitted, "the channel has room");
        }
        assert_eq!(
            record(),
            TraceRecordStatus::NotEmittedQueueFull,
            "a full channel turns the decision away from TRACE, not blocks it"
        );

        let bodies = lock.bodies(4).await;
        let sequences = |body: &str| -> Vec<u64> {
            records(body)
                .iter()
                .map(|record| position(record).sequence)
                .collect()
        };
        assert_eq!(sequences(&bodies[0]), [1], "sent before the outage");
        assert_eq!(sequences(&bodies[1]), [2], "lost to the outage");
        assert_eq!(bodies[2], bodies[1], "the retry before giving up");
        assert_eq!(
            sequences(&bodies[3]),
            [3, 4, 5, 6],
            "signing resumes right after the last signed record, without a gap"
        );
        handle.shut_down().await;
    }

    #[tokio::test(start_paused = true)]
    async fn records_lost_with_their_request_are_counted_once() {
        let lock = FakeLock::new([Reply::Retryable, Reply::Retryable, Reply::Rejected]);
        let metrics = metrics();
        let mut params = params(lock.clone(), metrics.clone());
        params.max_retries = 1;
        let emitter = start(params, 4, [draft()]);

        lock.bodies(2).await;
        emitter
            .tx
            .send(draft())
            .await
            .expect("the emitter is running");
        lock.bodies(3).await;
        emitter.handle.shut_down().await;

        let stats = metrics.snapshot_and_reset().operational_stats;
        assert_eq!(
            stats.get("trace.dropped"),
            Some(&1),
            "the record given up after its retries"
        );
        assert_eq!(
            stats.get("trace.rejected"),
            Some(&1),
            "the record of the refused request"
        );
    }

    #[tokio::test(start_paused = true)]
    async fn shutdown_sends_an_unanswered_batch_once() {
        let lock = FakeLock::new(std::iter::repeat_with(|| Reply::Retryable).take(10));
        let metrics = metrics();
        let emitter = start(params(lock.clone(), metrics.clone()), 4, [draft(), draft()]);

        lock.bodies(1).await;
        emitter.handle.shut_down().await;

        let bodies = lock.bodies(0).await;
        assert_eq!(
            bodies.len(),
            2,
            "one request before shutdown, one during it"
        );
        assert_eq!(
            bodies[1], bodies[0],
            "shutdown sends the held batch as it was"
        );
        assert_eq!(
            counter(&metrics, "trace.dropped"),
            Some(2),
            "records Lock did not take at shutdown are dropped"
        );
    }

    #[tokio::test(start_paused = true)]
    async fn shutdown_does_not_resend_failed_records() {
        let lock = FakeLock::new([
            Reply::Retryable,
            Reply::PerRecord(vec![Some("storage_failure"), Some("storage_failure")]),
        ]);
        let metrics = metrics();
        let emitter = start(params(lock.clone(), metrics.clone()), 4, [draft(), draft()]);

        lock.bodies(1).await;
        emitter.handle.shut_down().await;

        assert_eq!(
            lock.bodies(0).await.len(),
            2,
            "records Lock failed to store are not sent again at shutdown"
        );
        assert_eq!(
            counter(&metrics, "trace.dropped"),
            Some(2),
            "records Lock failed to store at shutdown are dropped"
        );
    }

    #[tokio::test]
    async fn shutdown_flushes_queue() {
        let lock = FakeLock::new([]);
        let emitter = start(params(lock.clone(), metrics()), 4, [draft(), draft()]);

        emitter.handle.shut_down().await;

        let sent: usize = lock
            .bodies(0)
            .await
            .iter()
            .map(|body| records(body).len())
            .sum();
        assert_eq!(sent, 2, "shutdown sends what the channel held");
    }

    #[tokio::test]
    async fn signing_runs_off_the_runtime_thread() {
        let lock = FakeLock::new([]);
        let signer = Arc::new(ThreadRecordingSigner {
            inner: test_signer(),
            threads: Mutex::new(Vec::new()),
        });
        let mut params = params(lock.clone(), metrics());
        params.signer = signer.clone();
        let emitter = start(params, 4, [draft()]);

        lock.bodies(1).await;
        let threads = signer
            .threads
            .lock()
            .expect("thread list is not poisoned")
            .clone();
        assert!(!threads.is_empty(), "the record was signed");
        let runtime_thread = std::thread::current().id();
        assert!(
            threads.iter().all(|thread| *thread != runtime_thread),
            "signing should not run on the runtime thread"
        );
        emitter.handle.shut_down().await;
    }

    #[tokio::test]
    async fn bulk_max_bytes_below_one_record_is_refused() {
        let lock = FakeLock::new([]);
        let mut params = params(lock, metrics());
        let min = TraceConfig::MIN_BULK_MAX_BYTES;
        params.bulk_max_bytes = NonZeroUsize::new(min - 1).expect("non-zero");
        let (_tx, rx) = mpsc::channel(1);

        let err = spawn_emitter(params, rx)
            .err()
            .expect("a bulk limit below one record should be refused");
        assert_eq!(
            err,
            EmitterInitError::BulkMaxBytesTooSmall { min },
            "the error should name the minimum"
        );
    }
}
