#!/usr/bin/env node
import 'source-map-support/register';
import * as cdk from '@aws-cdk/core';
import { AwsScraperStepFunctionsStack } from '../lib/aws-scraper-step-functions-stack';

const app = new cdk.App();
new AwsScraperStepFunctionsStack(app, 'AwsScraperStepFunctionsStack');
