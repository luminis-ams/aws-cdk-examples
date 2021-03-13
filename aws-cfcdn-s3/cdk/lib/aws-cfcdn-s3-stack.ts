import * as cdk from '@aws-cdk/core';
import {
    AllowedMethods,
    Distribution,
    OriginRequestPolicy,
    PriceClass,
    ViewerProtocolPolicy
} from "@aws-cdk/aws-cloudfront";
import {Bucket} from "@aws-cdk/aws-s3";
import {HttpOrigin, S3Origin} from "@aws-cdk/aws-cloudfront-origins";
import {BucketDeployment, Source} from "@aws-cdk/aws-s3-deployment";
import {Certificate} from "@aws-cdk/aws-certificatemanager";

export class AwsCfcdnS3Stack extends cdk.Stack {
    constructor(scope: cdk.Construct, id: string, props?: cdk.StackProps) {
        super(scope, id, props);

        const sourceBucket = new Bucket(this, 'jettro-cfcdn-bucket');

        new BucketDeployment(this, 'jettro-cfcdn-deploy', {
            sources: [Source.asset('../build')],
            destinationBucket: sourceBucket,
        });

        const certificate = Certificate.fromCertificateArn(this, 'ImportedCertificate', "arn:aws:acm:us-east-1:044915237328:certificate/bd614fb1-fc88-4c8c-b2ff-225398699f28")

        // The code that defines your stack goes here
        new Distribution(this, 'jettro-cloudfront-example', {
            defaultBehavior: {
                origin: new S3Origin(sourceBucket),
                originRequestPolicy: OriginRequestPolicy.CORS_S3_ORIGIN,
            },
            additionalBehaviors: {
                '/prod/*': {
                    origin: new HttpOrigin("qke0rtmpb0.execute-api.eu-west-1.amazonaws.com", {}),
                    viewerProtocolPolicy: ViewerProtocolPolicy.REDIRECT_TO_HTTPS,
                    allowedMethods: AllowedMethods.ALLOW_GET_HEAD_OPTIONS,
                    originRequestPolicy: OriginRequestPolicy.CORS_CUSTOM_ORIGIN,
                }
            },
            priceClass: PriceClass.PRICE_CLASS_100,
            enableLogging: true,
            logFilePrefix: "jettro-cfcdn-",
            domainNames: [
                "jettro.cloudsearchsolutions.com"
            ],
            certificate: certificate,
            defaultRootObject: "index.html",
        });
    }
}
