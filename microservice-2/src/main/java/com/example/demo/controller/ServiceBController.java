package com.example.demo.controller;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.TraceMetadata;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
// import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/service-b")
public class ServiceBController {

    private static final Logger logger = LoggerFactory.getLogger(ServiceBController.class);

    @Value("${http.client.connect-timeout:225}")
    private int connectTimeout;
    
    @Value("${http.client.read-timeout:475}")
    private int readTimeout;

    @Value("${service.c.url:localhost}")
    private String serviceC;
   
    private  RestTemplate restTemplate;
    
    @PostConstruct
    public void init() {
        this.restTemplate = new RestTemplate();
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout);  // Now has correct value
        factory.setReadTimeout(readTimeout);        // Now has correct value
        restTemplate.setRequestFactory(factory); 
    }

    @Trace(dispatcher = true)
    @GetMapping("/compute")
    public ResponseEntity<String> callServiceC(@RequestParam double t1, HttpServletRequest request) {
        // Extract incoming trace headers
        String incomingTraceParent = request.getHeader("traceparent");
        String incomingTraceId = request.getHeader("X-NewRelic-Transaction");
        String incomingSpanId = request.getHeader("X-NewRelic-Span");

        TraceMetadata traceMetadata = NewRelic.getAgent().getTraceMetadata();

        String traceId = traceMetadata != null ? traceMetadata.getTraceId() : null;
        String spanId = traceMetadata != null ? traceMetadata.getSpanId() : null;

        // Logging for debug
        System.out.println("Service-B | Incoming traceparent: " + incomingTraceParent);
        System.out.println("Service-B | TraceId: " + traceId + ", SpanId: " + spanId);

        // Propagate the same trace headers to Service C
        String url = "http://" + serviceC + ":8082/ode/compute?t1=" + t1;
        HttpHeaders headers = new HttpHeaders();
        headers.set("traceparent", incomingTraceParent != null ? incomingTraceParent : ("00-" + traceId + "-" + spanId + "-01"));
        headers.set("X-NewRelic-Transaction", incomingTraceId != null ? incomingTraceId : traceId);
        headers.set("X-NewRelic-Span", incomingSpanId != null ? incomingSpanId : spanId);

        HttpEntity<String> entity = new HttpEntity<>(headers);
        String result;
        
        try {
            result = restTemplate.exchange(url, HttpMethod.GET, entity, String.class).getBody();
            String response = "Service-B | TraceId=" + (incomingTraceId != null ? incomingTraceId : traceId) + "| SpanId=" + (incomingSpanId != null ? incomingSpanId : spanId) + " | Result " + result;
            System.out.println(response);
            return ResponseEntity.ok(response);

        } catch (ResourceAccessException ex) {
            String errorMsg = "Timeout/Connection error calling Service C: " + ex.getMessage();
            logger.error("Timeout/Connection error calling Service C: {}", ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).body(errorMsg);

        } catch (HttpServerErrorException ex) {
            String errorMsg = "Service C returned error: " + ex.getStatusCode() + " - " + ex.getMessage();
            logger.error("Service C returned error: {} - {}", ex.getStatusCode(), ex.getMessage(), ex);
            return ResponseEntity.status(ex.getStatusCode()).body(errorMsg);

        } catch (Exception ex) {
            String errorMsg = "Unexpected error calling Service C: " + ex.getMessage();
            logger.error("Unexpected error calling Service C: {}", ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorMsg);
        } 
    }
}
