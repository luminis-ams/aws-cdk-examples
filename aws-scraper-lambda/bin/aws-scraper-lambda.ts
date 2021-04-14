#!/usr/bin/env node
import 'source-map-support/register';
import * as cdk from '@aws-cdk/core';
import { AwsScraperLambdaStack } from '../lib/aws-scraper-lambda-stack';

const app = new cdk.App();
new AwsScraperLambdaStack(app, 'AwsScraperLambdaStack');
