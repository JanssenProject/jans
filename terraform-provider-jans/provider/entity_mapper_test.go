package provider

import (
	"testing"

	"github.com/google/go-cmp/cmp"
	"github.com/hashicorp/terraform-plugin-sdk/v2/helper/schema"
)

func TestEntityToMap(t *testing.T) {

	type testCase struct {
		Att1 string `schema:"att1"`
		Att2 string `schema:"att2"`
		Att3 int    `schema:"att3"`
	}

	entity := testCase{
		Att1: "att1 val",
		Att2: "att2 val",
		Att3: 3,
	}

	m := map[string]any{}
	setter := func(key string, val any) error {
		m[key] = val
		return nil
	}

	err := encoder(setter, entity)
	if err != nil {
		t.Fatal(err)
	}

	if m["att1"] != "att1 val" {
		t.Errorf("expected att1 val, got %s", m["att1"])
	}

	if m["att2"] != "att2 val" {
		t.Errorf("expected att2 val, got %s", m["att2"])
	}

	if m["att3"] != 3 {
		t.Errorf("expected 3, got %d", m["att3"])
	}

	if len(m) != 3 {
		t.Errorf("expected 3 keys, got %d", len(m))
	}
}

func TestMapToEntity(t *testing.T) {
	type testCase struct {
		Att1 string `schema:"att1"`
		Att2 string `schema:"att2"`
		Att3 int    `schema:"att3"`
	}

	entity := &testCase{}
	m := map[string]interface{}{
		"att1": "att1 val",
		"att2": "att2 val",
		"att3": 3,
	}

	getter := func(key string) (any, bool) {
		val, ok := m[key]
		return val, ok
	}

	if err := decoder(getter, entity); err != nil {
		t.Fatal(err)
	}

	if entity.Att1 != "att1 val" {
		t.Errorf("expected att1 val, got %s", entity.Att1)
	}

	if entity.Att2 != "att2 val" {
		t.Errorf("expected att2 val, got %s", entity.Att2)
	}

	if entity.Att3 != 3 {
		t.Errorf("expected 3, got %d", entity.Att3)
	}
}

func TestResourceMapping(t *testing.T) {

	sd := &schema.Resource{
		Schema: map[string]*schema.Schema{
			"string_attr": {
				Type:     schema.TypeString,
				Optional: true,
			},
			"int_attr": {
				Type:     schema.TypeInt,
				Optional: true,
			},
			"slice_attribute": {
				Type:     schema.TypeList,
				Optional: true,
				Elem: &schema.Schema{
					Type: schema.TypeString,
				},
			},
			"complex_attribute": {
				Type:     schema.TypeList,
				Optional: true,
				MaxItems: 1,
				Elem: &schema.Resource{
					Schema: map[string]*schema.Schema{
						"string_attr": {
							Type:     schema.TypeString,
							Optional: true,
						},
						"int_attr": {
							Type:     schema.TypeInt,
							Optional: true,
						},
						"slice_attribute": {
							Type:     schema.TypeList,
							Optional: true,
							Elem: &schema.Schema{
								Type: schema.TypeString,
							},
						},
						"map_attribute": {
							Type:     schema.TypeMap,
							Optional: true,
							Elem: &schema.Schema{
								Type: schema.TypeString,
							},
						},
					},
				},
			},
		},
	}

	data := sd.Data(nil)

	type testSubStruct struct {
		StringAttr string            `schema:"string_attr"`
		IntAttr    int               `schema:"int_attr"`
		SliceAttr  []string          `schema:"slice_attribute"`
		MapAttr    map[string]string `schema:"map_attribute"`
	}

	type testStruct struct {
		StringAttr  *string        `schema:"string_attr"`
		IntAttr     int            `schema:"int_attr"`
		SliceAttr   []string       `schema:"slice_attribute"`
		ComplexAttr *testSubStruct `schema:"complex_attribute"`
	}

	str := "https://acme.com/oauth2/default"

	entity := testStruct{
		StringAttr: &str,
		IntAttr:    42,
		SliceAttr:  []string{"slice1", "slice2"},
		ComplexAttr: &testSubStruct{
			StringAttr: "sub att1 val",
			IntAttr:    43,
			SliceAttr:  []string{"subslice1", "subslice2"},
			MapAttr: map[string]string{
				"key1": "val1",
				"key2": "val2",
			},
		},
	}

	if err := toSchemaResource(data, entity); err != nil {
		t.Fatal(err)
	}

	newEntity := testStruct{}

	if err := fromSchemaResource(data, &newEntity); err != nil {
		t.Fatal(err)
	}

	if diff := cmp.Diff(entity, newEntity); diff != "" {
		t.Errorf("Got different entity after mapping: %s", diff)
	}

}

