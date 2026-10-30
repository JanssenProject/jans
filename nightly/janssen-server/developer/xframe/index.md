# X-Frame-Options Header

AS has `io.jans.as.server.filter.HeadersFilter` which is responsible for attaching headers to server responses.

```
	<filter-mapping>
		<filter-name>HeadersFilter</filter-name>
		<url-pattern />
	</filter-mapping>

```

## Configure X-Frame-Options Header

The `X-Frame-Options` HTTP response header can be used to indicate whether a browser should be allowed
to render a page in a `<frame>`, `<iframe>`, `<embed>` or `<object>`.
Sites can use this to avoid clickjacking attacks,
by ensuring that their content is not embedded into other sites.

There are two AS configuration properties related to `X-Frame-Options`:

- `xframeOptionsHeaderValue` - sets value of `X-Frame-Options` header. Default value is `SAMEORIGIN`. Possible values are: `SAMEORIGIN` or `DENY`.
- `applyXFrameOptionsHeaderIfUriContainsAny` - array of strings. If incoming request contains any string from this array it will attach `X-Frame-Options` header to response.

By default AS attaches `X-Frame-Options` header to all responses where request uri contains `.htm`.
It means for all AS pages.

## Configure Security Headers

`HeadersFilter` also attaches defense-in-depth headers to API responses:

- `Content-Security-Policy: default-src 'none'; frame-ancestors 'none'` - blocks execution of injected markup and clickjacking.
- `X-Content-Type-Options: nosniff`
- `Referrer-Policy: no-referrer` - keeps URL secrets from leaking.

Configuration properties:

- `securityHeadersEnabled` - boolean, default `true`. Set to `false` to disable the headers.
- `applySecurityHeadersIfUriContainsAny` - array of strings. If the request uri contains any string from this array, AS attaches the headers to the response.
  Default value: `/restv1/token`, `/restv1/userinfo`, `/restv1/introspection`, `/restv1/jwks`, `/restv1/register`, `/restv1/revoke`.

Do not add endpoints that RPs must frame (for example `check_session_iframe`) or normal HTML pages to this list.
