---
tags:
  - administration
  - installation
  - x-frame
  - header
---

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
Sites can use this to avoid click-jacking attacks, 
by ensuring that their content is not embedded into other sites.

There are two AS configuration properties related to `X-Frame-Options`:

- `xframeOptionsHeaderValue` - sets value of `X-Frame-Options` header. Default value is `SAMEORIGIN`. Possible values are: `SAMEORIGIN` or `DENY`.
- `applyXFrameOptionsHeaderIfUriContainsAny` - array of strings. If incoming request contains any string from this array it will attach `X-Frame-Options` header to response.

By default AS attaches `X-Frame-Options` header to all responses where request uri contains `.htm`. 
It means for all AS pages.
  
