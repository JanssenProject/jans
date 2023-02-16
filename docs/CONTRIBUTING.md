# Contributing to Janssen Project

Purpose of this guide is to provide necessary information and resources to community in order to make successful contribution to the Janssen Project.

There are many ways you can contribute. Of course you can contribute code. But we also need people to write documentation and guides, to help us with testing, to answer questions on the forums and chat, to review PR's, to help us with devops and CI/CD, to provide feedback on usability, and to promote the project through outreach. Also, by sharing metrics with us, we can gain valuable insights into how the software performs in the wild. 

- [Join the Community](#join-the-community)
- [First Time Contributors](#first-time-contributors)
- [Contribution Guidelines](#contribution-guidelines)
  - [Code of Conduct](#code-of-conduct)
  - [About Issues](#about-issues)
  - [Triaging](#triaging)
  - [Code Conventions and Guidelines](#code-conventions-and-guidelines)
    - [Commits](#commits)
    - [Branches](#branches)
    - [PRs](#prs)
    - [Issues](#issues)
    - [Contributing to the documentation](#contributing-to-the-documentation)
- [Contribution Workflow](#contribution-workflow)
  - [Find Something To Work On](#find-something-to-work-on)
  - [Start a Discussion](#start-a-discussion)
  - [Implement the Change](#implement-the-change)
  - [Document](#document)
  - [Raise a PR](#raise-a-pr)
  - [Follow Through](#follow-through)
  

# Join the Community

* **Repo**: Watch and Star [Janssen repository](https://github.com/JanssenProject/jans) on Github
* **Discussions**: Join interesting discussions at [Github Discussions](https://github.com/JanssenProject/jans/discussions)
* **Chat**: We have an active [community chat on Gitter](https://gitter.im/JanssenProject/Lobby). You can register for free their with your Github identity.
* **Tweet**: Janssen is on [Twitter](https://twitter.com/janssen_project) too. Follow us there to stay up to date on release announcements and news around Janssen.

# First Time Contributors

In case you are _**first-time**_ contributor, then you can start with our [good first issues list](https://github.com/JanssenProject/jans/issues?q=is%3Aissue+is%3Aopen++label%3A%22good+first+issue%22) These are issues where you can easily contribute and community members will guide and support your contribution as always.

If you need Janssen installation to test out your fix, here are the [steps](https://github.com/JanssenProject/jans/wiki#janssen-installation).


# Contribution Guidelines

We are really glad you are reading this, because we need volunteer developers to help this project come to fruition.

- [Code of Conduct](#code-of-conduct)
- [Issues](#issues)
- [Triaging](#triaging)
- [Coding Conventions](#code-conventions-and-guidelines)

## Code of Conduct

Janssen project has a
[Code of Conduct](CODE_OF_CONDUCT.md)
to which all contributors must adhere, please read it before interacting with the repository or the community in any way.

## About Issues

There are four kinds of issues you can open:

- **Bug report**: you believe you found a problem in a project and you want to discuss and get it fixed,
  creating an issue with the **bug report template** is the best way to do so.
- **Feature Request**: any kind of new feature need to be discussed in this kind of issue, do you want a new rule or a new feature? This is the kind of issue you want to open. Be very good at explaining your intent, it's always important that others can understand what you mean in order to discuss, be open and collaborative in letting others help you getting this done!
- **Security Vulnerability**: If you identify a security problem, please report it immediately, providing details about the nature, and if applicable, how to reproduce it. If you want to report an issue privately, you can email security@gluu.org
- **Failing tests**: you noticed a flaky test or a problem with a build? This is the kind of issue to triage that!

The best way to get **involved** in the project is through issues, you can help in many ways:

- Issues triaging: participating in the discussion and adding details to open issues is always a good thing,
  sometimes issues need to be verified, you could be the one writing a test case to fix a bug!
- Fix an issue: you can help in getting issue fixed in many ways. More often by opening a pull request. In case you are _**first-time**_ contributor, then you can start with our [good first issues list](https://github.com/JanssenProject/jans/issues?q=is%3Aissue+is%3Aopen++label%3A%22good+first+issue%22) These are issues where you can easily contribute and community members will guide and support your contribution as always.

## Triaging

Triage is a process of evaluating issues and PRs in order to determine their characteristics and take quick actions if possible.

When you triage an issue, you:

* assess whether it has merit or not

* quickly close it by correctly answering a question

* point the reporter to a resource or documentation answering the issue

* tag it via labels, projects, or milestones

* take ownership submitting a PR for it, in case you want 😇

Here is how we [continously triage](governance/triage.md) new issues and PRs so that contributors can contribute faster and better.


## Code Conventions and Guidelines

### Commits

Janssen Project mandates all commits to follow guidelines as below.

- **Commit messages**

  As commit convention, we adopt [Conventional Commits v1.0.0](https://www.conventionalcommits.org/en/v1.0.0/), we have an history
  of commits that do not adopt the convention but any new commit must follow it to be eligible for merge.

- **Add GPG signature to your commit**

  To ensure that contribution is coming for a trusted source, all commits should be signed using GPG key and verified by Github. If you have GPG key    setup already then just use `-S` switch with you commit to sign it. If you need to setup your GPG key and verification, then you can find detailed instructions [here](https://docs.github.com/en/authentication/managing-commit-signature-verification)

- **Add DCO sign-off**
  
  The [Developer Certificate of Origin (DCO)](https://developercertificate.org/) is a lightweight way for contributors to certify that they wrote or otherwise have the right to submit the code they are contributing to the project.

  Contributors to the Janssen project sign-off that they adhere to these requirements by adding a `Signed-off-by` line to commit messages.

  ```
  This is a commit message

  Signed-off-by: Foo Bar <foobar@spam.org>
  ```

  Git even has a `-s` command line option to append this automatically to your commit message:

  ```
  $ git commit -s -m 'This is my commit message'
  ```

In all, if you have your GPG verification setup, your commit command should look like 

```
git commit -S -s -m 'message that follows conventional commit style'
```

### Branches
Branch name should have component name as prefix, eg `jans-core-mybranch`

### PRs
- PR titles should also follow [Conventional Commits v1.0.0](https://www.conventionalcommits.org/en/v1.0.0/). This will help in keeping merge commit messages inline with commit message standards
- Squash commits into small number of cohesive commits before raising a PR
- PR should be rebased on main branch so that there are minimal or no conflicts at the time of merge
- PR should only have changes related to target feature or issue. Create a separate PR for formatting or other quick bug fixes
- PR should include relevent documentaton changes
- PR should include unit and integration tests

### Issues 
- Issue titles should follow [Conventional Commits v1.0.0](https://www.conventionalcommits.org/en/v1.0.0/)

### Backport changes to a different version

Backport changes are now supported through a [workflow](https://github.com/zeebe-io/backport-action#backport-action) through labels prefixed with `backport/`.
For-example to backport changes in a certain PR to version `v1.0.0` a label to that PR matching the version must be added i.e `backport/v1.0.0`.
The flow consists of creating a new branch, cherry-picking the changes of the original PR and creating a new PR to merge them.

# Contributing to the documentation
Great documentation is a reflection of software's maturity and the great community that stands behind it. Contributing to the Janssen Project documentation is the easiest way to learn about the Janssen Project and to get involved in the community process. 

In order to ensure consistency of style, language, format, and terminology across all documents, please follow the guidelines below:

## Glossary of terms

This glossary helps to keep terms and their meanings consistent across documentation.

- `Janssen Project` or `Jans`: 

  Refers to the official project name under Linux Foundation that seeks to build the world’s fastest and most comprehensive cloud native identity and access management software platform

- `Janssen Server`:

  Refers to a set of software components developed under the Janssen Project . Components of the Janssen Server include client and server implementations of the OAuth, OpenID Connect, SCIM and FIDO standards. The term `Janssen Server` is used to refer to these components as a group. 

- `jans-auth-server`: 

  Refers to a module within the Janssen Server named `jans-auth-server`. This is one of the significant modules of the Janssen Server that has an implementation for OAuth and OpenId Connect. 

- Janssen Server module names:

  For correct naming of other modules of the Janssen Server, please refer to [README](https://github.com/JanssenProject/jans#janssen-modules)

## Documentation Style Guide

Janssen Project documentation uses Markdown. Guidelines below are intended to bring consistency in writing and formatting documents. 

!!! Testing

    [Janssen Project documentation site](https://docs.jans.io) is published using MkDocs. Markdown parsers used by Github and the one used by MkDocs may have slight variations in how they generate HTML. So, for a small number of cases, document may look different between Github and [Janssen Project documentation site](https://docs.jans.io). Hence it is critical to [test documentation](developer/testing.md#documentation-local-testing) changes locally before pushing to repository. This will ensure that final HTML rendering of documents by MkDocs is as desired.

### Document Title

The document title summarises what the document aims to achieve and at the same time, it plays a critical role in making the document easy to find via search. Below are a few guidelines to write good titles for documents.

- Every document **must** start with a title. Meaning, `#<space><title text>`
- Title should summarise what the document is trying to achieve. For examples: `Integrating with the Apache mod_auth_openidc module`, `Integrating DUO's Universal Prompt as an authentication method`, `Install using Ubuntu Linux Package`
- Title should include its context. For example, the document under `Installation`>`VM Installation`>`Ubuntu` should not be titled as just `Ubuntu` but it should have a more detailed title similar to  `Install using Ubuntu Linux Package`. When required, to keep the title from becoming too long, assume that `Janssen Server` is already understood as context.
- Titles should be written using [title case](https://en.wikipedia.org/wiki/Title_case)

### Document Tags

Janssen Project documentation uses [tags](https://squidfunk.github.io/mkdocs-material/setup/setting-up-tags/#adding-tags) to make the search more accurate and add context to search results. Following are guidelines and examples to follow while adding tags to a document.

- Maximum 6 tags
- First three should establish the context of the section hierarchy under which the document belongs. See the example below.
- Remaining tags can be based on the content of the document.
- Each tag should be a single word (no spaces, hyphens or commas, etc)
- All tags should be in lowercase

Example:

Let's look at how to add tags to a document that is located on [documentation site](https://docs.jans.io/) at path `Administration -> Installation -> VM installation`. Also, assume that the document describes the steps to install Janssen Project on the Ubuntu platform. Tags below would be recommended:

```json
---
tags:
 - administration
 - installation
 - vm
 - ubuntu
---
```

### Referencing Janssen Project Release in Documents

We often need to reference release numbers in the documentation. For example, [Ubuntu package installation guide](admin/install/vm-install/ubuntu.md).
In this guide, the following command is documented:

```
wget https://github.com/JanssenProject/jans/releases/download/v1.0.5/jans_1.0.5.ubuntu20.04_amd64.deb -P /tmp
```

Above command contains references to the release number at two places. `v1.0.5` in the URL and `1.0.5` as part of the file
name. There are many such places throughout the documentation when release numbers need to be mentioned. Whenever we
make a new release, these numbers need to change as they point to the latest release number. This becomes a manual task.

To avoid this manual, error-prone approach the Janssen Project uses a release marker, `replace-janssen-version` instead
of writing actual release numbers in the `head`(latest) documentation branch. So, when there is a need to mention the release number, instead of
writing the actual release number, use the release marker. Let's see how to document the above command (at the `head` version)
so that it stays up-to-date release after release.

```
wget https://github.com/JanssenProject/jans/releases/download/vreplace-janssen-version/jans_replace-janssen-version.ubuntu20.04_amd64.deb -P /tmp
```

!!! Warning
- `head` version of documentation may contain the release marker at various places. URLs, commands etc. So, URLs, and commands
might not work as it is. Users will have to manually replace the marker or use the most recent stable release documentation.
- Release marker should be used only when contributing to the latest documentation. Not when updating documentation
for previous releases.

### General Text
 - Allow long lines to wrap, rather than manually breaking them. For example, the Introduction paragraph is a single line
 - Keep explanations short and clear
 - Use complete sentences when possible
 - To make text _italicised_, put an `_` on each side, like this: `_word_`
 - To **bold** text, put a double `*` on each end, like this: `**word**`
 - Leave a blank line between paragraphs. Count a header as a paragraph for this purpose
 - Avoid passive voice as much as possible. It's clearer to say that a subject does something than to say a result was done
 - Avoid using `you` in statements as much as possible. For example, instead of saying `You can navigate to...` simply say `Navigate to...` 
 
### Page Setup
 - Start your page with a title on the first line
 - Follow with a concise overview of the document / product's purpose
 - Organize the information in the document from least technical to most technical if possible. Start conceptual, then get detailed
 
### Lists
 - Leave a blank line between text and first item in the list
 - Only use a numbered list if the order of the list matters
 - A line of a list should not end with a period. If it's multiple sentences, like this one, drop the last period
 - Start each item in the list with a capital letter
 - End each item in the list with at least three spaces. This makes sure the line breaks properly
 - To make a *bulleted* list, start each line with `-`
 - To make a *numbered* list, start each line with `1.` For example:
 
    ```
    1. This is the first item
    1. This is the second item
    1. This is the third item
    ```
 
    It will look like this:

    ```
    1. This is the first item
    2. This is the second item
    3. This is the third item
    ```
 
 - To include additional lines in a list item, start the sub-line with four spaces. For example:
 
    ```
    1. This is the first item in a list   
       There are four spaces to start this line   
       Another four spaces here   
       This keeps all text inside the numbered list item, before starting...   

    1. The following list item   
    ```

It will look like this:

1. This is the first item in a list   
    There are four spaces to start this line   
    Another four spaces here   
    This keeps all text inside the list, before starting...    

1. The following list item   

#### Other formatting considerations

 - Admonitions cannot be nested inside a list. They must be aligned all the way left. Inserting them within a list will break the list sequence (starting back over from 1).
    
 - Nesting a [fenced block of code](#code-formatting) in a numbered list is more challenging, as the list and code block syntaxes clash. To nest a code block into a list, insert four spaces to the left of all lines of the formatting. For example:

```
1. This is the first item  
    ```
    This is code  
    This is also code.
    ```
1. This is the second item  
```

It will look like this:

1. This is the first item  
    ```
    This is code  
    This is also code.
    ```
1. This is the second item  


### Headings
 - Headings should be in title format. All important words should be capitalized
 - The main title of the page should start with a single `#`, then each level of subheading should add one. For example, the first subheading should start with `##`, a subheading of that should use `###`, and so on

### Code Formatting
  - To format text as code within a line of normal text, surround the code with a single backtick (\`).
  - If the code is to be on its own line, it should be a fenced code block. To make a fenced code block, make a line before and after the code with three backticks:
  
    ```
        ```
        This is code
        ```
    ```
    
  - We use the [SuperFences](https://facelessuser.github.io/pymdown-extensions/extensions/superfences/) plugin to enhance this functionality.
  
  
### Examples & Navigation
 - When possible, provide an example in the form of code output or a screenshot
 - To instruct a user to click a button, or navigate to a certain page or through a menu, use the following style:

     ```
     Navigate to `Configuration` > `Authentication` and click the `Passport` tab
     ```  
 
It will look like this:  
 
Navigate to `Configuration` > `Authentication` and click the `Passport` tab
 
### Linking

We recommend using relative linking syntax when linking to other artifacts in repository. Linking to a page within the same repo use this format: `[text for the link](../where/the/link/goes.md)`
 - You must link to the `.md` file on GitHub for it to work properly
 - As an example, to make text `this link` link to a Markdown document named `example.md` in the same directory, you'd type it as `[this link](./example.md)`
 
#### Service Commands 

The Janssen Server supports many different Operating Systems (e.g. Ubuntu, SUSE etc.). Service commands can vary. Rather than "hard coding" service commands into documentation, please instead reference the dedicated documentation page for [Service Commands](https://jans.io/docs/vm-ops/checking-service-status/). 

In documenting a process that involves a service restart, the Service Command documentation is linked:  

```  
## Add the attribute to LDAP

 - Add custom attribute 
 - [Restart](https://jans.io/docs/vm-ops/restarting-services/) the `jans-auth.service` service.
```

The word `Restart` is simply linked to the dedicated doc for Service Commands. 
 
### Tables
 - Try to make tables visually readable by spacing to make distinct columns
 - The header for each column must be separated by at least three dashes
 - Use outer pipes for consistency
 - If an entry is too long to fit in the neat boxes, that's fine, just try to keep it legible
 - An example table follows:

 ```
 |This    |Is     |A     |Table    |
 |--------|-------|------|---------|
 |1       |2      |3     |4        |
 |Word    |Code   |Text  |Table    |
```

It looks like this:

|This    |Is     |A     |Table    |
|--------|-------|------|---------|
|1       |2      |3     |4        |
|Word    |Code   |Text  |Table    |

### Help On Technical Writing

It is essential for everyone in the community to actively participate in the documentation. At the same time, not everyone is formally trained or experienced in writing technical documents. To help everyone understand the basics of good technical writing, we have listed a few resources below. Going through these resources will not take a lot of time and will help in writing better technical documents.

- [Introduction to Technical Writing (part-1)](https://developers.google.com/tech-writing/one)
- [Introduction to Technical Writing (part-2)](https://developers.google.com/tech-writing/two)

# Contribution Workflow

## Find something to work on
Best place to find something to work on is to look at [currently open issues](https://github.com/JanssenProject/jans/issues). If you are a first time contributor then starting with [list of good first issues](https://github.com/JanssenProject/jans/issues?q=is%3Aopen+is%3Aissue+label%3A%22good+first+issue%22) is best.

## Start a discussion
Start a [Github Discussion](https://github.com/JanssenProject/jans/discussions) about what you are planning to contribute. Explain the feature or issue that you are planning to contribute and what your solution or implementation approach. Janssen is a community driven project and it'll be helpful to get community's view about it.

## Create an issue
Take your time to write a proper issue including a good summary and description. Outcome of Github discussion about your contribution can help you create good content for the issue. As a first step when creating a new issue, an issue template has to be selected. Select appropriate `issue template` and it'll help you create an issue with right content.

Remember that issue may be the first thing a reviewer of your PR will look at to get an idea of what you are proposing. It will also be used by the community in the future to find about what new features and enhancements are included in new releases.

## Implement the change
All contributions to Janssen Project should be made via Github pull requests(PR). 

> New to PR workflow?? Learn and practice it at [first-contributions](https://github.com/firstcontributions/first-contributions)

### Create a Fork
Fork [Janssen repository](https://github.com/JanssenProject/jans). And create a clone.

### Implement the Change

Start working on changes as required. 

- Make sure the [code conventions](#code-conventions-and-guidelines) are being followed.
- Use static code analysis and linting tools to make sure the code is high-quality.
- Write tests first and then code. Ensure that integration tests that cover your code are appropriately updated and reviewed.
- Create PR early and push often. 
- Janssen uses Github actions to run automated checks on PR changes. Ensure that these checks are passing with every push.
- Engage PR reviewers at the start so that they can continue to reivew code as it is developed and in small chunks.
- For a change that is non-trivial(an enhancement or a new feature), design should be reviewed. This should be done via PR by adding appropriate code owners.

<!-- TODO: enable this once the workspace guide is ready and developers can test locally from workspace
## Test 

To run tests locally before pushing your code, refer to [TESTING] guide.
-->

## Document

- PR should include changes in relevant documentation along with code changes.  PR is checked by bot to have either one 
of the following :
  - A commit that follows [commit guidelines](#commits) with `docs:` message 
  - Changes in artifacts under `jans/docs`
  
- If PR does not need any documentation changes, then the developer needs to acknowledge that in one of two ways:
  - Add an empty commit to the PR (using `--allow-empty` git flag) with `docs:` message (i.e `docs: no doc changes required`)
  - Add footer to the commit message of one of the code commits with `docs:` message e.g
  ```
  fix: typo on class name

  More details here.

  docs: no docs modification
  ```
    

## Raise a PR
- Make sure that PR title follows these [guidelines](#prs)
- Janssen uses Github PR template. Template provides helpful instructions to ensure new PRs are complete in details and easy to review. Github will populate new PR's description with these instructions. You can edit PR description as per your requirements.
- When PR is raised, Github will automatically assign reviewer to the PR based changed files and [CODEOWNERS](https://github.com/JanssenProject/jans/blob/main/.github%2FCODEOWNERS) list.
- Once PR is raised, ensure that PR passes all the mandatory Github actions checks available on Github PR page. Github will not allow PR to be merged if any of the mandatory check is failing.

## Follow Through
Once the PR is raised, wait for reviewers to start review. Reviewers will start review at the first opportunity available. If you want to draw attention, give a gentle reminder in PR comments. But please be patient. 

Follow activities on your PR closely till the time PR is merged. PR reviewer may want to suggest a change or may need to ask a question to get more clarity. Make sure you are actively collaborating. Once Reviewer has completed the review and approved the changes, the PR will be merged.

Thats it!! Congratulations on successful contribution. 🥳 🤟
