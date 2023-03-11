package provider

import (
	"fmt"
	"reflect"

	"github.com/hashicorp/terraform-plugin-sdk/v2/helper/schema"
	"github.com/jans/terraform-provider-jans/jans"
)

// setter is a function that is passed to the encoder function. It
// sets the value of a field in the entity. Depending on the nesting
// level this can either be directly into the resource data or into a
// nested map.
type setter func(key string, val any) error

// getter is a function that is passed to the decoder function. It
// gets the value from any data collection and returns the respective
// value and a boolean indicating if the value was found.
type getter func(key string) (val any, ok bool)

// toSchemaResource converts an entity to a schema.ResourceData, using
// the `schema` tag to map the entity fields to the schema fields. The
// entity must be a pointer to a struct.
func toSchemaResource(d *schema.ResourceData, entity any) error {

	setter := func(key string, val any) error {
		return d.Set(key, val)
	}

	return encoder(setter, entity)
}

// fromSchemaResource retrieves the values from a schema.ResourceData and
// maps them to the entity fields, using the `schema` tag. The entity
// must be a pointer to a struct.
func fromSchemaResource(d *schema.ResourceData, entity any) error {

	getter := func(key string) (any, bool) {
		return d.GetOk(key)
	}

	return decoder(getter, entity)
}

// patchFromResourceData creates a list of patch requests from the
// resource data. For each field in the entity, the value is retrieved
// from the resource data and if it was set there, a new patch
// request will be created with the correct path set.
func patchFromResourceData(d *schema.ResourceData, entity any) ([]jans.PatchRequest, error) {

	ret, err := recursivePatchFromResourceData(d, entity, "/", "", []jans.PatchRequest{})

	return ret, err
}

// recursivePatchFromResourceData is the recursive part of the functionality
// of patchFromResourceData, used for nested structs. It should never be
// called on its own, only via patchFromResourceData.
func recursivePatchFromResourceData(d *schema.ResourceData, entity any, jsonPath, schemaPath string, data []jans.PatchRequest) ([]jans.PatchRequest, error) {

	var e reflect.Value

	if reflect.TypeOf(entity).Kind() == reflect.Ptr {
		e = reflect.ValueOf(entity).Elem()
	} else {
		e = reflect.ValueOf(entity)
	}

	for i := 0; i < e.NumField(); i++ {

		schemaTag, ok := e.Type().Field(i).Tag.Lookup("schema")
		if !ok {
			// skip fields that don't have a schema mapping name
			continue
		}

		jsonTag, ok := e.Type().Field(i).Tag.Lookup("json")
		if !ok {
			// skip fields that don't have a json mapping name
			continue
		}

		val, ok := d.GetOk(schemaPath + schemaTag)
		if !ok {
			// value was not set
			continue
		}

		// get the value of the entity field
		v := e.Field(i).Interface()

		// create an instance of the field type, if it's a pointer
		t := reflect.TypeOf(v)
		if t.Kind() == reflect.Ptr {
			t = t.Elem()
			v = reflect.New(t).Interface()
		}

		// for structs, recurse into the field
		if t.Kind() == reflect.Struct {

			subPath := jsonPath + jsonTag + "/"
			schemaPath := schemaTag + ".0."
			newPatches, err := recursivePatchFromResourceData(d, v, subPath, schemaPath, data)
			if err != nil {
				return nil, err
			}

			data = newPatches

			continue
		}

		// create new patch request for the field
		data = append(data, jans.PatchRequest{
			Op:    "replace",
			Path:  jsonPath + jsonTag,
			Value: val,
		})

	}

	return data, nil
}

