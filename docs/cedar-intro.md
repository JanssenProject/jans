---
tags:
  - administration
  - lock
  - authorization / authz
  - Cedar
  - Cedarling
---

# What is Cedar

[Cedar](https://www.cedarpolicy.com/en) is a policy syntax invented by Amazon and used by their 
[Verified Permission](https://aws.amazon.com/verified-permissions/) service. Cedar policies
enable developers to implement fine-grain access control and externalize policies. To learn more
about why the design of Cedar is **intuitive**, **fast** and **safe**, read this 
[article](https://aws.amazon.com/blogs/security/how-we-designed-cedar-to-be-intuitive-to-use-fast-and-safe/) 
or watch this [video](https://www.youtube.com/watch?v=k6pPcnLuOXY&t=1779s)

Cedar uses the **PARC** syntax: 

* **P**rincipal
* **A**ction
* **R**esource
* **C**ontext 

For example, you may have a policy that says *Admins* can *write* to the */config* folder. The *Admin* role 
is the Principal, *write* is the Action, and the */config* folder is the Resource. The Context is used to 
specify information about the enivironment, like the time of day or network address.

![](../assets/lock-cedarling-diagram-3.jpg)

Fine grain access control makes sense in both the frontend and backend. In the frontend, mastery of 
authz can help developers build better UX. For example, why display form fields a user is not 
authorized to see? In the backend, fine grain policies are necessary for a zero trust architecture.
