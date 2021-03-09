#!/usr/bin/env node
import 'source-map-support/register';
import * as cdk from '@aws-cdk/core';
import { AwsEsGatewayLambdaStack } from '../lib/aws-es-gateway-lambda-stack';

const app = new cdk.App();
new AwsEsGatewayLambdaStack(app, 'AwsEsGatewayLambdaStack');