// encoder recursively transforms the passed structure into the correct
// target structure, using reflection to iterate over the attributes and
// the provided setter function to correctly transform them.
func encoder(s setter, entity any) error {

	var e reflect.Value

	if reflect.TypeOf(entity).Kind() == reflect.Ptr {
		e = reflect.ValueOf(entity).Elem()
	} else {
		e = reflect.ValueOf(entity)
	}

	for i := 0; i < e.NumField(); i++ {

		// first get the name of the schema field, skipping fields
		// that don't have a schema mapping name
		n, ok := e.Type().Field(i).Tag.Lookup("schema")
		if !ok {
			continue
		}

		// get the value of the entity field
		v := e.Field(i).Interface()

		// dereference the value if it's a pointer
		t := reflect.TypeOf(v)
		if t.Kind() == reflect.Ptr {

			if !reflect.ValueOf(v).IsNil() {
				t = reflect.Indirect(reflect.ValueOf(v)).Type()
				v = reflect.ValueOf(v).Elem().Interface()
			} else {
				v = nil
			}
		}

		// Since structs cannot directly be represented in the schema.ResourceData,
		// the common workaround is to convert the struct to a map and then set the
		// map as the value, wrapping it inside an interface slice ¯\_(ツ)_/¯
		if t.Kind() == reflect.Struct {

			m := make(map[string]interface{})
			mapSetter := func(key string, val any) error {
				m[key] = val
				return nil
			}

			if err := encoder(mapSetter, v); err != nil {
				return fmt.Errorf("failed to convert struct to map: %w", err)
			}

			if err := s(n, []interface{}{m}); err != nil {
				return fmt.Errorf("failed to set %s: %w", n, err)
			}

			continue
		}

		// check if slice is a slice of structs. If so, iterate over the entries and
		// convert each struct to a map and add it to a slice of maps.
		if t.Kind() == reflect.Slice && t.Elem().Kind() == reflect.Struct {

			sl := reflect.ValueOf(v)

			nestedMaps := make([]map[string]interface{}, sl.Len())

			for i := 0; i < sl.Len(); i++ {

				nestedMap := make(map[string]interface{})
				mapSetter := func(key string, val any) error {
					nestedMap[key] = val
					return nil
				}

				val := sl.Index(i).Interface()
				if err := encoder(mapSetter, val); err != nil {
					return fmt.Errorf("failed to convert struct to map: %w", err)
				}

				nestedMaps[i] = nestedMap
			}

			if err := s(n, nestedMaps); err != nil {
				return fmt.Errorf("failed to set %s: %w", n, err)
			}

			continue
		}

		// primitive types and slices are set directly
		if err := s(n, v); err != nil {
			return fmt.Errorf("failed to set %s: %w", n, err)
		}

	}

	return nil
}

// decorder recursively sets the attributes of the passed structure
// with the values returned by the getter func for the respective key.
func decoder(g getter, entity any) error {

	var e reflect.Value

	if reflect.TypeOf(entity).Kind() == reflect.Ptr {
		e = reflect.ValueOf(entity).Elem()
	} else {
		e = reflect.ValueOf(entity)
	}

	for i := 0; i < e.NumField(); i++ {

		t, ok := e.Type().Field(i).Tag.Lookup("schema")
		if !ok {
			// skip fields that don't have a schema mapping name
			continue
		}

		v, ok := g(t)
		if !ok {
			// value was not set

			// we're entering this branch for empty slices, so we need to
			// set the value to an empty slice if we want to fully restore
			// the original state of the entity

			continue
		}

		f := e.Field(i)

		if err := setFieldValue(f, v); err != nil {
			return fmt.Errorf("failed to set field %s: %w", t, err)
		}
	}

	return nil
}

