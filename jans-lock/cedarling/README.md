# Cedarling
The Cedarling is a performant local authorization service that runs the Rust Cedar Engine.
Cedar policies and schema are loaded at startup from a locally cached "Policy Store".
In simple terms, the Cedarling returns the answer: should the application allow this action on this resource given these JWT tokens.
"Fit for purpose" policies help developers build a better user experience.
For example, why display form fields that a user is not authorized to see?
The Cedarling is a more productive and flexible way to handle authorization.

## Rust Cedarling
Cedarling is written in the Rust programming language (folder `cedarling`). And you can import it into your project as a dependency.

## Cedarling bindings
We have support binding for this platforms:
- [ ] Python
- [ ] Wasm

