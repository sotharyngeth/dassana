## What ENIs are attached to Security Group

Identifies and returns ENIs attached to a security group.

### Sample Input:
```json
{
  "groupId": "",
  "region": "us-east-1"
}
```

### Sample Output:
```json
{
  "result": [
    {
      "Attachment": {
        "AttachmentId": "",
        "DeleteOnTermination": false,
        "DeviceIndex": 1,
        "InstanceOwnerId": "amazon-aws",
        "Status": "attached"
      },
      "AvailabilityZone": "us-east-1b",
      "Description": "",
      "Groups": [
        {
          "GroupName": "terraform-xyz",
          "GroupId": "sg-12345example"
        }
      ],
      "InterfaceType": "vpc_endpoint",
      "Ipv6Addresses": [],
      "MacAddress": "",
      "NetworkInterfaceId": "eni-12345example",
      "OwnerId": "123456",
      "PrivateDnsName": "",
      "PrivateIpAddress": "",
      "PrivateIpAddresses": [
        {
          "Primary": true,
          "PrivateDnsName": "",
          "PrivateIpAddress": ""
        }
      ],
      "RequesterId": "",
      "RequesterManaged": true,
      "SourceDestCheck": true,
      "Status": "in-use",
      "SubnetId": "",
      "TagSet": [],
      "VpcId": ""
    }
  ]
}
```