// setFieldValue sets the value of a reflection field, handling all different
// types of values and considering terraform schema limitations.
func setFieldValue(f reflect.Value, v interface{}) error {

	vType := reflect.TypeOf(f.Interface())
	isPointer := false

	// target field is of type `any`
	if vType == nil {
		f.Set(reflect.ValueOf(v))
		return nil
	}

	// if the field is a pointer, use the underlying type
	// for the below switch statement
	if vType.Kind() == reflect.Ptr {
		vType = vType.Elem()
		isPointer = true
	}

	switch vType.Kind() {

	case reflect.Struct:
		// if the field is a struct, we need to convert the single item slice
		// with a map to a struct first, before we can set the value.

		s, ok := v.([]interface{})
		if !ok {
			return fmt.Errorf("failed to convert value to slice")
		}

		if len(s) == 0 || s[0] == nil {
			return nil
		}

		m, ok := s[0].(map[string]interface{})
		if !ok {
			return fmt.Errorf("failed to convert value to map")
		}

		v := f.Addr().Interface()
		if isPointer {
			// if the field is a pointer, we need to create a new
			// instance of the struct, since it is nil at this point
			v = reflect.New(vType).Interface()
		}

		getter := func(key string) (any, bool) {
			val, ok := m[key]
			return val, ok
		}

		if err := decoder(getter, v); err != nil {
			return fmt.Errorf("failed to convert map to struct: %w", err)
		}

		// set the field value using the pointer to the struct.
		valueToSet := reflect.ValueOf(v)
		if !isPointer {
			// If it's already a pointer, we have to dereference it first.
			valueToSet = reflect.Indirect(valueToSet)
		}
		f.Set(valueToSet)

	case reflect.Slice:
		elemSlice, err := convertSlice(vType, v)
		if err != nil {
			return fmt.Errorf("failed to convert slice: %w", err)
		}

		f.Set(elemSlice)

	case reflect.Map:
		// if the field is a map, we need to initialize a new map of
		// the correct type and add all items to it

		// check the size of the map, if it is 0, we can skip setting the value
		// to avoid some inconsistency, which result from terraform's schema
		// not passing nil values for maps correctly.
		// https://github.com/hashicorp/terraform/issues/29921
		length := len(v.(map[string]any))
		if length == 0 {
			break
		}

		// only string keys are supported for maps, so we can just
		// use the map[string]interface{} type for iterating, but
		// need to create a new map of the correct type
		newMap := reflect.MakeMap(reflect.MapOf(reflect.TypeOf(""), vType.Elem()))

		for key, elem := range v.(map[string]any) {
			newMap.SetMapIndex(reflect.ValueOf(key), reflect.ValueOf(elem))
		}

		f.Set(newMap)

	default:

		// All other types are set directly. We just need to make sure
		// pointers are handled correctly via a temporary variable.
		var valueToSet reflect.Value
		if isPointer {
			interim := reflect.New(vType).Elem()
			interim.Set(reflect.ValueOf(v))
			valueToSet = interim.Addr()
		} else {
			valueToSet = reflect.Indirect(reflect.ValueOf(v))
		}

		// special treatment for OptionalString
		if f.Type().AssignableTo(valueToSet.Type()) {
			f.Set(valueToSet)
		} else {
			f.Set(valueToSet.Convert(f.Type()))
		}

	}

	return nil
}

// convertSlice will take the provided value and convert it to a slice of the
// correct type. If the elements in the slice are structs, they will be
// converted from a map to a struct first. If the value is nil, an empty slice
// will be returned. For slices, this will run recursively.
func convertSlice(targetType reflect.Type, value interface{}) (reflect.Value, error) {

	// if the field is a slice, we need to initialize a new slice of
	// the correct type and add all items to it

	elemType := targetType.Elem()

	// create a new slice of the correct type
	elemSlice := reflect.New(reflect.SliceOf(elemType)).Elem()

	// if the elements in the slice are structs, we need to map them back
	// from the map to the struct first, before we can add them to the slice
	if targetType.Elem().Kind() == reflect.Struct {

		for _, elem := range value.([]interface{}) {

			m, ok := elem.(map[string]interface{})
			if !ok {
				return reflect.ValueOf(nil), fmt.Errorf("failed to convert value to map")
			}

			// create a new struct of the correct type
			newElem := reflect.New(elemType)

			getter := func(key string) (any, bool) {
				val, ok := m[key]
				return val, ok
			}

			if err := decoder(getter, newElem.Interface()); err != nil {
				return reflect.ValueOf(nil), fmt.Errorf("failed to convert map to struct: %w", err)
			}

			elemSlice = reflect.Append(elemSlice, reflect.Indirect(newElem))
		}

	} else if targetType.Elem().Kind() == reflect.Slice {

		// nested slices have to be handled separately from primitive types

		subType := reflect.SliceOf(elemType.Elem())
		for _, elem := range value.([]interface{}) {
			val, err := convertSlice(subType, elem)
			if err != nil {
				return reflect.ValueOf(nil), fmt.Errorf("failed to convert slice: %w", err)
			}

			elemSlice = reflect.Append(elemSlice, val)
		}

	} else {

		// otherwise we can just fill the slice, assuming it's
		// a slice of primitive types
		for _, elem := range value.([]interface{}) {
			elemSlice = reflect.Append(elemSlice, reflect.ValueOf(elem))
		}

	}

	return elemSlice, nil
}
