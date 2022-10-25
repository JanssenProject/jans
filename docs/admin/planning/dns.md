---
tags:
  - administration
  - planning
  - DNS
  - hostname
  - TLS
  - X.509
---

Trust on the Internet is largely derived from TLS. Like it or not, we are highly
reliant on making a secure connection from a web browser using X.509
security to ensure that we are connecting to a trusted web server. The `cn` of
that web server certificate is the hostname of the server. There is no guarantee
that a two distinct trusted root CA's have not issued duplicate certificates for
the same hostname. Thus, if your DNS server is hacked, bad things may ensue.
This is not a hypothetical risk. Actual attacks have been implemented by adding
entries to a DNS server that look real, but point to the attacker's website.
DNS attacks are very difficult for the end-user to detect. For example,
`https://account.acme.com` v. `https://accounts.acme.com`--both would look ok
to a typical end user. So rule number one: PROTECT DNS--the whole integrity of
your identity platform is otherwise vulnerable.

From a deployment perspective, remember that in most cases the hostname of your
IDP points to the HTTP load balancer (unless you have a single VM deployment).
In most cases, you will also terminate TLS in the load balancer.

The DNS hostname is baked into the protocols. For example, the authorization
endpoint may look like this:

```
"authorization_endpoint" : "https://account.acme.com/jans-auth/restv1/authorize"
```

Note that the hostname is built into the protocol, not just the Web TLS layer.
If you were to change the hostname of your web server, your deployment would
break, because you would have to update all the configuration data in the IDP
that contains the hostname too. And you'd have to regenerate any X.509
certificates.  For this reason, you should think carefully about the
hostname before you run setup and create the base installation of your Janssen
Platform. To change the hostname, we recommend you re-install. If you want to
promote your configuration from development to production, you may also need to
consider keeping the hostname the same--although be careful you don't hit
production by mistake in your testing.
