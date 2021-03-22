#!/bin/bash
lambda_name="AwsLambdasStack-SendToSNSLambdaE1D5DD6A-T6Q6W0KDYCTV"
payload=$(echo '{"message": "Hi, hope you like this message from AWS." }' | openssl base64)
aws lambda invoke --function-name $lambda_name out --payload "$payload"
sed -i'' -e 's/"//g' out
sleep 15
aws logs get-log-events --log-group-name /aws/lambda/$lambda_name --log-stream-name $(cat out) --limit 5