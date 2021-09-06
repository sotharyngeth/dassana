from glob import glob

from aws_lambda_powertools.utilities.parser import event_parser
from aws_lambda_powertools.utilities.typing import LambdaContext
from pydantic import BaseModel
from dassana.common.models import NormalizedOutput
from yaml import safe_load
from json import loads


class Input(BaseModel):
    input: NormalizedOutput


file_paths = glob('/opt/aws/**/*.yaml', recursive=True)


def find_policy_match(file_path, vendor, csp, policyId):
    with open(file_path, 'r') as f:
        yml = safe_load(f)
        if yml.get('csp') != csp:
            return
        filters = yml.get('filters', [])
        if filters is None:
            return
        for fil in filter(lambda x: x.get('vendor', '') == vendor, filters):
            if policyId in fil.get('policies', []):
                return yml.get('service'), yml.get('resource-type'), yml.get('csp')


@event_parser(model=Input)
def handle(event: Input, context: LambdaContext):
    if event.input.resourceType is not None and event.input.service is not None and event.input.csp is not None:
        return loads(event.input.json())
    service, resource_type, csp = next(filter(lambda x: x is not None,
                                              map(find_policy_match, file_paths,
                                                  [event.input.vendorId] * len(file_paths),
                                                  [event.input.csp] * len(file_paths),
                                                  [event.input.vendorPolicy] * len(file_paths))), (None, None, None))
    event.input.service = service if service is not None else event.input.service
    event.input.resourceType = resource_type if service is not None else event.input.service
    event.input.csp = csp if service is not None else event.input.service
    return loads(event.input.json())
