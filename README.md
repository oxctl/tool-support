# Canvas Proxy

This is a simple webapp that just takes a HTTP request and proxies it through to Canvas. This allows a frontend webapp to be able to make XHR requests to Canvas. It should support multiple developer keys so that it can support multiple applications using it. It needs to request tokens for users, then maybe hand them back to the client (HTML/JS) so that they can be stored in local storage. That way we don't need any persistence server side, although we don't want to pass 2 tokens so we should probably re-wrap the token from Canvas with our own so that we don't rely on the internal structure of the token we get back from canvas.

To start with we should just persist the tokens in a DB.

## Status

There is a small proxy controller that sends requests on the Canvas with a hard coded token. Headers get passed through and responses get passed back. At the moment there seems to be a bug with HTML responses. There's no authentication.


Lifetime on tokens is currently pretty low. JS OAuth library?

