package io.jans.ca.server;

import brave.Tracing;
import brave.opentracing.BraveTracer;
import com.google.common.base.Strings;
import io.jaegertracing.Configuration;
import io.jaegertracing.samplers.ConstSampler;
import io.opentracing.Scope;
import io.opentracing.Tracer;
import io.opentracing.noop.NoopTracerFactory;
import io.opentracing.tag.Tags;
import io.opentracing.util.GlobalTracer;
import zipkin2.Span;
import zipkin2.reporter.AsyncReporter;
import zipkin2.reporter.Reporter;
import zipkin2.reporter.okhttp3.OkHttpSender;

public class TracingUtil {

    private TracingUtil() {
    }

    public static boolean configureGlobalTracer(RpServerConfiguration configuration, String componentName) {
        GlobalTracer.register(createTracer(configuration, componentName));
        return true;
    }

    private static Tracer createTracer(RpServerConfiguration configuration, String componentName) {
        String tracerName = configuration.getTracer();

        if (!configuration.getEnableTracing() || Strings.isNullOrEmpty(tracerName)) {
            return NoopTracerFactory.create();
        } else if ("jaeger".equals(tracerName)) {
            Configuration.SamplerConfiguration samplerConfig = new Configuration.SamplerConfiguration()
                    .withType(ConstSampler.TYPE)
                    .withParam(1);

            Configuration.SenderConfiguration senderConfig = new Configuration.SenderConfiguration()
                    .withAgentHost(configuration.getTracerHost())
                    .withAgentPort(configuration.getTracerPort());

            Configuration.ReporterConfiguration reporterConfig = new Configuration.ReporterConfiguration()
                    .withLogSpans(true)
                    .withFlushInterval(1000)
                    .withMaxQueueSize(10000)
                    .withSender(senderConfig);

            return new Configuration(componentName)
                    .withSampler(samplerConfig)
                    .withReporter(reporterConfig)
                    .getTracer();
        } else if ("zipkin".equals(tracerName)) {
            OkHttpSender sender = OkHttpSender.create(
                    "http://" + configuration.getTracerHost() + ":" + configuration.getTracerPort() + "/api/v1/spans");

            Reporter<Span> reporter = AsyncReporter.builder(sender).build();

            return BraveTracer.create(Tracing.newBuilder()
                    .localServiceName(componentName)
                    .spanReporter(reporter)
                    .build());
        } else {
            return NoopTracerFactory.create();
        }
    }

    private static Tracer getGlobalTracer() {
        return GlobalTracer.get();
    }

    public static Scope buildSpan(String spanName, boolean startActive) {
        return getGlobalTracer().buildSpan(spanName).startActive(startActive);
    }

    public static io.opentracing.Span buildChildSpan(io.opentracing.Span parentSpan, String childSpanName) {
        return getGlobalTracer().buildSpan(childSpanName).asChildOf(parentSpan).start();
    }

    public static io.opentracing.Span getActiveSpan() {
        return getGlobalTracer().activeSpan();
    }

    public static void setTag(String tagName, String tagValue) {
        if (getGlobalTracer().activeSpan() != null) {
            getGlobalTracer().activeSpan().setTag(tagName, tagValue);
        }
    }

    public static void log(String message) {
        if (getActiveSpan() != null) {
            getActiveSpan().log(message);
        }
    }

    public static void errorLog(Exception e) {
        if (getActiveSpan() != null) {
            Tags.ERROR.set(getActiveSpan(), true);
            getActiveSpan().log("Error: " + e.getMessage());
        }
    }

    public static void errorLog(String message) {
        if (getActiveSpan() != null) {
            Tags.ERROR.set(getActiveSpan(), true);
            getActiveSpan().log("Error: " + message);
        }
    }

}
