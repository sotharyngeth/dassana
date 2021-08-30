# High Level Overview

On a high level, Dassana's magic begins when alerts are sent to an inbound queue:

![Magic](/img/how-it-works/magic.png)

## Input

### Inbound queue

This queue is automatically created when you deploy Dassana. This is the entry point for all alerts.

:::note
If you use AWS Config and/or Guard Duty with SecurityHub, you don't have to do anything! Dassana ingests those alerts using an eventbridge rule. This rule copies the alert to the inbound queue.
:::note

### API

Deployment of Dassana also creates an API gateway. You can interact with the API using [Dassana Editor](https://editor.dassana.io/).

## Magic (Processing)

Dassana Engine gets triggered (when an alert is received in the inbound queue or when API is invoked), and it runs the alert through a series of workflows. We will talk about them in the next section.

## Output

### Outbound queue

Much like the inbound queue, an outbound queue is created automatically. Once alerts are processed, they are sent to this queue. Any valid JSON object sent to the inbound queue will make its way to the outbound queue regardless of whether the context was added or not. Say, for example, you sent an alert that Dassana didn't recognize. This alert would still make it into the outbound queue since it was a valid JSON object. If Dassana does understand the alert in question, the alert will have an additional `dassana` object added to the original JSON containing the context.

## Error handling

### Dead letter queue

If any errors or issues occur when processing an alert, they will be sent here. Errors can occur if a non JSON object is sent to the inbound queue or any other issues like lack of permission. In this queue, you will find the original alert JSON along with an error message.
