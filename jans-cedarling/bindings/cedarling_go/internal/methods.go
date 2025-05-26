package internal

import (
	"encoding/json"
	"errors"
)

var G2R = G2RCallImpl{}

func (ri *ResultInstance) Error() error {
	if ri.error == "" {
		return nil
	}
	return errors.New(ri.error)
}

func (r *Result) JsonValue() string {
	return r.value
}

func (r *Result) Error() error {
	if r.error == "" {
		return nil
	}
	return errors.New(r.error)
}

func NewInstance(bootstrap_config_raw map[string]interface{}) (uint, error) {
	bootstrap_config_raw_json_b, err := json.Marshal(bootstrap_config_raw)
	if err != nil {
		return 0, err
	}
	bootstrap_config_raw_json := string(bootstrap_config_raw_json_b)

	result := G2R.new_instance(&bootstrap_config_raw_json)

	return result.instance_id, result.Error()
}

func NewInstanceWithEnv(bootstrap_config_raw *map[string]interface{}) (uint, error) {
	bootstrap_config_raw_json_b, err := json.Marshal(bootstrap_config_raw)
	if err != nil {
		return 0, err
	}
	bootstrap_config_raw_json := string(bootstrap_config_raw_json_b)
	result := G2R.new_with_env_instance(&bootstrap_config_raw_json)
	return result.instance_id, result.Error()
}

func DropInstance(instance_id uint) {
	G2R.drop_instance(&instance_id)
}

func CallAuthorize(instance_id uint, request_json string) Result {
	result := G2R.authorize(&instance_id, &request_json)
	return result
}

func CallAuthorizeUnsigned(instance_id uint, request_json string) Result {
	result := G2R.authorize_unsigned(&instance_id, &request_json)
	return result
}

func CallPopLogs(instance_id uint) []string {
	logs := G2R.pop_logs(&instance_id)
	return logs
}

func CallGetLogById(instance_id uint, log_id string) string {
	result := G2R.get_log_by_id(&instance_id, &log_id)
	return result
}

func CallGetLogIds(instance_id uint) []string {
	log_ids := G2R.get_log_ids(&instance_id)
	return log_ids
}

func CallGetLogsByTag(instance_id uint, tag string) []string {
	logs := G2R.get_logs_by_tag(&instance_id, &tag)
	return logs
}

func CallGetLogsByRequestId(instance_id uint, request_id string) []string {
	logs := G2R.get_logs_by_request_id(&instance_id, &request_id)
	return logs
}

func CallGetLogsByRequestIdAndTag(instance_id uint, request_id string, tag string) []string {
	logs := G2R.get_logs_by_request_id_and_tag(&instance_id, &request_id, &tag)
	return logs
}

func CallShutDown(instance_id uint) {
	G2R.shut_down(&instance_id)
}
