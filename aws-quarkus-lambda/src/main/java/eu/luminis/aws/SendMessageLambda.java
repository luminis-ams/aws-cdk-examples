package eu.luminis.aws;

import javax.inject.Inject;
import javax.inject.Named;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

@Named("message")
public class SendMessageLambda implements RequestHandler<InputObject, OutputObject> {

    @Inject
    ProcessingService service;

    @Override
    public OutputObject handleRequest(InputObject input, Context context) {
        String process = service.process(input.getGreeting(), input.getName());
        OutputObject out = new OutputObject();
        out.setResult(process);
        out.setRequestId(context.getAwsRequestId());

        return out;
    }
}
