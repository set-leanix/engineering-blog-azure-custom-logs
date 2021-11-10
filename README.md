# Azure Log Analytics and Data Collector API for JVM backend services

> Azure Log Analytics and Custom Logs are important components at LeanIX to get visibility beyond the reach of standard APM solutions. Let's look into some use cases and how we've integrated it into our JVM backend.    

## Motivation

* Application Performance Management (APM) is great, we use it as well but there are some places where APM solutions don't dig deep enough
* So it's an accompanying data source to Application Performance Management (APM)
* Kusto makes it a powerful tool for Application insights
* Main use cases: 
  * Tracing request insights on domain level
  * Interval-based statistics like workspace statistics
  * Silence releases
  * Transaction-level analysis

* Our little Java-based library
  * Production-proven code to build and produce Azure custom logs
  * Support for multiple custom logs with common set of attributes
  * Support for system-wide attributes
  * Non-blocking sending
