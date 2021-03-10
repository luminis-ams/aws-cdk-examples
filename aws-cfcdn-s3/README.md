# Welcome to your CDK TypeScript project!

This is a blank project for TypeScript development with CDK.

The `cdk.json` file tells the CDK Toolkit how to execute your app.

## Useful commands

 * `npm run build`   compile typescript to js
 * `npm run watch`   watch for changes and compile
 * `npm run test`    perform the jest unit tests
 * `cdk deploy`      deploy this stack to your default AWS account/region
 * `cdk diff`        compare deployed stack with current state
 * `cdk synth`       emits the synthesized CloudFormation template

## How did this work
$ mkdir aws-cfcdn-s3
$ cd aws-cfcdn-s3
$ cdk init --language typescript

$ npm install '@aws-cdk/aws-s3'
$ npm install '@aws-cdk/aws-cloudfront'
$ npm install '@aws-cdk/aws-cloudfront-origins'
$ npm install '@aws-cdk/aws-s3-deployment'
$ npm install '@aws-cdk/aws-certificatemanager'


## Articles used to create this
https://docs.aws.amazon.com/cdk/api/latest/docs/aws-cloudfront-readme.html
https://docs.aws.amazon.com/cdk/api/latest/docs/aws-s3-deployment-readme.html

 