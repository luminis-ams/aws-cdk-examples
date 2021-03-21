#!/usr/bin/env node
import 'source-map-support/register';
import * as cdk from '@aws-cdk/core';
import { AwsStepFunctionsStack } from '../lib/aws-step-functions-stack';

const app = new cdk.App();
new AwsStepFunctionsStack(app, 'AwsStepFunctionsStack');
