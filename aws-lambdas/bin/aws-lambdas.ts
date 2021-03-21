#!/usr/bin/env node
import 'source-map-support/register';
import * as cdk from '@aws-cdk/core';
import { AwsLambdasStack } from '../lib/aws-lambdas-stack';

const app = new cdk.App();
new AwsLambdasStack(app, 'AwsLambdasStack');
