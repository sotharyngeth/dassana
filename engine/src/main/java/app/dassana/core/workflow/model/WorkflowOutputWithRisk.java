package app.dassana.core.workflow.model;

import app.dassana.core.risk.model.Risk;

public class WorkflowOutputWithRisk extends WorkflowOutput {

  Risk risk;


  public WorkflowOutputWithRisk() {
    risk = new Risk();
  }

  public Risk getRisk() {
    return risk;
  }
  public void setRisk(Risk risk) {
    this.risk = risk;
  }

  @Override
  public String toString() {
    return "WorkflowOutputWithRisk{" +
        "risk=" + risk +
        '}';
  }
}
