// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
import {v4 as uuid} from 'uuid';
import {CrawlInput} from './crawler/types';
import {startCrawl} from './steps/1_startCrawl';

const {
    HISTORY_TABLE_NAME,
    CONTEXT_TABLE_NAME_PREFIX,
    WEB_CRAWLER_STATE_MACHINE_ARN,
} = process.env;

/**
 * Responsible for starting a crawl
 */
export const startCrawlHandler = async (event: any, context: any) => {
    console.log(event, context);

    if (!HISTORY_TABLE_NAME || !WEB_CRAWLER_STATE_MACHINE_ARN || !CONTEXT_TABLE_NAME_PREFIX) {
        throw new Error("Environment not configured correctly. HISTORY_TABLE_NAME, WEB_CRAWLER_STATE_MACHINE_ARN and CONTEXT_TABLE_NAME_PREFIX must be specified");
    }

    const input: CrawlInput = event;

    return await startCrawl({
        ...input,
        crawlId: uuid(),
    }, CONTEXT_TABLE_NAME_PREFIX, WEB_CRAWLER_STATE_MACHINE_ARN);
};

