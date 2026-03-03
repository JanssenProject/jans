# Log Engine

 Log Engine is responsible for log all authz and init events.

## Cedarling log types

In Cedarling framework [Bootstrap properties](https://github.com/JanssenProject/jans/wiki/Cedarling-Nativity-Plan#bootstrap-properties), a configuration property called`CEDARLING_LOG_TYPE` can take on one of the following values::

* off
* memory
* std_out
* lock

### Log type `off`

This log type is do nothing. It means that all logs will be ignored.

#### Log type `memory`

This log type stores all logs in memory with a time-to-live (TTL) eviction policy.

`CEDARLING_LOG_TTL` - variable determines how long logs are stored in memory, measured in seconds.

### Log type `std_out`

This log type writes all logs to `stdout`. Without storing or additional handling log messages.
[Standart streams](https://www.gnu.org/software/libc/manual/html_node/Standard-Streams.html).

Configuration options for native targets (not applicable to WASM):
- `CEDARLING_STDOUT_MODE`: Logging mode, either `async` or `immediate` (default).
- `CEDARLING_STDOUT_TIMEOUT_MILLIS`: Maximum time in milliseconds to buffer logs before flushing (only applies to async mode)
- `CEDARLING_STDOUT_BUFFER_LIMIT`: Maximum buffer size in bytes before forcing a flush (only applies to async mode)

In `async` mode, logs are buffered and flushed periodically to improve performance.
In `immediate` mode, each log line is written immediately to stdout.

### Log type `lock`

This log type is send logs to the server (corporate feature). Will be discussed later.

## Log Strategy

We use `LogStrategy` logger to implement all types of logger under one interface.
On creating (method new()) it consumes the `LogConfig` with all information about log type and it configuration. And this **factory** method

## Interfaces

Currently we have 2 interfaces (traits):

* `LogWriter` (not public) it is used to write logs.

All log implementations should implement this.

* `LogStorage` are used to gettting logs from storage.

Currently only `MemoryLogger` implement it.
