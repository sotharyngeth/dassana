This workflow adds generic context to an alert. By generic context, we mean a context that you can generally apply to any cloud resource even without knowing the resource type. Take for example "s3 bucket open to the internet" alert, in generic terms, you should not even care that the alert is for an s3 bucket let alone that it is open to the internet.

What you should care about is the account id the s3 bucket belongs to, which AWS Org does the account belong to, tags associated with the resource.

In other words, this workflow should get run for almost any cloud resource (assuming, of course, normalization workflow has run).

After all of the steps defined in the workflow have been run, the risk is set using the configuration defined in the `risk-config` section of the workflow. Have a look at the [AWS general context workflow](https://github.com/dassana-io/dassana/blob/main/content/workflows/csp/aws/general-context/general-context.yaml).
