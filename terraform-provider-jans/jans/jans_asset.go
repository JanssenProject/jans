package jans

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"io"
)

type PagedResult[T any] struct {
	Start           int `json:"start"`
	TotalEntryCount int `json:"total_entries_count"`
	EntriesCount    int `json:"entries_count"`
	Entries         []T `json:"entries"`
}

type Document struct {
	Dn                 string   `schema:"dn" json:"dn"`
	Inum               string   `schema:"inum" json:"inum"`
	DisplayName        string   `schema:"display_name" json:"displayName"`
	Description        string   `schema:"description" json:"description"`
	Document           string   `schema:"document" json:"document"`
	CreationDate       string   `schema:"creation_date" json:"creationDate"`
	JansFilePath       string   `schema:"jans_file_path" json:"jansFilePath"`
	JansModuleProperty []string `schema:"jans_module_property" json:"jansModuleProperty"`
	JansLevel          string   `schema:"jans_level" json:"jansLevel"`
	JansRevision       string   `schema:"jans_revision" json:"jansRevision"`
	JansEnabled        bool     `schema:"jans_enabled" json:"jansEnabled"`
	JansAlias          string   `schema:"jans_alias" json:"jansAlias"`
	Selected           bool     `schema:"selected" json:"selected"`
	BaseDn             string   `schema:"base_dn" json:"baseDn"`
}

type AssetForm struct {
	Document  Document `schema:"document" json:"document"`
	AssetFile []byte   `schema:"asset_file" json:"assetFile"`
}

func assetFormFromDocAndFile(doc Document, file io.Reader) (*AssetForm, error) {
	b, err := io.ReadAll(file)
	if err != nil {
		return nil, fmt.Errorf("failed to read file: %w", err)
	}

	data := AssetForm{
		Document:  doc,
		AssetFile: b,
	}

	return &data, nil
}

func (c *Client) createJansAssetData(doc Document, file io.Reader) (map[string]FormField, error) {
	data := map[string]FormField{}

	b, err := json.Marshal(doc)
	if err != nil {
		return nil, fmt.Errorf("failed to marshal request: %w", err)
	}

	r := bytes.NewReader(b)

	data["document"] = FormField{
		Typ:  "json",
		Data: r,
	}

	data["assetFile"] = FormField{
		Typ:  "file",
		Data: file,
	}

	return data, nil
}

func (c *Client) CreateJansAsset(ctx context.Context, doc Document, file io.Reader) (*Document, error) {

	data, err := c.createJansAssetData(doc, file)
	if err != nil {
		return nil, fmt.Errorf("failed to create asset data: %w", err)
	}

	resp := &Document{}
	req, err := c.newParams("POST", "/jans-config-api/api/v1/jans-assets/upload", resp,
		c.withToken(ctx, "https://jans.io/oauth/config/jans_asset-write"),
		c.withFormData(data),
	)
	if err != nil {
		return nil, fmt.Errorf("failed to create request: %w", err)
	}

	if err := c.request(ctx, *req); err != nil {
		return nil, fmt.Errorf("request failed: %w", err)
	}

	return resp, nil
}

func (c *Client) UpdateJansAsset(ctx context.Context, doc Document, file io.Reader) (*Document, error) {
	data, err := c.createJansAssetData(doc, file)
	if err != nil {
		return nil, fmt.Errorf("failed to create asset data: %w", err)
	}

	resp := &Document{}
	req, err := c.newParams("PUT", "/jans-config-api/api/v1/jans-assets/upload", resp,
		c.withToken(ctx, "https://jans.io/oauth/config/jans_asset-write"),
		c.withFormData(data),
	)
	if err != nil {
		return nil, fmt.Errorf("failed to create request: %w", err)
	}

	if err := c.request(ctx, *req); err != nil {
		return nil, fmt.Errorf("request failed: %w", err)
	}

	return resp, nil
}

func (c *Client) DeleteJansAsset(ctx context.Context, inum string) error {
	token, err := c.getToken(ctx, "https://jans.io/oauth/config/jans_asset-delete")
	if err != nil {
		return fmt.Errorf("failed to get token: %w", err)
	}

	if err := c.delete(ctx, "/jans-config-api/api/v1/jans-assets/"+inum, token); err != nil {
		return fmt.Errorf("delete request failed: %w", err)
	}

	return nil
}

func (c *Client) GetJansAsset(ctx context.Context, inum string) (*Document, error) {
	token, err := c.getToken(ctx, "https://jans.io/oauth/config/jans_asset-read")
	if err != nil {
		return nil, fmt.Errorf("failed to get token: %w", err)
	}

	ret := &Document{}
	if err := c.get(ctx, "/jans-config-api/api/v1/jans-assets/"+inum, token, ret); err != nil {
		return nil, fmt.Errorf("get request failed: %w", err)
	}

	return ret, nil
}
