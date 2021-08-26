## Tutorial

In this section, we will create a security group that is open on the inbound SSH port. This will result in an AWS Config alert being created in Security Hub because the `securityhub-restricted-ssh` policy will be noncompliant. This alert will automatically get picked up and processed by Dassana.

## Creating a security group

1. Navigate to the `EC2` service.

![EC2](/img/guides/vendor-setup/security-hub/ec2.png)

2. On the left-hand sidebar, select `Security Groups`.
3. Click on `Create security group`.

![Security Groups](/img/guides/vendor-setup/security-hub/securityGroups.png)

4. For the security group, enter a name and description of your choosing.
5. Add an inbound rule with the following properties:
    - Type: `SSH`
    - Source: `Anywhere-IPv4`
6. Add a tag where:
    - Key: `env`
    - Value: `dev`
7. Click `Create security group`.

![Create Security Group](/img/guides/vendor-setup/security-hub/createSecurityGroup.png)

## Speeding up the AWS Config check

1. Navigate to the `Config` service.

![Config](/img/guides/vendor-setup/security-hub/config.png)

2. On the left-hand sidebar, click on `Rules`.
3. Click on the rule with the prefix `securityhub-restricted-ssh`.

![Config Rules](/img/guides/vendor-setup/security-hub/configRules.png)

4. On the top right hand corner of the `securityhub-restricted-ssh` rule, click on `Actions` > `Re-evaluate`.

![Security Group Restricted SSH](/img/guides/vendor-setup/security-hub/securityGroupRestrictedSSH.png)

:::note

In the `Resources in scope` section, click on the refresh icon on the top right. You will have to wait a few minutes for the noncompliant resource to show up. We are only doing this so that we can speed up the test.

:::

5. Soon enough, you should see the noncompliant resource show up!

![Noncompliant Security Group](/img/guides/vendor-setup/security-hub/noncompliantSecurityGroup.png)

## Dassana magic

1. Navigate to the `Simple Queue Service`.

![SQS](/img/guides/vendor-setup/security-hub/sqs.png)

2. In the list of queues, search for the prefix `Dassana`.
3. You will note that three queues are created, namely:
    - `DassanaDeadLetterQueue`
    - `DassanaInboundQueue`
    - `DassanaOutboundQueue`

![Queues](/img/guides/vendor-setup/security-hub/queues.png)

:::note

We will cover the purpose of the queues in the [following section](/docs/how-it-works/alert-lifecycle).

:::

4. Click on `DassanaOutboundQueue`.
5. On the top right of the `DassanaOutboundQueue` details page, click on the `Send and receive messages` button.

![Send and Receive Messages](/img/guides/vendor-setup/security-hub/sendAndReceiveMessages.png)

6. Click on the `Poll for messages` button at the bottom of the page.

![Polling](/img/guides/vendor-setup/security-hub/polling.png)

7. You should see an alert pop-up. Click on the alert ID.
8. When the dialog box opens, you will see the alert JSON. Interestingly, there is a `dassana` object inserted into the original alert created by AWS Config. This `dassana` object contains normalized information about the alert, additional context, and a risk evaluation based on the added context.

![Contextualized Alert](/img/guides/vendor-setup/security-hub/contextualizedAlert.png)

9. And there you have it! Depending upon if ENIs are attached to the security group or not, the risk value changes
