# This repository contains some samples to configure AWS services using CDK

## Elasticsearch + Kibana + Cognito
Most of the code is taken from this sample:
https://github.com/aws-samples/amazon-elasticsearch-service-with-cognito

I removed a few parts that we not necessary to explain what I wanted in a blog. I added the Level 2 integration with with cdk Elasticsearch Domain.


## Resources
https://dev.to/evnz/single-cloudfront-distribution-for-s3-web-app-and-api-gateway-15c3
https://aws.amazon.com/premiumsupport/knowledge-center/no-access-control-allow-origin-error/
curl -H "origin: jettro.cloudsearchsolutions.com" -v "https://jettro.cloudsearchsolutions.com/prod/?q=blauw"