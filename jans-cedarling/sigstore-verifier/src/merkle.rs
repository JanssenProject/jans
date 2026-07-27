// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Offline Merkle inclusion-proof verification (RFC 6962).
//!
//! The proof is carried inside the bundle, so this needs no network — the
//! "online" step we skip is fetching a fresh signed tree head. The root hash is
//! authenticated separately by the signed checkpoint (see [`crate::tlog`]); this
//! module only ties the log entry to that root.

use sha2::{Digest, Sha256};

use crate::error::SigstoreVerificationError;

/// RFC 6962 leaf hash: `SHA-256(0x00 || entry_bytes)`.
fn hash_leaf(entry: &[u8]) -> [u8; 32] {
    let mut h = Sha256::new();
    h.update([0x00]);
    h.update(entry);
    h.finalize().into()
}

/// RFC 6962 node hash: `SHA-256(0x01 || left || right)`.
fn hash_children(left: &[u8], right: &[u8]) -> [u8; 32] {
    let mut h = Sha256::new();
    h.update([0x01]);
    h.update(left);
    h.update(right);
    h.finalize().into()
}

/// Verify that `entry_bytes` is included at `index` in a log of `tree_size`
/// entries whose Merkle root is `expected_root`, using `proof` (sibling hashes,
/// leaf-to-root order).
///
/// Uses the Trillian `RootFromInclusionProof` fold.
pub(crate) fn verify_inclusion(
    index: u64,
    tree_size: u64,
    entry_bytes: &[u8],
    proof: &[Vec<u8>],
    expected_root: &[u8],
) -> Result<(), SigstoreVerificationError> {
    if index >= tree_size {
        return Err(SigstoreVerificationError::RekorInconsistency {
            reason: format!("inclusion proof index {index} >= tree size {tree_size}"),
        });
    }

    // Number of inner proof nodes using bit-flipping; remaining are border.
    let inner = u64_bit_len(index ^ (tree_size - 1)) as usize;
    let expected = proof_size(index, tree_size);
    if proof.len() != expected {
        return Err(SigstoreVerificationError::RekorInconsistency {
            reason: format!(
                "inclusion proof has {} hashes but expected {expected}",
                proof.len()
            ),
        });
    }

    for (i, sibling) in proof.iter().enumerate() {
        if sibling.len() != 32 {
            return Err(SigstoreVerificationError::RekorInconsistency {
                reason: format!(
                    "inclusion proof sibling {i} is {} bytes, expected 32",
                    sibling.len()
                ),
            });
        }
    }

    let mut res = hash_leaf(entry_bytes).to_vec();

    // Inner nodes: bit `i` of `index` decides sibling side.
    for (i, sibling) in proof[..inner].iter().enumerate() {
        if (index >> i) & 1 == 0 {
            res = hash_children(&res, sibling).to_vec();
        } else {
            res = hash_children(sibling, &res).to_vec();
        }
    }
    // Border nodes: always fold on the left.
    for sibling in &proof[inner..] {
        res = hash_children(sibling, &res).to_vec();
    }

    if res != expected_root {
        return Err(SigstoreVerificationError::RekorInconsistency {
            reason: "inclusion proof does not reconstruct the checkpoint root hash".into(),
        });
    }
    Ok(())
}

/// Bit length of a `u64` (0 → 0, 1 → 1, 5 → 3).
fn u64_bit_len(v: u64) -> u32 {
    64 - v.leading_zeros()
}

/// Exact number of proof hashes for leaf `index` in a Merkle tree of `tree_size`.
fn proof_size(index: u64, tree_size: u64) -> usize {
    let mut count = 0usize;
    let mut i = index;
    let mut n = tree_size;
    while n > 1 {
        if i % 2 == 0 {
            if i + 1 < n {
                count += 1;
            }
        } else {
            count += 1;
        }
        i /= 2;
        n = (n + 1) / 2;
    }
    count
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn single_entry_tree_root_is_leaf_hash() {
        // Tree of size 1: root == leaf hash, empty proof.
        let entry = b"only-entry";
        let root = hash_leaf(entry);
        verify_inclusion(0, 1, entry, &[], &root).expect("size-1 tree verifies with empty proof");
    }

    #[test]
    fn wrong_root_rejected() {
        let entry = b"only-entry";
        let bad = [0u8; 32];
        verify_inclusion(0, 1, entry, &[], &bad).expect_err("wrong root must be rejected");
    }

    #[test]
    fn two_entry_tree_verifies_both_leaves() {
        // size-2 tree: root = H(0x01 || H0 || H1).
        let e0 = b"left";
        let e1 = b"right";
        let h0 = hash_leaf(e0);
        let h1 = hash_leaf(e1);
        let root = hash_children(&h0, &h1);
        // entry 0: sibling is h1 on the right.
        verify_inclusion(0, 2, e0, &[h1.to_vec()], &root).expect("leaf 0 verifies");
        // entry 1: sibling is h0 on the left.
        verify_inclusion(1, 2, e1, &[h0.to_vec()], &root).expect("leaf 1 verifies");
    }

    #[test]
    fn corrupted_proof_hash_rejected() {
        let e0 = b"left";
        let e1 = b"right";
        let h0 = hash_leaf(e0);
        let h1 = hash_leaf(e1);
        let root = hash_children(&h0, &h1);
        let mut bad = h1.to_vec();
        bad[0] ^= 0x01;
        verify_inclusion(0, 2, e0, &[bad], &root)
            .expect_err("a bit-flipped proof hash must be rejected");
    }
}
