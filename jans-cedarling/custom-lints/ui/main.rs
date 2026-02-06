#![allow(unused)]

use cedar_policy::EntityUid;

fn main() {
    // This should trigger the lint
    let eid1 = EntityUid::from_str(&format!(r#"User::"alice""#));

    // This should trigger the lint
    let user = "alice";
    let eid2 = EntityUid::from_str(&format!(r#"User::"{}""#, user));

    // This should trigger the lint - (even with std::format!)
    let eid3 = EntityUid::from_str(&std::format!(r#"User::"bob""#));

    // This should NOT trigger
    let eid4 = EntityUid::from_str(r#"User::"alice""#);

    // This should NOT trigger
    let literal = String::from(r#"User::"dave""#);
    let eid5 = EntityUid::from_str(&literal);

    let entity_type_name = "Jans::User";
    let entity_id = "user123";
    let eid7 = cedar_policy::EntityUid::from_str(&format!(r#"{entity_type_name}::"{entity_id}""#));

    let eid8 = cedar_policy::EntityUid::from_str(&format!(r#"Jans::User::"bob123""#));

    // This should be flagged as well since it also has an impact on the performance
    let raw_eid = format!(r#"{entity_type_name}::"{entity_id}""#);
    let eid9 = EntityUid::from_str(&raw_eid);
}

// Mock EntityUid for testing (since we don't want to pull in the actual Cedar crate)
mod cedar_policy {
    pub(super) struct EntityUid;

    impl EntityUid {
        pub(super) fn from_str(s: &str) -> Result<Self, String> {
            Ok(EntityUid)
        }
    }
}
