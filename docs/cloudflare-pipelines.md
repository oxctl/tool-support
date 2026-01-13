# Cloudflare Pipelines

Cloudflare Pipelines is a new service that allows realtime consumption of data. We are using this in tool-support to publish LTI launch events. The data is then collected into Parquet files in a Cloudflare R2 bucket for later analysis.

## Setup

First a bucket needs to be created in R2 to hold the Parquet files. This can be done through the Cloudflare CLI. Pipelines at the moment doesn't support region specific buckets for R2.

```bash
wrangler r2 bucket create tool-support
```

Next a pipeline is created.

```bash
wrangler pipelines streams create tool_support_stream --schema-file schema.json

wrangler pipelines sinks create tool_support_sink  --bucket  tool-support --type r2 --roll-interval 86400 --path beta
 
wrangler pipelines create tool_support --sql 'INSERT INTO tool_support_sink SELECT * FROM tool_support_stream;'
```