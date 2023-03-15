package jans

import (
	"reflect"
)

// PatchRequest is used when calling PATCH requests to update
// certain entities.
type PatchRequest struct {
	Op    string      `json:"op,omitempty"`
	Path  string      `json:"path,omitempty"`
	Value interface{} `json:"value,omitempty"`
}

// createPatches creates a list of patch requests from the provided entity.
// For each of its fields, the value is retrieved, a new patch request will
// be created with the correct path set.
func createPatches(entity, original any) ([]PatchRequest, error) {

	ret, err := recursivePatchFromEntity(entity, original, "/", []PatchRequest{})

	return ret, err
}

// recursivePatchFromEntity is the recursive part of the functionality
// of createPatches, used for nested structs. It should never be
// called on its own, only via patchFromResourceData.
func recursivePatchFromEntity(entity, original any, jsonPath string, data []PatchRequest) ([]PatchRequest, error) {

	var e reflect.Value

	if reflect.TypeOf(entity).Kind() == reflect.Ptr {
		e = reflect.ValueOf(entity).Elem()
	} else {
		e = reflect.ValueOf(entity)
	}

	var o reflect.Value

	if reflect.TypeOf(original).Kind() == reflect.Ptr {
		o = reflect.ValueOf(original).Elem()
	} else {
		o = reflect.ValueOf(original)
	}

	for i := 0; i < e.NumField(); i++ {

		jsonTag, ok := e.Type().Field(i).Tag.Lookup("json")
		if !ok {
			// skip fields that don't have a json mapping name
			continue
		}

		// get the value of the entity field
		v := e.Field(i).Interface()
		oV := o.Field(i).Interface()

		if reflect.DeepEqual(v, oV) {
			// skip fields that are equal to the original value
			continue
		}

		// create an instance of the field type, if it's a pointer
		t := reflect.TypeOf(v)
		if t.Kind() == reflect.Ptr {
			t = t.Elem()
			v = reflect.New(t).Interface()
		}

		// for structs, recurse into the field
		if t.Kind() == reflect.Struct {

			subPath := jsonPath + jsonTag + "/"
			newPatches, err := recursivePatchFromEntity(v, oV, subPath, data)
			if err != nil {
				return nil, err
			}

			data = newPatches

			continue
		}

		// create new patch request for the field
		data = append(data, PatchRequest{
			Op:    "replace",
			Path:  jsonPath + jsonTag,
			Value: v,
		})

	}

	return data, nil
}
