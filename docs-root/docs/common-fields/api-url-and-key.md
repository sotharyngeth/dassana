In order to get started with the Dassana workflow editor, you first need to provide your API gateway url and key. In order to get this information, please do the following:

1. In the AWS console, navigate to the CloudFormation service.
1. Ensure you have `Stacks` selected on the left-hand panel.
1. Click on the Dassana stack that you deployed.
1. Once you see all the stack details, click on the `Outputs` tab.
1. The `ApiGatewayEndpoint` value is your API url.
1. For the API key, click on the value of the `ApiGatewayApiKey` output.
1. This will redirect you to the API Gateway service.
1. Click on the Dassana API (this will be prefixed with the Dassana stack name you set).
1. In the left-hand panel, ensure you select `API Keys`.
1. Click on the appropriate API key (this will be prefixed with the Dassana stack name you set).
1. Click `Show` to view and copy the API key.
1. Finally enter the API key in the [Dassana workflow editor](https://editor.dassana.io/) to get started!
