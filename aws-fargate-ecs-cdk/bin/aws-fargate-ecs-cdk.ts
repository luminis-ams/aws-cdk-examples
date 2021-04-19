#!/usr/bin/env node
import 'source-map-support/register';
import * as cdk from '@aws-cdk/core';
import { AwsFargateEcsCdkStack } from '../lib/aws-fargate-ecs-cdk-stack';

const app = new cdk.App();
new AwsFargateEcsCdkStack(app, 'AwsFargateEcsCdkStack');
