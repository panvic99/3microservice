package com.example.demo.controller;
import org.apache.commons.math3.ode.FirstOrderDifferentialEquations;
import org.apache.commons.math3.ode.FirstOrderIntegrator;
import org.apache.commons.math3.ode.nonstiff.DormandPrince853Integrator;
 
public class RobertsonODE {
 
    public static void main(String[] args) {
        FirstOrderDifferentialEquations equations = new RobertsonEquations();
 
        double[] y = new double[] {1.0, 0.0, 0.0}; // Initial conditions
        double t0 = 0.0;
        double t1 = 1e5;
 
        FirstOrderIntegrator integrator = new DormandPrince853Integrator(1e-8, 1e5, 1e-10, 1e-10);
        integrator.integrate(equations, t0, y, t1, y);
 
        System.out.printf("At t = %.2e, y1 = %.6f, y2 = %.6f, y3 = %.6f%n", t1, y[0], y[1], y[2]);
    }
 
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


/** 
import org.springframework.web.bind.annotation.*;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.NewRelic;

@RestController
@RequestMapping("/ode")
public class ODEController {

    @GetMapping("/compute")
    @Trace(dispatcher = true)
    public String compute(@RequestParam double t1) {
        RobertsonEquations equations = new RobertsonEquations();
        double[] y = new double[]{1.0, 0.0, 0.0};

        long start = System.currentTimeMillis();
        FirstOrderIntegrator integrator = new DormandPrince853Integrator(1e-8, 1e5, 1e-10, 1e-10);
        integrator.integrate(equations, 0.0, y, t1, y);
        long end = System.currentTimeMillis();

        String traceId = NewRelic.getAgent().getTransaction().getTraceMetadata().getTraceId();
        return String.format("TraceID=%s | Time=%dms | Result: y1=%.6f y2=%.6f y3=%.6f",
                traceId, (end - start), y[0], y[1], y[2]);
    }
}
*/