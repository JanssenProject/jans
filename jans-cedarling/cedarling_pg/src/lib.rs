// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! `PostgreSQL` extension (`cedarling_pg`) for Cedarling-backed authorization.

use pgrx::prelude::*;

/// Compile-time anchor so the `cedarling` blocking API is linked; full integration comes later.
const _: usize = core::mem::size_of::<cedarling::blocking::Cedarling>();

::pgrx::pg_module_magic!(name, version);

#[pg_extern]
fn hello_cedarling_pg() -> &'static str {
    "Hello, cedarling_pg"
}

#[cfg(any(test, feature = "pg_test"))]
#[pg_schema]
mod tests {
    use pgrx::prelude::*;

    #[pg_test]
    fn test_hello_cedarling_pg() {
        assert_eq!("Hello, cedarling_pg", crate::hello_cedarling_pg());
    }
}

#[cfg(feature = "pg_bench")]
#[pg_schema]
mod benches {
    use pgrx::prelude::*;
    use pgrx_bench::{black_box, Bencher};

    #[pg_bench]
    fn bench_hello_cedarling_pg(b: &mut Bencher) {
        b.iter(|| {
            black_box(crate::hello_cedarling_pg());
        });
    }
}

/// Required by `cargo pgrx test`.
#[cfg(test)]
pub mod pg_test {
    pub fn setup(_options: Vec<&str>) {
        // One-off initialization when the pg_test framework starts.
    }

    #[must_use]
    pub fn postgresql_conf_options() -> Vec<&'static str> {
        vec![]
    }
}
