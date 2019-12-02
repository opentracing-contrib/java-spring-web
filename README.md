[![Build Status][ci-img]][ci] [![Released Version][maven-img]][maven]

# OpenTracing Spring Web Instrumentation

This library provides instrumentation for Spring Web applications (Boot, MVC and WebFlux). It creates tracing data for 
server requests and also client requests (`RestTemplate`, `AsyncRestTemplate` and `WebClient`).

## Comparison with [opentracing-spring-cloud](https://github.com/opentracing-contrib/java-spring-cloud)

As it was mentioned above, this library traces only inbound/outbound HTTP requests. If you would like to 
get automatically traced different set of technologies e.g. `spring-cloud-netflix`, JMS or even more then
use project [opentracing-spring-cloud](https://github.com/opentracing-contrib/java-spring-cloud) instead.

For reactive applications, it is especially recommended to use `reactor` tracing from
[opentracing-spring-cloud](https://github.com/opentracing-contrib/java-spring-cloud), as that will ensure
that the `Span` is activated in reactor handler functions. (Without that, one would have to extract the
`Span` from the subscriber context.)

## How does the server tracing work?

### Servlet
Server span is started in [Web Servlet Filter](https://github.com/opentracing-contrib/java-web-servlet-filter),
then tracing interceptor adds spring related tags and logs. There are use case when spring boot invokes a handler after 
a request processing in filter finished, in this case interceptor starts a new span as `followsFrom` 
which references the initial span created in the servlet filter.

### Reactive
Server span is started in [TracingWebFilter](opentracing-spring-web/src/main/java/io/opentracing/contrib/spring/web/webfilter/TracingWebFilter.java)
(upon subscription), then `onNext()`, `onError()`, etc. handlers add Spring WebFlux related tags and logs.

## Library versions

Versions 1.x.y, 2.x.y, ... of the library are meant to target Spring Framework 5.x and Spring Boot 2.x while versions 0.x.y are meant to be used with Spring Framework 4.3 and Spring Boot 1.5


## Configuration

### Spring Boot Auto-configuration
If you are using Spring Boot the easiest way how to configure OpenTracing instrumentation is to use auto-configuration:

```xml
<dependency>
  <groupId>io.opentracing.contrib</groupId>
  <artifactId>opentracing-spring-web-starter</artifactId>
</dependency>

```
Just provide an OpenTracing tracer bean and all required configuration is automatically
done for you. It also instruments all `RestTemplate`, `AsyncRestTemplate`, `WebClient` and `WebClient.Builder` beans.

### Manual configuration

#### Servlet and MVC Server
Configuration needs to add `TracingFilter` and `TracingHandlerInterceptor`. Both of these classes
are required!

Tracing interceptor can be instantiated manually or injected via CDI, but
it needs bean of type `Tracer` configured.

Java based configuration:
```java
@Configuration
@Import({TracingHandlerInterceptor.class})
public class MVCConfiguration extends WebMvcConfigurerAdapter {

    @Autowired
    private Tracer tracer;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new TracingHandlerInterceptor(tracer));
    }

    @Bean
    public FilterRegistrationBean tracingFilter() {
        TracingFilter tracingFilter = new TracingFilter(tracer);

        FilterRegistrationBean filterRegistrationBean = new FilterRegistrationBean(tracingFilter);
        filterRegistrationBean.addUrlPatterns("/*");
        filterRegistrationBean.setOrder(Integer.MIN_VALUE);
        filterRegistrationBean.setAsyncSupported(true);

        return filterRegistrationBean;
    }
}
```

XML based configuration can be used too. Filter can be also directly defined in `web.xml`.

#### Reactive Server
Configuration needs to add the `TracingWebFilter` bean.

```java
@Configuration
class TracingConfiguration {
    @Bean
    public TracingWebFilter tracingWebFilter(Tracer tracer) {
        return new TracingWebFilter(
                tracer,
                Integer.MIN_VALUE,               // Order
                Pattern.compile(""),             // Skip pattern
                Collections.emptyList(),         // URL patterns, empty list means all
                Arrays.asList(new WebFluxSpanDecorator.StandardTags(), new WebFluxSpanDecorator.WebFluxTags())
        );
    }
}
```

#### Client
```java
RestTemplate restTemplate = new RestTemplate();
restTemplate.setInterceptors(Collections.singletonList(new TracingRestTemplateInterceptor(tracer)));

// the same applies for AsyncRestTemplate 
```

#### Reactive Client
```java
WebClient webClient = WebClient.builder()
        .filter(new TracingExchangeFilterFunction(tracer, Collections.singletonList(new WebClientSpanDecorator.StandardTags())))
        .build();
```

## Access server span
```java
@RequestMapping("/hello")
public String hello(HttpServletRequest request) {
    Span serverSpan = tracer.activeSpan();

    Span span = tracer.buildSpan("localSpan")
                      .asChildOf(serverSpan.context())
                      .start();
    try {
        // Traced work happens between start() and deactivate();
        return "Hello world!";
    } finally {
        span.finish();
    }
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
   [maven-img]: https://img.shields.io/maven-central/v/io.opentracing.contrib/opentracing-spring-web.svg?maxAge=2592000
   [maven]: http://search.maven.org/#search%7Cga%7C1%7Copentracing-spring-web
