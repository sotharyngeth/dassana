# Putting It Together

Now that you understand how the magic happens. Let's review the terminology and walk through a summary of the whole process.

## Terminology

| Syntax   | Description                                                                                                                                                                                                                                                                                                  |
| -------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| Engine   | Responsible for running workflows against an alert (json object). It can take alerts from a SQS queue or you can interact with it using [Dassana Editor](https://editor.dassana.io/) to author workflows                                                                                                     |
| Workflow | A YAML file which describes what to do with an alert. It has filters on top which tells if the workflow should run or not, it has steps in the middle which run actions (serverless functions) whose output is used in the risk-config section to determine the risk. The workflows can also emit an output. |
| Action   | Lightweight serverless functions which are used in workflows. Although they are lightweight, they do the heavy lifting of adding context                                                                                                                                                                     |

## Summary

Alerts are sent to the inbound queue. Then, the engine processes alerts one at a time. The engine first runs the alert against the normalize workflow. Here, the vendor is identified, and required fields are plucked out into a normalized object. Next, the engine runs the alert against all three context workflows (general, resource, and policy) in parallel. Each of the context workflows can add a risk of their own.

## Dassana output

Once alerts are processed by the engine, they are sent to the outbound queue. A `dassana` object is added into the original alert JSON containing all the context outputted by the various workflows.

Let's say the original alert was something like this -

```json
{
	"foo": "bar"
}
```

After the alert has been processed, Dassana will add a key to it like this-

```json
{
	"foo": "bar",
	"dassana": {}
}
```

The output and risk from each of the workflow becomes available inside the `dassana` key
