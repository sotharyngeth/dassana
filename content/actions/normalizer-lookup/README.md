# Normalizer Resource Hierarchy Lookup

If the normalizer cannot normalize the `csp`, `service`, and `resourceType` fields from an alert, this action uses the alert's `vendorPolicyId` to find the relevant policy context workflow (if applicable) to fill out the empty fields.