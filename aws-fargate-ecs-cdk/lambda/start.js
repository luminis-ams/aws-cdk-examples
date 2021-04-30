const AWS = require('aws-sdk');
let ecs = new AWS.ECS();

const task = process.env.taskDef;
const cluster = process.env.cluster;
const subnets = process.env.subnets;
const security_group = process.env.security_group;
const containerName = process.env.containerName;
const runProcess = process.env.runProcess;

exports.handler = async function (events) {
    console.log("START");
    return await new Promise((resolve, reject) => {
        ecs.listTasks({
            cluster: cluster
        }, function (err, data) {
            if (!err) {
                console.log(data)
                if (data && data.taskArns && data.taskArns.length > 0) {
                    buildResponse(resolve, 500, "Already indexing task running");
                } else {
                    let params = buildRunTaskParams(events);
                    ecs.runTask(params, function (err, data) {
                        let response;
                        let statusCode;
                        if (err) {
                            console.log(err, err.stack);
                            response = "Error starting new task: " + err.stack
                            statusCode = 500
                        } else {
                            //todo Fix start call to fargate container / Decide on auto start in container
                            console.log(data)
                            if (runProcess === 'START') {
                                response = "Started a new indexing task"
                            } else {
                                response = "Started a new cleaning task"
                            }

                            statusCode = 200
                        }
                        buildResponse(resolve, statusCode, response)
                    })
                }
            } else {
                buildResponse(resolve, 500, "Error retrieving current tasks: " + err.stack);
            }
        })
    })

    function buildRunTaskParams(events) {
        return {
            taskDefinition: task,
            cluster: cluster,
            count: 1,
            networkConfiguration: {
                awsvpcConfiguration: {
                    assignPublicIp: 'ENABLED',
                    subnets: [subnets],
                    securityGroups: [security_group]
                }
            },
            overrides: {
                containerOverrides: [
                    {
                        name: containerName,
                        environment: [{
                            name: 'norconex.action',
                            value: runProcess
                        }]
                    }]
            },
            launchType: 'FARGATE'
        }
    }

    function buildResponse(resolve, statusCode, message) {
        resolve({
            "isBase64Encoded": false,
            "statusCode": statusCode,
            "headers": {
                "Access-Control-Allow-Origin": '*'
            },
            "body": message
        });
    }

}
