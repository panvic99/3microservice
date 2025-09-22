package com.example.demo.controller;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.TraceMetadata;
import org.apache.commons.math3.ode.FirstOrderDifferentialEquations;
import org.apache.commons.math3.ode.FirstOrderIntegrator;
import org.apache.commons.math3.ode.nonstiff.DormandPrince853Integrator;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/ode")
public class ServiceCController {

    private static final Logger logger = LoggerFactory.getLogger(ServiceCController.class);

    @GetMapping("/compute")
    @Trace(dispatcher = true)
    public String computeODE(@RequestParam double t1, HttpServletRequest request) {
        // For debug, show incoming headers
        String incomingTraceParent = request.getHeader("traceparent");
        String incomingTraceId = request.getHeader("X-NewRelic-Transaction");
        String incomingSpanId = request.getHeader("X-NewRelic-Span");
        System.out.println("Service-C | Incoming X-NewRelic-Transaction (TraceId): " + incomingTraceId);
        System.out.println("Service-C | Incoming X-NewRelic-Span (SpanId): " + incomingSpanId);

        TraceMetadata traceMetadata = NewRelic.getAgent().getTraceMetadata();
        String traceId = traceMetadata != null ? traceMetadata.getTraceId() : null;
        String spanId = traceMetadata != null ? traceMetadata.getSpanId() : null;

        System.out.println("Service-C | Incoming traceparent: " + incomingTraceParent);
        System.out.println("Service-C | TraceId: " + traceId + ", SpanId: " + spanId);

        try {

            if (t1 < 0) {
                throw new IllegalArgumentException("t1 cannot be negative: " + t1);
            }
            else{
                // Define the Robertson equations
                FirstOrderDifferentialEquations equations = new RobertsonEquations();

                // Initial conditions
                double[] y = new double[]{1.0, 0.0, 0.0};
                double t0 = 0.0;

                // Use Dormand-Prince integrator
                FirstOrderIntegrator integrator = new DormandPrince853Integrator(
                        1e-8, 1e5, 1e-10, 1e-10);

                // Perform integration
                integrator.integrate(equations, t0, y, t1, y);

                // Return formatted result
                return String.format("Result at t = %.2e: y1 = %.6f, y2 = %.6f, y3 = %.6f",
                        t1, y[0], y[1], y[2]);
            }
        } catch (Exception e) {
            // System.out.println("t1 cannot be negative: " + t1);
            logger.error("t1 cannot be negative: " + t1);
            throw new IllegalArgumentException("t1 cannot be negative: " + t1);
            //return "Error during ODE computation: " + e.getMessage();
        }
    }

    // Robertson ODE equations
    static class RobertsonEquations implements FirstOrderDifferentialEquations {
        @Override
        public int getDimension() {
            return 3;
        }

        @Override
        public void computeDerivatives(double t, double[] y, double[] yDot) {
            yDot[0] = -0.04 * y[0] + 1e4 * y[1] * y[2];
            yDot[1] = 0.04 * y[0] - 1e4 * y[1] * y[2] - 3e7 * y[1] * y[1];
            yDot[2] = 3e7 * y[1] * y[1];
        }
    }
}
