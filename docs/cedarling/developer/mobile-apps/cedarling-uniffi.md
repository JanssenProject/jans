# UniFFI binding for Cedarling

[UniFFI](https://mozilla.github.io/uniffi-rs/latest/) (Universal Foreign Function Interface) is a tool developed by Mozilla to simplify cross-language bindings, primarily between Rust and other languages like Kotlin, Swift, and Python. It allows Rust libraries to be used in these languages without manually writing complex foreign function interface (FFI) bindings. For eg. developing iOS applications typically involves languages like Swift or Objective-C. However, with the advent of UniFFI, it’s now possible to use rust code iOS apps.

Speaking about Cedarling, it interacts with outside world mainly using 3 interfaces `init`, `authz` and `log`. The UniFFI binding for Cedarling wraps the init, authz and log interfaces of Cedarling and uses the [Procedural Macros](https://mozilla.github.io/uniffi-rs/latest/proc_macro/index.html) to expose them to other languages. It also exposes the required Structs and Enums used as parameters and return-types in these interfaces.

## Exposed Functions

- **Cedarling::load_from_json**

   Loads a Cedarling instance from a JSON configuration.
    
   ```declarative
   #[uniffi::constructor]
   pub fn load_from_json(config: String) -> Result<Self, CedarlingError>
   ```

   **Usage in Swift:**

   ```declarative
   let cedarling = try Cedarling.loadFromJson(config: configJson);
   ```

  **Usage in Kotlin:**

  ```declarative
  val cedarling: Cedarling = Cedarling.loadFromJson(configJson)
  ```

- **Cedarling::load_from_file**

   Loads a Cedarling instance from a configuration file.
    
   ```declarative
   #[uniffi::constructor]
   pub fn load_from_file(path: String) -> Result<Self, CedarlingError>
   ```

   **Usage in Swift:**

   ```declarative
   let cedarling = try Cedarling.loadFromFile(path: "/path/to/config.json")
   ```

  **Usage in Kotlin:**

  ```declarative
  val cedarling: Cedarling = Cedarling.loadFromFile("/path/to/config.json")
  ```

- **Cedarling::load_from_json_with_archive_bytes**

   Loads a Cedarling instance from a bootstrap JSON string plus the raw bytes of a Cedar archive (`.cjar`). Use this when the host cannot pass a policy store path—for example **Android `assets/`** read via `AssetManager`. Fields `CEDARLING_POLICY_STORE_LOCAL`, `CEDARLING_POLICY_STORE_URI`, and `CEDARLING_POLICY_STORE_LOCAL_FN` in the JSON are ignored; the archive bytes are the policy source (same idea as WASM `init_from_archive_bytes`).

   ```declarative
   #[uniffi::constructor]
   pub fn load_from_json_with_archive_bytes(
       config: String,
       archive_bytes: Vec<u8>,
   ) -> Result<Self, CedarlingError>
   ```

   **Usage in Swift:**

   ```declarative
   let cedarling = try Cedarling.loadFromJsonWithArchiveBytes(
       config: configJson,
       archiveBytes: cjarData
   )
   ```

  **Usage in Kotlin:**

  ```declarative
  val cedarling = Cedarling.loadFromJsonWithArchiveBytes(configJson, cjarBytes)
  ```

- **Cedarling::authorizeUnsigned**

   Handles unsigned authorization requests with directly provided principals.
    
   ```declarative
   #[uniffi::method]
        pub fn authorize_unsigned(
            &self,
            principals: Vec<EntityData>,
            action: String,
            resource: EntityData,
            context: String,
        ) -> Result<AuthorizeResult, AuthorizeError> 
   ```

   **Usage in Swift:**

   ```declarative
   let result = try cedarling.authorizeUnsigned(principals: principalEntities, action: "Jans::Action::\"Update\"", resource: resourceEntity, context: "{}")
   ```

  **Usage in Kotlin:**

  ```declarative
  val authResult: AuthorizeResult = cedarling.authorizeUnsigned(principalEntities, "Jans::Action::\"Update\"", resourceEntity, "{}")
  ```

- **Cedarling::pop_logs**

   Retrieves logs as JSON strings.
    
   ```declarative
   #[uniffi::method]
   pub fn pop_logs(&self) -> Result<Vec<String>, LogError> 
   ```

   **Usage in Swift:**

   ```declarative
   let logs = try cedarling.popLogs()
   ```

  **Usage in Kotlin:**

  ```declarative
  val logs = cedarling.popLogs()

  ```

- **Cedarling::get_log_by_id**

   Retrieves a log entry by ID.

   ```declarative
   #[uniffi::method]
   pub fn get_log_by_id(&self, id: &str) -> Result<String, LogError>
   ```

   **Usage in Swift:**

   ```declarative
   let log = try cedarling.getLogById(id: "log123")
   ```

  **Usage in Kotlin:**

  ```declarative
  val logs = cedarling.getLogById("log123")

  ```

- **Cedarling::get_log_ids**

   Get all log ids

   ```declarative
   #[uniffi::method]
   pub fn get_log_ids(&self) -> Vec<String>
   ```

   **Usage in Swift:**

   ```declarative
   let ids = try cedarling.getLogIds()
   ```

  **Usage in Kotlin:**

  ```declarative
  val ids = cedarling.getLogById("log123")

  ```

- **Cedarling::get_logs_by_tag**

   Get logs by tag, like `log_kind` or `log level`.

   ```declarative
   #[uniffi::method]
   pub fn get_logs_by_tag(&self, tag: &str) -> Result<Vec<String>, LogError> {
   ```

   **Usage in Swift:**

   ```declarative
   let logs = try cedarling.getLogsByTag(tag: "DEBUG")
   ```

  **Usage in Kotlin:**

  ```declarative
  val logs = cedarling.getLogsByTag("DEBUG")

  ```

- **Cedarling::get_logs_by_request_id**

   Get all logs by request_id

   ```declarative
   #[uniffi::method]
   pub fn get_logs_by_request_id(&self, request_id: &str) -> Result<Vec<String>, LogError>
   ```

   **Usage in Swift:**

   ```declarative
   let logs = try cedarling.getLogsByRequestId(request_id: "12434-32323-43434")
   ```

  **Usage in Kotlin:**

  ```declarative
  val logs = cedarling.getLogsByRequestId("12434-32323-43434")

  ```

- **Cedarling::get_logs_by_request_id_and_tag**

   Get log by request_id and tag, like composite key `request_id` + `log_kind`.

   ```declarative
   #[uniffi::method]
   pub fn get_logs_by_request_id_and_tag(
        &self,
        request_id: &str,
        tag: &str,
    ) -> Result<Vec<String>, LogError>
   ```

   **Usage in Swift:**

   ```declarative
   let logs = try cedarling.getLogsByRequestIdAndTag(request_id: "12434-32323-43434", tag: "Decision")
   ```

  **Usage in Kotlin:**

  ```declarative
  val logs = cedarling.getLogsByRequestId("12434-32323-43434", "Decision")

  ```
