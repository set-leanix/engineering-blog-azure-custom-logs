# Azure Custom Logs at LeanIX

> This is complementary code to a blog post at https://engineering.leanix.net/

## Motivation

* Application Performance Management (APM) is great, we use it as well but there are some places where APM solutions don't dig deep enough
* So it's an accompanying data source to Application Performance Management (APM)
* Use cases: 
  * Tracing request insights on domain level
  * Interval-based statistics like workspace statistics
  * Silence releases
  * Transaction-level analysis
* Along with Kusto it's awesome!

## Capabilities

* Best practises how to build and produce Azure custom logs
* Support for multiple custom logs with common set of attributes
* Support for system-wide attributes
* Non-blocking sending
