[![Build Status][ci-img]][ci] [![Released Version][maven-img]][maven]

# OpenTracing Spring Web Instrumentation

This library provides instrumentation for Spring  Web applications (Boot and MVC). It creates tracing data for 
server requests and also client requests (`RestTemplate` and `AsyncRestTemplate`).

## How does the server tracing work?

Server span is started in [Web Servlet Filter](https://github.com/opentracing-contrib/java-web-servlet-filter),
then tracing interceptor adds spring related tags and logs. There are use case when spring boot invokes a handler after 
a request processing in filter finished, in this case interceptor starts a new span as `followsFrom` 
which references the initial span created in the servlet filter.

## Configuration

### Auto-configuration
If you are using Spring Boot the easiest way how to configure OpenTracing instrumentation is to use auto-configuration:

```xml
<dependency>
  <groupId>io.opentracing.contrib</groupId>
  <artifactId>opentracing-spring-web-autoconfigure</artifactId>
</dependency>

```
Just provide an OpenTracing tracer bean and all required configuration is automatically
done for you. It also instruments all `RestTemplate` and `AsyncRestTemplate` beans.

### Manual configuration

#### Server
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

#### Client
```java
RestTemplate restTemplate = new RestTemplate();
restTemplate.setInterceptors(Collections.singletonList(new TracingRestTemplateInterceptor(tracer)));

// the same applies for AsyncRestTemplate 
```

## Access server span
```java
@RequestMapping("/hello")
public String hello(HttpServletRequest request) {
    ActiveSpan serverSpan = tracer.activeSpan();

    ActiveSpan span = tracer.buildSpan("localSpan");
            .asChildOf(serverSpan.context())
            .start();
    span.deactivate();
    
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
   [maven-img]: https://img.shields.io/maven-central/v/io.opentracing.contrib/opentracing-spring-web.svg?maxAge=2592000
   [maven]: http://search.maven.org/#search%7Cga%7C1%7Copentracing-spring-web
