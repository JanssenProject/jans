# cedarling_ext_php_rs

This example uses `ext-php-rs` https://crates.io/crates/ext-php-rs to create a PHP extension library from Rust code. Follow the steps below to install and build the library.

## Steps to make it working

NOTICE!!! Here is assumed that  your cedarling repository existed on the path: /var/www/html/cedarling/jans . If it is under the different path then you need to change prefixes on steps 1. and 4 to your correct ones.

1. 
   
   ```bash
   cd /var/www/html/cedarling/jans/jans-cedarling/bindings/cedarling_ext_php_rs
   ```

2. 
   Verify Rust installation by running:

   ```bash
   cargo --version
   ```

   If Rust is not installed, you can install it from [here](https://www.rust-lang.org/tools/install)
   If Rust is installed but can not be accessed globally then perform command:
   ```bash
   export PATH="path_to_cargo_bin/.cargo/bin:$PATH" 
   ```
3. Build project
   ```bash
   cargo build
   ```

4. 
   - Run test : 

     ```bash
     php -d extension=/var/www/html/cedarling/jans/jans-cedarling/target/debug/libext_php_rs_test.so /var/www/html/cedarling/jans/jans-cedarling/bindings/cedarling_ext_php_rs/test.php
     ```
     
5.      You can find php extension library on the path /var/www/html/cedarling/jans/jans-cedarling/target/debug/libext_php_rs_test.so and use it . Function cedarling_authorize_test($token, $payload_str); will be accessible in your php code. 