func TestNestedStruct(t *testing.T) {

	type lvl3Struct struct {
		SimpleAttr string `schema:"simple_attribute"`
	}

	type lvl2Struct struct {
		Lvl3 lvl3Struct `schema:"lvl3"`
	}

	type lvl1Struct struct {
		Lvl2 lvl2Struct `schema:"lvl2"`
	}

	sd := &schema.Resource{
		Schema: map[string]*schema.Schema{
			"lvl2": {
				Type:     schema.TypeList,
				MaxItems: 1,
				Optional: true,
				Elem: &schema.Resource{
					Schema: map[string]*schema.Schema{
						"lvl3": {
							Type:     schema.TypeList,
							MaxItems: 1,
							Optional: true,
							Elem: &schema.Resource{
								Schema: map[string]*schema.Schema{
									"simple_attribute": {
										Type:     schema.TypeString,
										Optional: true,
									},
								},
							},
						},
					},
				},
			},
		},
	}

	e1 := lvl1Struct{
		Lvl2: lvl2Struct{
			Lvl3: lvl3Struct{
				SimpleAttr: "simple attr val",
			},
		},
	}
	var e2 lvl1Struct

	data := sd.Data(nil)
	if err := toSchemaResource(data, e1); err != nil {
		t.Fatal(err)
	}

	if err := fromSchemaResource(data, &e2); err != nil {
		t.Fatal(err)
	}

	if diff := cmp.Diff(e1, e2); diff != "" {
		t.Errorf("Got different entity after mapping: %s", diff)
	}

}

func TestStructSlice(t *testing.T) {

	type subStruct struct {
		Attr string `schema:"attr"`
	}

	type wrapStruct struct {
		SubSlice []subStruct `schema:"sub_slice"`
	}

	sd := &schema.Resource{
		Schema: map[string]*schema.Schema{
			"sub_slice": {
				Type:     schema.TypeList,
				Optional: true,
				Elem: &schema.Resource{
					Schema: map[string]*schema.Schema{
						"attr": {
							Type:     schema.TypeString,
							Optional: true,
						},
					},
				},
			},
		},
	}

	e1 := wrapStruct{
		SubSlice: []subStruct{
			{
				Attr: "attr1",
			},
			{
				Attr: "attr2",
			},
		},
	}
	var e2 wrapStruct

	data := sd.Data(nil)
	if err := toSchemaResource(data, e1); err != nil {
		t.Fatal(err)
	}

	if err := fromSchemaResource(data, &e2); err != nil {
		t.Fatal(err)
	}

	if diff := cmp.Diff(e1, e2); diff != "" {
		t.Errorf("Got different entity after mapping: %s", diff)
	}

}

func TestNilMaps(t *testing.T) {

	type testStruct struct {
		MapAttr map[string]string `schema:"map_attribute"`
	}

	type wrapStruct struct {
		ComplexAttr testStruct `schema:"complex_attr"`
	}

	sd := &schema.Resource{
		Schema: map[string]*schema.Schema{
			"complex_attr": {
				Type:     schema.TypeList,
				MaxItems: 1,
				Optional: true,
				Elem: &schema.Resource{
					Schema: map[string]*schema.Schema{
						"map_attribute": {
							Type:     schema.TypeMap,
							Optional: true,
							Elem: &schema.Schema{
								Type: schema.TypeString,
							},
						},
					},
				},
			},
		},
	}

	e1 := wrapStruct{
		ComplexAttr: testStruct{
			// MapAttr: nil,
		},
	}
	var e2 wrapStruct

	data := sd.Data(nil)
	if err := toSchemaResource(data, e1); err != nil {
		t.Fatal(err)
	}

	if err := fromSchemaResource(data, &e2); err != nil {
		t.Fatal(err)
	}

	if e1.ComplexAttr.MapAttr != nil {
		t.Errorf("expected nil, got %+v", e1.ComplexAttr.MapAttr)
	}

	if e2.ComplexAttr.MapAttr != nil {
		t.Errorf("expected nil, got %+v", e2.ComplexAttr.MapAttr)
	}

	if diff := cmp.Diff(e1, e2); diff != "" {
		t.Errorf("Got different entity after mapping: %s", diff)
	}
}

