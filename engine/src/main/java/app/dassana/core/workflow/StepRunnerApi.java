package app.dassana.core.workflow;

import app.dassana.core.normalize.model.NormalizedWorkFlowOutput;
import app.dassana.core.workflow.model.Step;
import app.dassana.core.workflow.model.StepRunResponse;
import app.dassana.core.workflow.model.Workflow;

public interface StepRunnerApi {

  StepRunResponse runStep(Workflow workflow,Step step,String inputJson, NormalizedWorkFlowOutput normalizedWorkFlowOutput) throws Exception;

}
