package jans

import (
	"encoding/json"
	"reflect"
	"testing"

	"github.com/google/go-cmp/cmp"
)

func TestCustomAttributeJSON(t *testing.T) {

	ca := &CustomAttribute{
		Name:         "birthdate",
		MultiValued:  false,
		Values:       []string{"\"2000-12-31T00:00\""},
		Value:        "\"2000-12-31T00:00\"",
		DisplayValue: "2000-12-31T00:00",
	}

	res, err := json.Marshal(ca)
	if err != nil {
		t.Error(err)
	}

	expected := `{"name":"birthdate","displayValue":"2000-12-31T00:00","value":"2000-12-31T00:00","values":["2000-12-31T00:00"]}`
	if string(res) != expected {
		t.Errorf("expected %s, got %s", expected, string(res))
	}

	var caParsed CustomAttribute
	err = json.Unmarshal(res, &caParsed)
	if err != nil {
		t.Error(err)
	}

	if cmp.Diff(ca, &caParsed) != "" {
		t.Errorf("expected %v, got %v", ca, caParsed)
	}

	ca2 := &CustomAttribute{
		Name:         "emailVerified",
		MultiValued:  false,
		Values:       []string{"true"},
		Value:        "true",
		DisplayValue: "true",
	}

	res, err = json.Marshal(ca2)
	if err != nil {
		t.Error(err)
	}

	expected = `{"name":"emailVerified","displayValue":"true","value":true,"values":[true]}`

	if string(res) != expected {
		t.Errorf("expected %s, got %s", expected, string(res))
	}

	ca3 := &CustomAttribute{
		Name: "noValues",
	}

	res, err = json.Marshal(ca3)
	if err != nil {
		t.Error(err)
	}

	expected = `{"name":"noValues"}`
	if string(res) != expected {
		t.Errorf("expected %s, got %s", expected, string(res))
	}
}

func TestOptionalStringReflection(t *testing.T) {

	foo := OptionalString("foo")
	b := &foo

	f := reflect.ValueOf(b)
	vType := reflect.TypeOf(f.Interface()).Elem()

	if vType.Kind() != reflect.String {
		t.Errorf("expected string, got %v", vType.Kind())
	}

	valueToSet := reflect.Indirect(reflect.ValueOf("bar"))
	toSet := reflect.Indirect(f)

	if toSet.Type().AssignableTo(valueToSet.Type()) {
		toSet.Set(valueToSet)
	} else {
		toSet.Set(valueToSet.Convert(toSet.Type()))
	}

	if foo != "bar" {
		t.Errorf("expected bar, got %s", foo)
	}

}
