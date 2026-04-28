package main

import (
	"fmt"
	"os"

	"github.com/JanssenProject/jans/jans-cedarling/cedarling_opa/builtins"
	"github.com/JanssenProject/jans/jans-cedarling/cedarling_opa/plugins"
	"github.com/open-policy-agent/opa/cmd"
)

func main() {
	builtins.Register()
	plugins.Register()
	if err := cmd.RootCommand.Execute(); err != nil {
		fmt.Fprintln(os.Stderr, err)
		os.Exit(1)
	}
}
