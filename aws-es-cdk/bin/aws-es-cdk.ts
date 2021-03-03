#!/usr/bin/env node
import 'source-map-support/register';
import * as cdk from '@aws-cdk/core';
import { AwsEsCdkStack } from '../lib/aws-es-cdk-stack';

const app = new cdk.App();
new AwsEsCdkStack(app, 'AwsEsCdkStack');
