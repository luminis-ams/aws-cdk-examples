#!/usr/bin/env node
import 'source-map-support/register';
import * as cdk from '@aws-cdk/core';
import { AwsStepFunctionsSearchByronStack } from '../lib/aws-step-functions-search-byron-stack';

const app = new cdk.App();
new AwsStepFunctionsSearchByronStack(app, 'AwsStepFunctionsSearchByronStack');
