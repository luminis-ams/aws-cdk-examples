#!/usr/bin/env node
import 'source-map-support/register';
import * as cdk from '@aws-cdk/core';
import { AwsCfcdnS3Stack } from '../lib/aws-cfcdn-s3-stack';

const app = new cdk.App();
new AwsCfcdnS3Stack(app, 'AwsCfcdnS3Stack');
