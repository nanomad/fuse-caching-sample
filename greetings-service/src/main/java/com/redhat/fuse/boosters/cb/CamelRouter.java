package com.redhat.fuse.boosters.cb;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.infinispan.InfinispanConstants;
import org.apache.camel.component.infinispan.InfinispanOperation;
import org.apache.camel.model.rest.RestBindingMode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * A simple Camel REST DSL route that implement the greetings service.
 */
@Component
public class CamelRouter extends RouteBuilder {

    @Value("${nameService.host}")
    String nameServiceHost;

    @Value("${nameService.port}")
    String nameServicePort;


    @Override
    public void configure() throws Exception {



        restConfiguration()
                .component("servlet")
                .bindingMode(RestBindingMode.json);

        // @formatter:off
        rest("/greetings").description("Greetings REST service")
                .consumes("application/json")
                .produces("application/json")
                .get()
                .to("direct:greetingsImpl")
                ;

        from("direct:greetingsImpl").description("Greetings REST service implementation route")
                .streamCaching()
                .removeHeader("Accept-Encoding")
                .removeHeader("If-None-Match")
                .log(" Try to call name Service")
                .to("direct:getRemoteName")
                .to("bean:greetingsService?method=getGreetings");

        from("direct:getRemoteName").description("Fetches name data from the remote service and caches it")
                .setHeader(InfinispanConstants.KEY, constant("name"))
                .setHeader(InfinispanConstants.OPERATION, constant(InfinispanOperation.GET))
                .to("infinispan:nameCache?resultHeader=OutputHeader")
                .process(x -> {
                    System.err.println(x.getMessage().getHeaders());
                    System.err.println(x.getMessage().getBody(String.class));
                })
                .choice().when(header("OutputHeader").isNull())
                    .to("https://reqres.in/api/users/2?bridgeEndpoint=true")
                    .convertBodyTo(String.class)
                    .log("Got data from service: ${body}")
                    .wireTap("direct:storeOnCache")
                    .log("Got data from service: ${body}")
                .endChoice().otherwise()
                    .setBody().header("OutputHeader")
                    .log("Got data from cache: ${body}")
                .end()
        ;

        from("direct:storeOnCache")
                .setHeader(InfinispanConstants.KEY, constant("name"))
                .setHeader(InfinispanConstants.OPERATION, constant(InfinispanOperation.PUT))
                .setHeader(InfinispanConstants.VALUE, body())
                .to("infinispan:nameCache")
                .log("Data stored in cache. ---> ${header.CamelInfinispanOperationResult}")
        ;
        // @formatter:on

    }

}