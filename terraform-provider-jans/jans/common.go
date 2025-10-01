package jans

import (
	"encoding/json"
)

// OptionalString is a string with a custom unmarsahller. It is used in
// cases, where the API will return an empty object instead of an empty
// string for certain attributes.
type OptionalString string

func (os *OptionalString) UnmarshalJSON(data []byte) error {
	var s interface{}
	if err := json.Unmarshal(data, &s); err != nil {
		return err
	}

	if st, ok := s.(string); ok {
		*os = OptionalString(st)
		return nil
	}

	*os = OptionalString("")
	return nil
}

// CustomAttribute is the definition of a custom attributes that
// can be used in various other entities.
type CustomAttribute struct {
	Name         string   `schema:"name" json:"name,omitempty"`
	MultiValued  bool     `schema:"multi_valued" json:"multiValued,omitempty"`
	DisplayValue string   `schema:"display_value" json:"displayValue,omitempty"`
	Value        string   `schema:"value" json:"value,omitempty"`
	Values       []string `schema:"values" json:"values,omitempty"`
}

type customAttributeJSON struct {
	Name         string `schema:"name" json:"name,omitempty"`
	MultiValued  bool   `schema:"multi_valued" json:"multiValued,omitempty"`
	DisplayValue string `schema:"display_value" json:"displayValue,omitempty"`
	JsonValue    any    `json:"value,omitempty"`
	JsonValues   []any  `json:"values,omitempty"`
}

type Meta struct {
	ResourceType string `schema:"resource_type" json:"resourceType,omitempty"`
	Created      string `schema:"created" json:"created,omitempty"`
	LastModified string `schema:"last_modified" json:"lastModified,omitempty"`
	Location     string `schema:"location" json:"location,omitempty"`
}

// create a custom json Unmarshaler for CustomAttribute
// to handle the different types of values
func (ca *CustomAttribute) UnmarshalJSON(data []byte) error {

	// create a generic structure to unmarshal into
	var generic customAttributeJSON
	err := json.Unmarshal(data, &generic)
	if err != nil {
		return err
	}

	ca.Name = generic.Name
	ca.MultiValued = generic.MultiValued
	ca.DisplayValue = generic.DisplayValue
	ca.Values = make([]string, 0)

	if generic.JsonValue != nil {
		value, err := json.Marshal(generic.JsonValue)
		if err != nil {
			return err
		}
		ca.Value = string(value)
	}

	for _, v := range generic.JsonValues {
		value, err := json.Marshal(v)
		if err != nil {
			return err
		}
		ca.Values = append(ca.Values, string(value))
	}

	return nil
}

// create a custom json Marshaler for CustomAttribute
// to handle the different types of values
func (ca *CustomAttribute) MarshalJSON() ([]byte, error) {

	jsonAtt := customAttributeJSON{
		Name:         ca.Name,
		MultiValued:  ca.MultiValued,
		DisplayValue: ca.DisplayValue,
		JsonValues:   make([]any, 0),
	}

	if ca.Value != "" {
		if err := json.Unmarshal([]byte(ca.Value), &jsonAtt.JsonValue); err != nil {
			return nil, err
		}
	}

	for _, v := range ca.Values {
		if v == "" {
			continue
		}
		var val any
		if err := json.Unmarshal([]byte(v), &val); err != nil {
			return nil, err
		}
		jsonAtt.JsonValues = append(jsonAtt.JsonValues, val)
	}

	ret, err := json.Marshal(jsonAtt)
	return ret, err
}
