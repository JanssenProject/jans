package main

import (
	"fmt"
	"github.com/JanssenProject/jans/jans-cedarling/cedarling_opa/plugins"
	"github.com/open-policy-agent/opa/cmd"
	"os"
)

func main() {
	plugins.Register()
	if err := cmd.RootCommand.Execute(); err != nil {
		fmt.Fprintln(os.Stderr, err)
		os.Exit(1)
	}
}
