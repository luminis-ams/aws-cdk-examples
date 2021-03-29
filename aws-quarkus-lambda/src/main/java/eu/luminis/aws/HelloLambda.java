package eu.luminis.aws;

import javax.inject.Inject;
import javax.inject.Named;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;


@Named("hello")
public class HelloLambda  implements RequestHandler<InputObject, OutputObject> {

    @Override
    public OutputObject handleRequest(InputObject input, Context context) {
        OutputObject out = new OutputObject();
        out.setResult("Hello World!!");
        out.setRequestId(context.getAwsRequestId());
        return out;
    }

}
