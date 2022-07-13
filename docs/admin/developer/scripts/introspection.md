# Introspection Script Guide

## Overview

Introspection scripts allows to modify response of Introspection Endpoint [spec](https://datatracker.ietf.org/doc/html/rfc7662).

## Interface

### Methods

Introspection script should be associated with client (used for obtaining the token) in order to be run. Otherwise it's possible to set introspectionScriptBackwardCompatibility global AS configuration property to true, in this case AS will run all scripts (ignoring client configuration).

### Objects

Definitions of all objects used in the script

## Common Use Cases

Descriptions of common use cases for this script, including a code snippet for each
