"""Rank injected anomalies (truth.json) among FACADE scores.

usage: evaluate_scores.py <facade_dir> <scores.tfrecord> <truth.json>
"""
import json
import sys

sys.path.insert(0, sys.argv[1])
import tensorflow as tf  # noqa: E402
from protos import score_pb2  # noqa: E402

scores_path, truth_path = sys.argv[2], sys.argv[3]
truth = {a["record_id"] for a in json.load(open(truth_path))["anomalies"]}


def record_id(action_id: bytes) -> str:
    return action_id.decode().rsplit(":", 1)[0].rsplit("/", 1)[-1]


rows = []
for raw in tf.data.TFRecordDataset([scores_path]):
    s = score_pb2.Score()
    s.ParseFromString(raw.numpy())
    rows.append((s.score, record_id(s.action_id), s.principal, s.resource_id))
rows.sort(reverse=True)
n = len(rows)
hits = [(i + 1, r) for i, r in enumerate(rows) if r[1] in truth]
print(f"scored actions: {n}, injected anomalies: {len(truth)}, found in output: {len(hits)}")
for rank, r in hits:
    print(f"  rank {rank:4d}/{n}  score={r[0]:.4f}  {r[2].rsplit('/', 1)[-1]}  {r[3]}")
pos = [r[0] for r in rows if r[1] in truth]
neg = [r[0] for r in rows if r[1] not in truth]
if pos and neg:
    auc = sum((p > q) + 0.5 * (p == q) for p in pos for q in neg) / (len(pos) * len(neg))
    print(f"AUC (anomaly vs normal) = {auc:.3f}")
print("top 8:")
for r in rows[:8]:
    print(f"  {r[0]:.4f}  {r[2].rsplit('/', 1)[-1]}  {r[3]}  {'<-- INJECTED' if r[1] in truth else ''}")
