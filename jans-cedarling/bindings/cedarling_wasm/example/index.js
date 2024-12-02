import { Cedarling } from "cedarling_wasm";

let cedarling = Cedarling.new({
  "application_name": "TestApp",
  "policy_store_id": "asdasd123123",
});

cedarling.authorize();
