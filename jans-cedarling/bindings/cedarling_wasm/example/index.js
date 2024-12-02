import * as cedarling_wasm from "cedarling_wasm";
// cedarling_wasm.init();

let cedarling = cedarling_wasm.Cedarling.new({
  "application_name": "TestApp",
  "policy_store_id": "asdasd123123",
});

cedarling.authorize();
