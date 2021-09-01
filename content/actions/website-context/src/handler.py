from json import load
from typing import Dict, Any, Optional

from aws_lambda_powertools.utilities.typing import LambdaContext
from aws_lambda_powertools.utilities.validation import validator
from boto3 import resource
from botocore.exceptions import ClientError
from aws_lambda_powertools import Logger

from dassana.common.aws_client import DassanaAwsObject, parse_arn

with open('input.json', 'r') as schema:
    schema = load(schema)
    dassana_aws = DassanaAwsObject()

logger = Logger(service='dassana-actions')


@logger.inject_lambda_context
@validator(inbound_schema=schema)
def handle(event: Dict[str, Optional[Any]], context: LambdaContext):
    arn = parse_arn(event.get('bucketArn'))
    region = event.get('awsRegion')
    arn_resource = arn.get('resource')
    client = dassana_aws.create_aws_client(context, 's3', event.get('awsRegion'))

    try:
        bucket_website = client.get_bucket_website(Bucket=arn_resource)
        return {"bucketWebsiteUrl": "%s.s3-website-%s.amazonaws.com" % (arn_resource, region)}
    except ClientError as e:
        logger.error(e.response)
        if e.response.get('Error').get('Code') in ['NoSuchBucket', 'NoSuchWebsiteConfiguration']:
            return {"bucketWebsiteUrl": ""}
        else:
            raise Exception(e)
