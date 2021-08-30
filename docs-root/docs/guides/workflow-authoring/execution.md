# Workflow Execution

Please familiarize yourself with the following diagram first and then read the rest of the document.

![foo](/img/guides/authoring/engine-execution.svg)

:::info
The following concepts apply to **ALL** types of workflows. Each workflow type can have its specific fields and processing logic. You can read workflow type-specific information based on the
workflow type here:
[`normalize`](./normalize)
[`general-context`](./general-context)
[`resource-context`](./resource-context)
[`policy-context`](./policy-context)

:::

## Filtering

In the above flow chart, refer to the boxes which read _Find applicable workflows (workflow-type), if any_.
The "Finding" part is achieved by filtering.

Let's say you have the following JSON:

```json
{
	"foo": "bar"
}
```

And there are three workflows (_we are showing only the relevant parts of the workflows below_):

```yaml title="1.yaml"
filters:
    - match-type: all
      rules:
          - .foo | contains ("bar")
```

```yaml title="2.yaml"
filters:
    - match-type: all
      rules:
          - .foo | contains ("b")
```

```yaml title="3.yaml"
filters:
    - match-type: all
      rules:
          - .foo | contains ("baz")
```

Which workflow will get run? Clearly, the `3.yaml` won't. But what about 1 and 2? There is no priority order, so either one will match.

:::caution
Workflows don't have priority order, so make sure to write filters as constrained as possible.
:::

## Step Execution

:::note
Steps are optional. It is entirely okay for workflows not to have steps.
:::

Let's have a look at how steps are defined:

```yaml title="content/workflows/csp/aws/service/s3/resources/bucket/policy-context/bucket-has-broad-access-permissions.yaml"
steps:
    - id: website-info
      uses: WebsiteContext
      with:
          - name: bucketArn
            value: .canonicalId
          - name: awsRegion
            value: .region
```

The above example is from a policy context workflow. If you recall, the output of the normalizer is sent to all workflows. As such, let's assume the normalizer generated the following output, and this output was received by `bucket-has-broad-access-permissions.yaml` workflow:

```json
{
	"canonicalId": "arn:aws:s3:::bucket_name",
	"region": "us-east-1",
	"alertId": "123",
	"vendorPolicy": "s3-open-to-the-internet"
}
```

Let's talk about each of the fields defined under `steps`.

`Steps` is simply an array of objects, and each object has the following fields:

`id`: This uniquely identifies the step within a workflow.

`uses`: This refers to the serverless function defined in the `content/pkg/template.yaml` file. Henceforth, we will call it Dassana Action.

`with`: This is an optional field. If you do not define it, the entire input received by the workflow is sent to the action. As such, if we did **not** define it, the action will be called with this input:

```json
{
	"canonicalId": "arn:aws:s3:::bucket_name",
	"region": "us-east-1",
	"alertId": "123",
	"vendorPolicy": "s3-open-to-the-internet"
}
```

If you define it (like in this example), you provide an array of objects converted to a JSON object. Each object must have a key named `name`, which becomes the JSON object key.
The `value` is simply a string whose value depends upon the `value-type`. The `value-type` field is optional with the default `JQ`. The possible values of `value-types` are `JQ` and `STRING`.

In the above example, the JSON object will is sent to the action will be:

```json
{
	"bucketArn": "arn:aws:s3:::bucket_name",
	"region": "us-east-1"
}
```

Notice that we did not define `value-type`, so the engine defaulted it to `JQ`, which means the engine will evaluate the JQ expression and construct the value accordingly.
In this case, the JQ expression `.canonicalId` when applied to the input json will evaluate to `arn:aws:s3:::bucket_name`
Let's say you want to send a static value to an action; you can do that by defining the `value-type` as `STRING`.

For example, consider this step definition:

```yaml
steps:
    - id: my-step-id
      uses: AweSomeAction
      with:
          - name: foo
            value: bar
            value-type: STRING
```

Dassana Engine will call the `AweSomeAction` serverless function with the following input:

```json
{
	"foo": "bar"
}
```

Since steps are executed in sequence, the output of the previous step is available to the current steps. The current step can refer to the previous output using the JQ expression `.steps.<id>.key`. For example, consider these steps:

