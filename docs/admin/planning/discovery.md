---
tags:
  - administration
  - planning
---

In a federated identity topology, you can authenticate a person by redirecting
their browser to the correct IDP. But what happens when there is more then
one IDP? Before the person authenticates, how do you know to which IDP to send
them? This challenge is called "discovery", "where are you from", or "WAYF".

There are a few ways to handle discovery. Here are a few:

1. Display a list of logos and enable the end user to pick the IDP. If you
ever clicked on a social login button, you've experienced this approach.

2. Use a different URL for each IDP, like `https://customer1.saas.com` or
`https://saas.com/customer2`. Based on which URL the person hits, you know
where to send them.

3. Prompt the person to enter something identifying, like their email address.
You can use this to look up their home IDP.

4. Use contextual information, like network address, geolocation, or browser
data.

Once you've figured out the person behind the browser, it may make sense to
write a cookie so next time, you don't have to bother them.

While it's possible to implement a discovery workflow using an Agama script
or an Auth Server person authentication interception script, you also write
a simple web application, and then redirect to the authorization endpoint
with a hint as an extra parameter and a specific `acr_values`. 
