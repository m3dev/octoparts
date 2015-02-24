# Java client

This simple client uses Async HTTP Client for transport. It is build for Java 8 but should compile on Java 7 too.

*It will not run on Java 6*

See Sample.java for usage

Features :

* It is possible to pass a HTTP client config to the `OctopartsApiBuilder` constructor.
* Two different timeouts can be set : one for the server, one for the client
* Broken responses are recognizable as their responseMeta are `null`
* When retrieving a missing/broken part response, its partId will be `null`