#!/usr/bin/env node
import 'source-map-support/register';
import * as cdk from '@aws-cdk/core';
import { AwsEsGatewayLambdaRequestResponseStack } from '../lib/aws-es-gateway-lambda-request-response-stack';

const app = new cdk.App();
new AwsEsGatewayLambdaRequestResponseStack(app, 'AwsEsGatewayLambdaRequestResponseStack');
