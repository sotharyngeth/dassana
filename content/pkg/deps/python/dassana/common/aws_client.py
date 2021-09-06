import uuid
from typing import Dict, Any

from aws_lambda_powertools.utilities.typing import LambdaContext
from aws_lambda_powertools.utilities.typing.lambda_client_context import LambdaClientContext
from boto3 import Session, client

from dassana.common.models import ArnComponent


class LambdaTestContext(LambdaContext):
    class LambdaTestClientContext(LambdaClientContext):
        def __init__(self):
            self._custom = {}
            self._env = {}

    def __init__(self, name: str, version: int = 1, region: str = "us-east-1", account_id: str = "111122223333"):
        self._function_name = name
        self._function_version = str(version)
        self._memory_limit_in_mb = 128
        self._invoked_function_arn = f"arn:aws:lambda:{region}:{account_id}:function:{name}:{version}"
        self._aws_request_id = str(uuid.uuid4())
        self._log_group_name = f"/aws/lambda/{name}"
        self._log_stream_name = str(uuid.uuid4())
        self._client_context = self.LambdaTestClientContext()


class DassanaAwsObject(object):
    def __init__(self):
        self._session = Session()

    def create_aws_client(self, context: LambdaContext, service, region: str) -> client:
        if context is None or context.client_context is None or context.client_context.custom is None or len(
                context.client_context.custom) == 0:
            return self._session.client(service, region)
        else:
            access_key = context.client_context.custom.get('AWS_ACCESS_KEY_ID')
            secret_key = context.client_context.custom.get('AWS_SECRET_ACCESS_KEY')
            session_token = context.client_context.custom.get('AWS_SESSION_TOKEN')
            return self._session.client(service_name=service, region_name=region, aws_access_key_id=access_key,
                                        aws_secret_access_key=secret_key,
                                        aws_session_token=session_token)


def parse_arn(arn: str):
    elements = arn.split(':', 5)
    result = {
        'arn': elements[0],
        'partition': elements[1],
        'service': elements[2],
        'region': elements[3],
        'account': elements[4],
        'resource': elements[5],
        'resource_type': None
    }
    if '/' in result['resource']:
        result['resource_type'], result['resource'] = result['resource'].split('/', 1)
    elif ':' in result['resource']:
        result['resource_type'], result['resource'] = result['resource'].split(':', 1)
    return ArnComponent(
        arn=result.get('arn', None),
        partition=result.get('partition'),
        service=result.get('service', None),
        region=result.get('region'),
        account=result.get('account'),
        resource=result.get('resource', None),
        resource_type=result.get('resource_type', None)
    )
