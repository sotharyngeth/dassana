## Is S3 bucket hosting a static website

Identifies if a S3 Bucket is associated with a website.

### Sample Input:
```json
{
  "bucketName": "foobar",
  "region": "us-east-1"
}
```

### Sample Output:
```json
{
  "bucketWebsiteUrl": "foobar-content.s3-website-us-east-1.amazonaws.com"
}
```
