package online.longlian.app.common.metrics;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.DoubleGaugeBuilder;
import io.opentelemetry.api.metrics.DoubleHistogramBuilder;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.LongCounterBuilder;
import io.opentelemetry.api.metrics.LongUpDownCounterBuilder;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.TracerProvider;
import io.opentelemetry.context.propagation.ContextPropagators;

import java.util.ArrayList;
import java.util.List;

public final class ResultMetricCapture implements AutoCloseable {

    private final List<Attributes> samples = new ArrayList<>();
    private final ApiResultMetrics metrics = new ApiResultMetrics(openTelemetry());

    public static ResultMetricCapture open() {
        return new ResultMetricCapture();
    }

    public ApiResultMetrics metrics() {
        return metrics;
    }

    public long count(int code, String route) {
        String codeLabel = Integer.toString(code);
        return samples.stream()
                .filter(sample -> codeLabel.equals(sample.get(ApiResultMetrics.RESULT_CODE))
                        && route.equals(sample.get(ApiResultMetrics.HTTP_ROUTE)))
                .count();
    }

    public boolean isEmpty() {
        return samples.isEmpty();
    }


    @Override
    public void close() {
    }

    private OpenTelemetry openTelemetry() {
        return new OpenTelemetry() {
            @Override
            public TracerProvider getTracerProvider() {
                return TracerProvider.noop();
            }

            @Override
            public ContextPropagators getPropagators() {
                return ContextPropagators.noop();
            }

            @Override
            public Meter getMeter(String instrumentationScopeName) {
                return new Meter() {
                    @Override
                    public LongCounterBuilder counterBuilder(String name) {
                        return new LongCounterBuilder() {
                            @Override
                            public LongCounterBuilder setDescription(String description) {
                                return this;
                            }

                            @Override
                            public LongCounterBuilder setUnit(String unit) {
                                return this;
                            }

                            @Override
                            public io.opentelemetry.api.metrics.DoubleCounterBuilder ofDoubles() {
                                throw new UnsupportedOperationException();
                            }

                            @Override
                            public io.opentelemetry.api.metrics.ObservableLongCounter buildWithCallback(
                                    java.util.function.Consumer<io.opentelemetry.api.metrics.ObservableLongMeasurement> callback) {
                                throw new UnsupportedOperationException();
                            }

                            @Override
                            public LongCounter build() {
                                return new LongCounter() {
                                    @Override
                                    public void add(long value) {
                                        throw new UnsupportedOperationException();
                                    }

                                    @Override
                                    public void add(long value, Attributes attributes) {
                                        samples.add(attributes);
                                    }

                                    @Override
                                    public void add(long value, Attributes attributes, io.opentelemetry.context.Context context) {
                                        add(value, attributes);
                                    }
                                };
                            }
                        };
                    }

                    @Override
                    public LongUpDownCounterBuilder upDownCounterBuilder(String name) {
                        throw new UnsupportedOperationException();
                    }

                    @Override
                    public DoubleHistogramBuilder histogramBuilder(String name) {
                        throw new UnsupportedOperationException();
                    }

                    @Override
                    public DoubleGaugeBuilder gaugeBuilder(String name) {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };
    }
}
