# Mid Level Details

Let's see what is inside the magic box
![Engine & Content](/img/how-it-works/engine-content.png)

We see that there are two boxes - Engine and Content.

## Engine

The Dassana Engine is responsible for running workflows against an alert. It can take alerts from an SQS queue, or you can interact with it using [Dassana Editor](https://editor.dassana.io/) to author content. We will not open this box; it isn't as interesting as the other box - content.

## Content

Let's open the content box.

![Content](/img/how-it-works/content.png)

We see that there are four tiny cute little boxes. These boxes are what we call Workflows.

### Workflows

Workflows are the fuel that powers the Dassana Engine. These are simple YAML files that describe what to do with an alert. These have filters on top, which tell if the workflow should run or not, and have steps in the middle which run actions (serverless functions) whose output is used in the risk-config section to determine the risk. The workflows can also emit an output that is available in the processed alert. We will discuss more on the next page.

### Actions

Lightweight serverless functions that are used in workflows. Although they are lightweight, they do the heavy lifting of adding context.

## Example

Let's take an example. Say that the following JSON was sent to the inbound queue:

```json
{
	"foo": "bar"
}
```

Dassana Engine will get triggered, and it will run a series of workflows to normalize and contextualize them. The processed alert will look like this:

```json
{
	"foo": "bar",
	"normalize": {
		"output": {},
		"step-output": []
	},
	"general-context": {
		"risk": {},
		"output": {},
		"step-output": []
	},
	"resource-context": {
		"risk": {},
		"output": {},
		"step-output": []
	},
	"policy-context": {
		"risk": {},
		"output": {},
		"step-output": []
	}
}
```

In the real world, you will see values and not these empty JSON objects.
But notice how the processed alert has these four JSON keys: `normalize`, `general-context` , `resource-context` and `policy-context`. These four keys correspond to the four boxes we saw above.

Also, notice the `output` and `step-output` keys. The output key corresponds to the `output` of the workflow.
The `step-output` key corresponds to the steps. Each step is a serverless function that we call an action.

Let's talk about workflows and actions in the next section.
