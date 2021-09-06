## Exposure of an EC2 instance to the internet

This action returns the exposure of an EC2 instance to the internet.

Note that even though [VPC Reachability Analyzer](https://docs.aws.amazon.com/vpc/latest/reachability/what-is-reachability-analyzer.html) can help you identify _network path_, the use case of instances behind ALB is not handled by the VPC Reachability Analyzer service.

On a high level, the action identifies the following questions for an instance:

1. Is the instance attached to an internet-facing Load Balancer as a target?
   1. Does the LoadBalancer have any authentication type rules?
      1. authenticate-cognito / authenticate-oidc
1. Is the instance attached to security group that has ingress rules permitting traffic from the Internet?
1. Does the EC2 instance have a public IP?

### Sample Input:

```json
{
  "instanceId": "i-09f3989e7c911111",
  "region": "us- east-1"
}
```

### Sample Output:

```json
{
  "appLayer": {
    "type": "internet-facing",
    "canReceiveUnauthenticatedTraffic": false,
    "authConfig": {
      "Type": "authenticate-oidc",
      "AuthenticateOidcConfig": {
        "Issuer": "https://accounts.google.com",
        "AuthorizationEndpoint": "https://accounts.google.com/o/oauth2/v2/auth",
        "TokenEndpoint": "https://oauth2.googleapis.com/token",
        "UserInfoEndpoint": "https://openidconnect.googleapis.com/v1/userinfo",
        "ClientId": "XXX.apps.googleusercontent.com",
        "SessionCookieName": "AWSELBAuthSessionCookie",
        "Scope": "openid",
        "SessionTimeout": 604800,
        "OnUnauthenticatedRequest": "authenticate"
      },
      "Order": 1
    }
  },
  "direct": {
    "publicIp": "3.223.222.223",
    "allowedVia": {
      "sg": []
    },
    "isExposed": false
  }
}
```

### Reference

#### `appLayer` field contains information about how the instance is exposed via some application layer e.g. ALB.

`type`: `str`

The only permissible type right now is `internet-facing`, meaning internet-facing LoadBalancers.

`canReceiveUnauthenticatedTraffic`: `bool`

A boolean that identifies if the EC2 instance is behind a LoadBalancer, and is receving un-authenticated traffic.

`authConfig`: `List[Dict]`

The rules of the LoadBalancer that are related to the auth configuration (i.e `authenticate-cognito` or
`authenticate-oidc`).

### `direct` field contains information about how instance is exposed on a "network" layer

`publicIp`: `str`

Str representing the publicIp of the instance.

`allowedVia`: `List[str]`

A list of security groups that permit ingress traffic from the Internet.

`isExposed`: `bool`

True if instance has a publicIP and a non-empty allowedVia. Otherwise, False.
