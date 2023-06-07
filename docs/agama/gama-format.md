---
tags:
  - administration
  - developer
  - agama
---

# The `.gama` file format

In practice, a web flow will make use of a bunch of artifacts, like UI pages, images, stylesheets, and source code. Actually, to solve a real-world problem several flows are needed to be able to keep flexibility and complexity at acceptable levels. Here is where the concept **project** emerges.

A project can be thought of as a container to hold all flows and related assets aimed at solving a particular problem - including metadata of the project itself. The idea of defining a standard way to specify projects brings several benefits:

- Provide a uniform conceptual scheme for community actors to interchange flows 
- Provide Agama engine implementors common ground for the materialization of flows deployment
- Serve as reference for developers interested in coding tools such as an Agama IDE

## Anatomy of a project

The below shows the structure of an Agama project:

```
├── code/
├── lib/           
├── web/
├── project.json   
├── LICENSE        
└── README.md
```

- `code` directory holds all flows part of the project. Every flow - implemented in Agama language - has to reside in a separate file with extension `flow` and with file name matching the qualified name of the flow in question. This directory can have nested folders if desired  
- `lib` may contain source code files in languages other than Agama. Every engine can make use of the contents of this folder as needed  
- `web` is expected to hold all UI templates plus required web assets (stylesheets, images, etc.) that all flows in this project may use
- `project.json` file contains metadata about this project. More on this later
- `README.md` file may contain extra documentation in markdown format
- `LICENSE` file may contain legal-related information

Except for `code` and `web` directories, all elements in the file structure above are optional. Note that files in `web` must follow a directory structure that is consistent with respect to `Basepath` and `RRF` directives found in the included flows.

### Metadata

`project.json` file is expected to contain metadata about the contents of the project in JSON format. This is an example:

```
{
  "projectName": "A unique name that will be associated to this project",
  "author": "A user handle that identifies you",
  "description": "Other relevant data can go here",
  "type": "",
  "configs": {
    "com.foods.sweet": {
        "prop1": "secret",
        "prop2": [1, 2, 3]
    }
  },
  "noDirectLaunch": [ "test" ]
}
```

The `configs` section is a JSON object containing exemplifying [configuration properties](./language-reference.md#header-basics) for flows that may need them. Note `.gama` files **must not** contain **real** configuration properties because these files can be freely distributed; in practice, configurations hold sensitive data that should not be exposed. The keys of object `configs`, if any, are qualified flow names already part of the project.  

The `noDirectLaunch` section is an array holding zero or more qualified flow names. Use this list to prevent certain flows to be launched directly from a web browser. This is a security measure to avoid end-users triggering flows at will.

Other fields can be added in `project.json`.

## Sample project

As an example assume you want to deliver these two flows:

```
Flow test
    Basepath "hello"

in = { name: "John" }
RRF "templates/index.htm" in

Log "Done!"
Finish "john_doe"
```

```
Flow com.foods.sweet
    Basepath "recipes/desserts"

...

choice = RRF "selection.htm"
list = Call com.foods.RecipeUtils#retrieveIngredients choice.meal
...
```

Here is how the project folder might look like:

```
├── code/
│   └── test.flow
│   └── com.foods.sweet.flow
├── web/
│   ├── hello/
|   |   └── templates/
│   |       └── index.htm
│   |           └── js/
│   |               └── font-awesome.js
|   └── recipes/
│       └── desserts/
│           └── selection.htm
│           └── logo.png
├── lib/
│   └── com/
│       └── foods/
│           └── RecipeUtils.java
└── project.json
```

## `.gama` file

Dealing with a folder this way can be awkward for sharing and other tasks. Instead, if the project contents are compressed using the ZIP file format, we have what is known as `.gama` file. Thus, a `.gama` file is a project archive that can be shared and deployed to an Agama engine.

## Project deployment

The actual "deployment" of an Agama project is an engine-specific detail. Engines must offer mechanisms for administrators and developers to "install" `.gama` files.
