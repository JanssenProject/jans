# SparKV

SparKV is an expirable in-memory key-value store for Rust.

## Features

1. Flexible expiration duration (a.k.a. time-to-live or TTL) per entry instead of database-wide common TTL.
    1. This is similar to that of DNS where each entries of the same domain can have its own unique TTL.
2. Automatically clears expired entries by default.
3. String-based key-value store.
4. Fast data entry enforcements, including ensuring entry size, database size and max TTL.
5. SparKV is intentionally not an LRU cache.
6. Configurable.

## Usage

Add SparKV crate to your Cargo dependencies:

Quick start

```rust
use sparkv::SparKV;

let mut sparkv = SparKV::new();
sparkv.set("your-key", "your-value"); // write
let value = sparkv.get("your-key").unwrap(); // read

// Write with unique TTL
sparkv.set_with_ttl("diff-ttl", "your-value", chrono::Duration::seconds(60));
```

See `config.rs` for more configuration options.

## TODO

1. Documentations
1. Support generic data types

## License

MIT License<br>
Copyright © 2024 U-Zyn Chua
