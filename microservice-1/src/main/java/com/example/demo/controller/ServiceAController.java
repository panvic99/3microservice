package com.example.demo.controller;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.TraceMetadata;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/service-a")
public class ServiceAController {

    private static final Logger logger = LoggerFactory.getLogger(ServiceAController.class);

    @Value("${http.client.connect-timeout:250}")
    private int connectTimeout;
    
    @Value("${http.client.read-timeout:500}")
    private int readTimeout;

    @Value("${service.b.url:localhost}")
    private String serviceB;

    private RestTemplate restTemplate;

    @PostConstruct
    public void init() {
        this.restTemplate = new RestTemplate();
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout);  // Now has correct value
        factory.setReadTimeout(readTimeout);        // Now has correct value
        restTemplate.setRequestFactory(factory);
        
        // Debug logging
        System.out.println("Service-B Timeouts: Connect=" + connectTimeout + "ms, Read=" + readTimeout + "ms");
        System.out.println("Service-B URL: " + serviceB);   
    }

    @Trace(dispatcher = true)
    @GetMapping("/")
    public String home() {
        return "Hello from Microservice A";
    }

    @Trace(dispatcher = true)
    @GetMapping("/start")
    public ResponseEntity<String> callServiceB(@RequestParam double t1) {
        TraceMetadata traceMetadata = NewRelic.getAgent().getTraceMetadata();
        if (traceMetadata == null) {
            // return ;   
            return ResponseEntity.ok("No trace metadata available");
        }

        String traceId = traceMetadata.getTraceId();
        String spanId = traceMetadata.getSpanId();

        System.out.println("Service-A | TraceId: " + traceId + ", SpanId: " + spanId);

        String url = "http://" + serviceB + ":8081/service-b/compute?t1=" + t1;

        // Propagate the trace headers
        HttpHeaders headers = new HttpHeaders();
        headers.set("traceparent", "00-" + traceId + "-" + spanId + "-01");
        headers.set("X-NewRelic-Transaction", traceId);
        headers.set("X-NewRelic-Span", spanId);
        // Optionally add more headers or logging here if needed
        // For example, you could log the outgoing headers for debugging:
        System.out.println("Outgoing headers: " + headers);
        
        HttpEntity<String> entity = new HttpEntity<>(headers);
        String responseFromB;
        try {
            responseFromB = restTemplate.exchange(url, HttpMethod.GET, entity, String.class).getBody();
            System.out.println(responseFromB);
            return ResponseEntity.ok(responseFromB);

        } catch (ResourceAccessException ex) {
            String errorMsg = "Timeout/Connection error calling Service B: " + ex.getMessage();
            logger.error("Timeout/Connection error calling Service B: {}", ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).body(errorMsg);

        } catch (HttpServerErrorException ex) {
            String errorMsg = "Service B returned error: " + ex.getStatusCode() + " - " + ex.getMessage();
            logger.error("Service B returned error: {} - {}", ex.getStatusCode(), ex.getMessage(), ex);
            return ResponseEntity.status(ex.getStatusCode()).body(errorMsg);

        } catch (HttpClientErrorException ex) { 
            // 4xx errors from Service B (including 408)
            String errorMsg = "Timeout/Connection error from B on calling Service C: " + ex.getMessage();
            logger.error("Timeout/Connection error from B on calling Service C: {}", ex.getMessage(), ex);
            return ResponseEntity.status(ex.getStatusCode()).body(errorMsg);

        }catch (Exception ex) {
            String errorMsg = "Unexpected error calling Service B: " + ex.getMessage();
            logger.error("Unexpected error calling Service B: {}", ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorMsg);
        }
    }
}