```yaml
steps:
    - id: my-step-id
      uses: AweSomeAction
      with:
          - name: foo
            value: bar
            value-type: STRING
    - id: my-step-up-id
      uses: AweLotAction
      with:
          - name: foo
            value: '.steps.my-step-id.foo'
          - name: bar
            value: baz
            value-type: STRING
```

The engine will invoke `AweSomeAction` serverless function with the input:

```json
{
	"foo": "bar"
}
```

and wait for the function to finish and then invoke `AweLotAction` with the input:

```json
{
	"foo": "bar",
	"bar": "baz"
}
```

## Workflow output

Each workflow can output a JSON object using the same convention we learned in the `steps` above.

:::caution
Although output is optional for `general-context`, `resource-context`, and `policy-context` workflows, it must provide output for the `normalize` workflow to succeed.
:::

Let's review a sample workout output definition:

```yaml title="content/workflows/vendors/security-hub/aws-config.yaml"
output:
    - name: vendorId
      value: aws-config
      value-type: STRING
    - name: alertId
      value: ."resource-info".alertId
    - name: canonicalId
      value: ."resource-info".arn
    - name: vendorPolicy
      value: ."resource-info".policyId
    - name: csp
      value: ."resource-info".csp
    - name: resourceContainer
      value: ."resource-info".resourceContainer
    - name: region
      value: ."resource-info".region
    - name: service
      value: ."resource-info".service
    - name: resourceType
      value: ."resource-info".resourceType
    - name: resourceId
      value: ."resource-info".resourceId
```

Here is a sample output generated by this definition:

```json
{
	"vendorId": "aws-config",
	"alertId": "1234",
	"canonicalId": "arn:aws:s3:::my-bucket",
	"vendorPolicy": "bucket-is-open-to-the-internet",
	"csp": "aws",
	"resourceContainer": "1234567891012",
	"region": "us-west-1",
	"service": "s3",
	"resourceType": "",
	"resourceId": "my-bucket"
}
```

## Risk Config

Except `normlize` workflow, context workflows (`general-context`, `resource-context` and `policy-context`) can define `risk-config`.
Let's take an example from real life [policy context](https://github.com/dassana-io/dassana/blob/main/content/workflows/csp/aws/service/s3/resources/bucket/policy-context/bucket-has-broad-access-permissions.yaml):

```yaml title="content/workflows/csp/aws/service/s3/resources/bucket/policy-context/bucket-has-broad-access-permissions.yaml"
risk-config:
    default-risk: critical
    rules:
        - name: bucket-has-associated-website
          condition: ."website-info".bucketWebsiteUrl != null
          risk: low
```

`default-risk`: this key tells what should be the risk associated with the workflow if none of the rules match. Possible values are `''` , `critical` , `high`, `medium` and `low`

`rules`: an array of objects with the following keys:

`name`: a friendly name

`condition`: JQ expression which must evaluate to `true` or `false`. Note that `website-info` refers to the step id already defined in the workflow.

`risk` : Possible values are `''` , `critical` , `high`, `medium` and `low`

:::note
The empty string `''` is meant to describe 'undefined' or 'unknown' risk value.
:::

:::caution

When writing rules, there are some best practices to keep in mind.

For example, let's say that we want to write a rule for security groups to ensure that they are not left open to the internet. One of the conditions we could write would be to check and see if there are any attached ENIs to the security group in question.

We could go about doing this one of two ways:

Approach 1: 👎

```yaml
name: are-enis-attached-to-sg
condition: ."list-of-attached-eni".result| length == 0
risk: low
```

Approach 2: 👍

```yaml
name: are-enis-attached-to-sg
condition: ."list-of-attached-eni".result| length > 0
risk: high
```

While both conditions are very similar, there are some major gotchas with the first approach (making it a bad choice). What if our security group is attached to a lambda function?
When we do a point-in-time check for attached ENIs using our `list-of-attached-eni` action, there is a probability that the lambda function could not be in running state hence there being `0` attached ENIs. This would result in a low alert but should have actually been high if the security group is indeed open to the internet.

So whenever you are writing conditions, ensure that you are doing higher confidence checks.

:::

:::note
Checkout [this](./general-context#risk-config) section for another example of how risk config is working for a `general-context` workflow.
:::
