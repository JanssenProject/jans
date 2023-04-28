# Quick Start Using Agama Lab

In this quick start guide, we will see how to build, deploy and test a simple password based authentication flow using
Agama and [Agama Lab](https://cloud.gluu.org/agama-lab). 

Agama is a component of the Janssen authentication server that executes web-based person-authentication flows. 
[Agama Lab](https://cloud.gluu.org/agama-lab) is an online tool that give easy to use visual editor to build these flows
. Agama Lab bundles authentication flow in form of projects that are packaged as a `.gama` file. This project file then
gets deployed on Janssen Server where Agama engine will execute the flow when authentication request is received.

Major Steps involved in this process are:
- Configure Janssen Server to enable Agama engine
- Designing a flow using Agama labs
- Deploying a flow on Janssen Server instance
- Testing the flow

## Prerequisites

- Need to have a Janssen Server instance

## Enable Agama using TUI

- Access TUI
- Make sure Agama engine is enabled ( )

## Design Agama flow

Below Agama flow is for simple username-password based user authentication. This flow can be [created using Agama Labs
as well]().

## Deploy Agama Project

## Test


## Notes:

- more details about inputs given on this screen: https://github.com/GluuFederation/private/wiki/Agama-Lab-Quick-Start-Guide#2
  like what are the available values and how to use them
- give a flow chart that depicts auth journey