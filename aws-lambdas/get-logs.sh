#!/bin/bash
lambda_name="AwsLambdasStack-JettroHelloLogsLambdaA3F63B77-130WFH89QU9AN"
payload=$(echo '{"say": "Hello", "to": "Lambda" }' | openssl base64)
aws lambda invoke --function-name $lambda_name out --payload "$payload"
sed -i'' -e 's/"//g' out
sleep 15
aws logs get-log-events --log-group-name /aws/lambda/$lambda_name --log-stream-name $(cat out) --limit 5