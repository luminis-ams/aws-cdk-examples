import * as cdk from '@aws-cdk/core';
import {Distribution, PriceClass} from "@aws-cdk/aws-cloudfront";
import {Bucket} from "@aws-cdk/aws-s3";
import {S3Origin} from "@aws-cdk/aws-cloudfront-origins";
import {BucketDeployment, Source} from "@aws-cdk/aws-s3-deployment";
import {Certificate} from "@aws-cdk/aws-certificatemanager";

export class AwsCfcdnS3Stack extends cdk.Stack {
    constructor(scope: cdk.Construct, id: string, props?: cdk.StackProps) {
        super(scope, id, props);

        const sourceBucket = new Bucket(this, 'jettro-cfcdn-bucket');

        new BucketDeployment(this, 'jettro-cfcdn-deploy', {
            sources: [Source.asset('./site-contents')],
            destinationBucket: sourceBucket,
        });

        const certificate = Certificate.fromCertificateArn(this, 'ImportedCertificate', "arn:aws:acm:us-east-1:044915237328:certificate/bd614fb1-fc88-4c8c-b2ff-225398699f28")

        // The code that defines your stack goes here
        new Distribution(this, 'jettro-cloudfront-example', {
            defaultBehavior: {
                origin: new S3Origin(sourceBucket),
            },
            priceClass: PriceClass.PRICE_CLASS_100,
            enableLogging: true,
            logFilePrefix: "jettro-cfcdn-",
            domainNames: [
                "jettro.cloudsearchsolutions.com"
            ],
            certificate: certificate,
        });
    }
}
