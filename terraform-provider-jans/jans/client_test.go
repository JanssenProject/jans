package jans

import (
	"context"
	"errors"
	"os"
	"testing"
)

var (
	host = ""
	user = ""
	pass = ""
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