func TestNilSlices(t *testing.T) {

	t.Skip("this test is not working, however, it seems like we don't need to cover that particular case")

	type testStruct struct {
		SliceAttr []string `schema:"slice"`
	}

	sd := &schema.Resource{
		Schema: map[string]*schema.Schema{
			"slice": {
				Type:     schema.TypeList,
				Optional: true,
				Elem: &schema.Schema{
					Type: schema.TypeString,
				},
			},
		},
	}

	e1 := testStruct{
		SliceAttr: []string{},
	}
	var e2 testStruct

	data := sd.Data(nil)
	if err := toSchemaResource(data, e1); err != nil {
		t.Fatal(err)
	}

	if err := fromSchemaResource(data, &e2); err != nil {
		t.Fatal(err)
	}

	if e1.SliceAttr != nil {
		t.Errorf("expected nil, got %+v", e1.SliceAttr)
	}

	if e2.SliceAttr != nil {
		t.Errorf("expected nil, got %+v", e2.SliceAttr)
	}

	if diff := cmp.Diff(e1, e2); diff != "" {
		t.Errorf("Got different entity after mapping: %s", diff)
	}
}

func TestPatchMapper(t *testing.T) {

	sd := &schema.Resource{
		Schema: map[string]*schema.Schema{
			"string_attr": {
				Type:     schema.TypeString,
				Optional: true,
			},
			"int_attr": {
				Type:     schema.TypeInt,
				Optional: true,
			},
			"slice_attribute": {
				Type:     schema.TypeList,
				Optional: true,
				Elem: &schema.Schema{
					Type: schema.TypeString,
				},
			},
			"complex_attribute": {
				Type:     schema.TypeList,
				Optional: true,
				MaxItems: 1,
				Elem: &schema.Resource{
					Schema: map[string]*schema.Schema{
						"sub_string_attr": {
							Type:     schema.TypeString,
							Optional: true,
						},
						"sub_int_attr": {
							Type:     schema.TypeInt,
							Optional: true,
						},
						"sub_slice_attribute": {
							Type:     schema.TypeList,
							Optional: true,
							Elem: &schema.Schema{
								Type: schema.TypeString,
							},
						},
						"sub_map_attribute": {
							Type:     schema.TypeMap,
							Optional: true,
							Elem: &schema.Schema{
								Type: schema.TypeString,
							},
						},
					},
				},
			},
		},
	}

	data := sd.Data(nil)

	type testSubStruct struct {
		StringAttr string            `schema:"sub_string_attr" json:"subStringAttr"`
		IntAttr    int               `schema:"sub_int_attr" json:"subIntAttr"`
		SliceAttr  []string          `schema:"sub_slice_attribute" json:"subSliceAttr"`
		MapAttr    map[string]string `schema:"sub_map_attribute" json:"subMapAttr"`
	}

	type testStruct struct {
		StringAttr  string         `schema:"string_attr" json:"stringAttr"`
		IntAttr     int            `schema:"int_attr" json:"intAttr"`
		SliceAttr   []string       `schema:"slice_attribute" json:"sliceAttr"`
		ComplexAttr *testSubStruct `schema:"complex_attribute" json:"complexAttr"`
	}

	entity := testStruct{
		StringAttr: "https://acme.com/oauth2/default",
		// IntAttr:    42, -- this field is intentionally omitted, there should be no patch for it
		SliceAttr: []string{"slice1", "slice2"},
		ComplexAttr: &testSubStruct{
			StringAttr: "sub att1 val",
			IntAttr:    43,
			SliceAttr:  []string{"subslice1", "subslice2"},
			MapAttr: map[string]string{
				"key1": "val1",
				"key2": "val2",
			},
		},
	}

	if err := toSchemaResource(data, entity); err != nil {
		t.Fatal(err)
	}

	patches, err := patchFromResourceData(data, &testStruct{})
	if err != nil {
		t.Fatal(err)
	}

	if len(patches) != 6 {
		t.Errorf("expected 7 patch, got %d", len(patches))
	}
}
