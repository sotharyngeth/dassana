# AWS Config

:::info

AWS Config (via Security Hub) is natively integrated in Dassana! You don't have to do anything.

:::

The native integration is achieved by listening on EventBridge.

Please note that currently we can process AWS Config alerts managed by SecurityHub only. This means that you must have SecurityHub enabled.

Here is an example SecurityHub alert (snipped for readability)-

```JSON
{
  "SchemaVersion": "2018-10-08",
  "Id": "arn:aws:securityhub:us-east-1:363265257036:subscription/cis-aws-foundations-benchmark/v/1.2.0/4.1/finding/4991510f-77f2-4c48-bbb9-f51697bffa3d",
  "ProductArn": "arn:aws:securityhub:us-east-1::product/aws/securityhub",
  "ProductName": "Security Hub",
  "CompanyName": "AWS",
  "Region": "us-east-1",
  "GeneratorId": "arn:aws:securityhub:::ruleset/cis-aws-foundations-benchmark/v/1.2.0/rule/4.1",

}
```

When this alert (event) is processed by EventBridge, following metadata is added :

```json
{
	"detail-type": "Security Hub Findings - Imported",
	"source": "aws.securityhub",
	"time": "2021-07-14T03:54:44Z"
}
```

As such, the event which is emitted by EventBridge becomes-

```json
{
	"detail-type": "Security Hub Findings - Imported",
	"source": "aws.securityhub",
	"detail": {
		"findings": [
			{
				"SchemaVersion": "2018-10-08",
				"Id": "arn:aws:securityhub:us-east-1:363265257036:subscription/cis-aws-foundations-benchmark/v/1.2.0/4.1/finding/4991510f-77f2-4c48-bbb9-f51697bffa3d",
				"ProductArn": "arn:aws:securityhub:us-east-1::product/aws/securityhub",
				"ProductName": "Security Hub",
				"CompanyName": "AWS",
				"Region": "us-east-1",
				"GeneratorId": "arn:aws:securityhub:::ruleset/cis-aws-foundations-benchmark/v/1.2.0/rule/4.1"
			}
		]
	}
}
```

Now, if you look at [AWS Config normalizer](https://github.com/dassana-io/dassana/blob/main/content/workflows/vendors/security-hub/aws-config.yaml) you will find that we have this filter config:

```
.detail and .detail.findings and (.detail.findings[].ProductFields.StandardsGuideArn or .detail.findings[].ProductFields.StandardsArn )
```

The JQ expression will return true for all AWS Config alerts ingested by SecurityHub and processed via EventBridge
