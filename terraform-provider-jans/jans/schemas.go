package jans

import (
	"context"
	"fmt"
)

type SchemaAttribute struct {
	Name            string            `schema:"name" json:"name"`
	Type            string            `schema:"type" json:"type"`
	MultiValued     bool              `schema:"multi_valued" json:"multiValued"`
	Description     string            `schema:"description" json:"description"`
	Required        bool              `schema:"required" json:"required"`
	CanonicalValues []string          `schema:"canonical_values" json:"canonicalValues"`
	CaseExact       bool              `schema:"case_exact" json:"caseExact"`
	Mutability      string            `schema:"mutability" json:"mutability"`
	Returned        string            `schema:"returned" json:"returned"`
	Uniqueness      string            `schema:"uniqueness" json:"uniqueness"`
	ReferenceTypes  []string          `schema:"reference_types" json:"referenceTypes"`
	SubAttributes   []SchemaAttribute `schema:"sub_attributes" json:"subAttributes"`
}

type Schema struct {
	Schemas     []string          `schema:"schemas" json:"schemas,omitempty"`
	ID          string            `schema:"id" json:"id,omitempty"`
	Meta        Meta              `schema:"meta" json:"meta,omitempty"`
	Name        string            `schema:"name" json:"name,omitempty"`
	Description string            `schema:"description" json:"description,omitempty"`
	Attributes  []SchemaAttribute `schema:"attributes" json:"attributes,omitempty"`
}

// GetScimAppConfiguration returns the current SCIM App configuration.
func (c *Client) GetSchemas(ctx context.Context) ([]Schema, error) {

	type Response struct {
		Schemas      []string `json:"schemas"`
		TotalResults int      `json:"totalResults"`
		StartIndex   int      `json:"startIndex"`
		ItemsPerPage int      `json:"itemsPerPage"`
		Resources    []Schema `json:"Resources"`
	}
	ret := Response{}

	if err := c.getScim(ctx, "/jans-scim/restv1/v2/Schemas/", "", &ret); err != nil {
		return nil, fmt.Errorf("getScim request failed: %w", err)
	}

	return ret.Resources, nil
}

// // UpdateScimAppConfiguration updates the SCIM App configuration.
func (c *Client) GetSchema(ctx context.Context, id string) (*Schema, error) {

	if id == "" {
		return nil, fmt.Errorf("id is required")
	}

	ret := &Schema{}

	if err := c.getScim(ctx, "/jans-scim/restv1/v2/Schemas/"+id, "", ret); err != nil {
		return nil, fmt.Errorf("getScim request failed: %w", err)
	}

	return ret, nil
}
