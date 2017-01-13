[![Build Status][ci-img]][ci] [![Released Version][maven-img]][maven]

# OpenTracing Spring Web Handler Interceptor

This library provides instrumentation for Spring  Web applications.

## How does this work?

Server span is started in [Web Servlet Filter](https://github.com/opentracing-contrib/java-web-servlet-filter),
then tracing interceptor adds spring related logs, for instance when `preHandle` and `afterCompletion` for given
handler were called. There are use case when spring boot invokes a handler after a request processing in filter
finished, in this case interceptor starts a new span as `followsFrom` which references initial span created in
the servlet filter.


## Configuration

Configuration needs to add `TracingFilter` and `TracingHandlerInterceptor`. Both of these classes
are required!

Tracing interceptor can be instantiated manually or injected via CDI, but
it needs beans of type `Tracer` and `List<SpanDecorator>` configured.

Java based configuration:
```java
@Configuration
@Import({TracingHandlerInterceptor.class})
public class MVCConfiguration extends WebMvcConfigurerAdapter {

    @Autowired
    private Tracer tracer;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new TracingHandlerInterceptor(tracer, Arrays.asList(SpanDecorator.STANDARD_TAGS)));
    }

    @Bean
    public FilterRegistrationBean tracingFilter() {
        TracingFilter tracingFilter = new TracingFilter(tracer, Arrays.asList(io.opentracing.contrib.web.servlet.filter.SpanDecorator.STANDARD_TAGS));

        FilterRegistrationBean filterRegistrationBean = new FilterRegistrationBean(tracingFilter);
        filterRegistrationBean.addUrlPatterns("/*");
        filterRegistrationBean.setOrder(Integer.MIN_VALUE);
        filterRegistrationBean.setAsyncSupported(true);

        return filterRegistrationBean;
    }
}
```

XML based configuration can be used too. Filter can be also directly defined in `web.xml`.

## Access server span context
```java
@RequestMapping("/hello")
public String hello(HttpServletRequest request) {
    SpanContext serverSpanContext = TracingHandlerInterceptor.serverSpanContext(request);

    Span span = tracer.buildSpan("localSpan");
            .asChildOf(serverSpanContext)
            .start();

    span.finish();
    return "Hello world!";
}
```

## Development
```shell
./mvnw clean install
```

## Release
Follow instructions in [RELEASE](RELEASE.md)


   [ci-img]: https://travis-ci.org/opentracing-contrib/java-spring-web.svg?branch=master
   [ci]: https://travis-ci.org/opentracing-contrib/java-spring-web
   [maven-img]: https://img.shields.io/maven-central/v/io.opentracing.contrib/opentracing-spring-web-handler-interceptor.svg?maxAge=2592000
   [maven]: http://search.maven.org/#search%7Cga%7C1%7Copentracing-spring-web-handler-interceptor
