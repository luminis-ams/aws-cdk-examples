#!/bin/bash
lambda_name="AwsLambdasStack-LambdaDemoSendToSNSLambda7078A2C4-52H663GDO7CH"
payload=$(echo '{"message": "Hi, hope you like this async message from AWS." }' | openssl base64)
aws lambda invoke --function-name $lambda_name --invocation-type Event out --payload "$payload"
sed -i'' -e 's/"//g' out
sleep 15
log_stream_name=`aws logs describe-log-streams --log-group-name /aws/lambda/$lambda_name --max-items 1 --order-by LastEventTime --descending --query logStreams[].logStreamName --output text | head -n 1`
aws logs get-log-events --log-group-name /aws/lambda/$lambda_name --log-stream-name $log_stream_name --limit 5
