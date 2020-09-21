package com.sd.seer.user.create;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sd.seer.model.User;
import lombok.SneakyThrows;
import org.apache.http.HttpStatus;

import java.util.HashMap;

public class LambdaRequestHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final ObjectMapper mapper = new ObjectMapper();

    @SneakyThrows
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        LambdaLogger logger = context.getLogger();
        logger.log("Input : " + event + "\n");

        User user = mapper.readValue(event.getBody(), User.class);

        logger.log("Requesting to create user with mail : " + user.getEmail() + "\n");

        // Create a connection to DynamoDB
        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard().build();
        DynamoDBMapper m = new DynamoDBMapper(client);
        logger.log("Mapper created" + "\n");

        if(m.load(User.class, user.getEmail()) == null) {
            DynamoDBMapperConfig config = DynamoDBMapperConfig.builder()
                    .withSaveBehavior(DynamoDBMapperConfig.SaveBehavior.PUT)
                    .build();
            m.save(user, config);
            logger.log("User saved : " + mapper.writeValueAsString(user) + "\n");

            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(HttpStatus.SC_CREATED)
                    .withHeaders(new HashMap<String, String>() {
                        {
                            put("Access-Control-Allow-Origin", "*");
                            put("Access-Control-Allow-Headers", "*");
                        }
                    })
                    .withBody(mapper.writeValueAsString(user));
        } else {
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(HttpStatus.SC_CONFLICT)
                    .withHeaders(new HashMap<String, String>() {
                        {
                            put("Access-Control-Allow-Origin", "*");
                            put("Access-Control-Allow-Headers", "*");
                        }
                    })
                    .withBody(event.getBody());
        }
    }

}
