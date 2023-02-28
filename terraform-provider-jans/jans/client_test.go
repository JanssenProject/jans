package jans

import (
	"context"
	"errors"
	"os"
	"testing"
)

var (
	host              = ""
	user              = ""
	pass              = ""
	skipKnownFailures = false
)

func TestMain(m *testing.M) {

	// extract from docker with `cat /opt/jans/jans-setup/setup.properties.last | grep jca`

	host = os.Getenv("JANS_URL")
	user = os.Getenv("JANS_CLIENT_ID")
	pass = os.Getenv("JANS_CLIENT_SECRET")

	os.Exit(m.Run())
}

func TestClient(t *testing.T) {

	client, err := NewInsecureClient(host, user, pass)
	if err != nil {
		t.Fatal(err)
	}

	ctx := context.Background()

	accessToken, err := client.getToken(ctx, "https://jans.io/oauth/jans-auth-server/config/properties.readonly")
	if err != nil {
		t.Fatal(err)
	}

	if accessToken == "" {
		t.Fatal("access token is empty")
	}

	ctx, cancel := context.WithCancel(ctx)
	cancel()

	_, err = client.getToken(ctx, "https://jans.io/oauth/jans-auth-server/config/properties.readonly")
	if !errors.Is(err, context.Canceled) {
		t.Fatal(err)
	}

}

func TestSortArrays(t *testing.T) {

	cfg := AppConfiguration{
		AuthorizationEncryptionEncValuesSupported: []string{
			"A128CBC+HS256",
			"A256CBC+HS512",
			"A128GCM",
			"A256GCM",
		},
		AuthorizationRequestCustomAllowedParameters: []CustomAllowedParameter{
			{
				ParamName:        "customParam2",
				ReturnInResponse: true,
			},
			{
				ParamName:        "customParam1",
				ReturnInResponse: false,
			},
			{
				ParamName:        "customParam3",
				ReturnInResponse: false,
			},
		},
	}

	sortArrays(&cfg)

	if cfg.AuthorizationEncryptionEncValuesSupported[0] != "A128CBC+HS256" ||
		cfg.AuthorizationEncryptionEncValuesSupported[1] != "A128GCM" ||
		cfg.AuthorizationEncryptionEncValuesSupported[2] != "A256CBC+HS512" ||
		cfg.AuthorizationEncryptionEncValuesSupported[3] != "A256GCM" {
		t.Errorf("unexpected value in AuthorizationEncryptionEncValuesSupported: %#v", cfg.AuthorizationEncryptionEncValuesSupported)
	}

	if cfg.AuthorizationRequestCustomAllowedParameters[0].ParamName != "customParam1" ||
		cfg.AuthorizationRequestCustomAllowedParameters[1].ParamName != "customParam2" ||
		cfg.AuthorizationRequestCustomAllowedParameters[2].ParamName != "customParam3" {
		t.Errorf("unexpected value in AuthorizationRequestCustomAllowedParameters: %#v", cfg.AuthorizationRequestCustomAllowedParameters)
	}

	arr := []AdminUIRolePermissionMapping{
		{
			Role: "admin",
			Permissions: []string{
				"permission3",
				"permission1",
				"permission2",
			},
		},
		{
			Role: "user",
			Permissions: []string{
				"permission2",
				"permission3",
				"permission1",
			},
		},
	}

	sortArrays(&arr)

	for _, v := range arr {
		if v.Permissions[0] != "permission1" ||
			v.Permissions[1] != "permission2" ||
			v.Permissions[2] != "permission3" {
			t.Errorf("unexpected value in Permissions: %#v", v.Permissions)
		}
	}
}
