package io.opentracing.contrib.spring.web.interceptor.itest.common.app;

import java.util.concurrent.Callable;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.servlet.ModelAndView;

import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.contrib.spring.web.interceptor.TracingHandlerInterceptor;

/**
 * @author Pavol Loffay
 */
@RestController
@RequestMapping("/")
public class TestController {

    public static final String EXCEPTION_MESSAGE = "exceptionMessage";
    public static final String MAPPED_EXCEPTION_MESSAGE = "mappedExceptionMessage";

    private Tracer tracer;

    @Autowired
    public TestController(Tracer tracer){
        this.tracer = tracer;
    }

    @RequestMapping("/sync")
    public String sync() {
        return "sync";
    }

    @GetMapping(path = "/view")
    public ModelAndView view() {
        ModelAndView staticView = new ModelAndView();
        staticView.setViewName("staticView");
        return staticView;
    }

    @RequestMapping("/localSpan")
    public String localSpan(HttpServletRequest request) {
        io.opentracing.Tracer.SpanBuilder spanBuilder = tracer.buildSpan("localSpan");
        Span localSpan = spanBuilder
                .asChildOf(TracingHandlerInterceptor.serverSpanContext(request))
                .start();
        localSpan.finish();

        return "sync";
    }

    @RequestMapping("/async")
    public Callable<String> async() {
        return new Callable<String>() {
            public String call() throws Exception {
                Thread.sleep(1000);
                return "async";
            }
        };
    }

    @RequestMapping(value = "/asyncDeferred", method = RequestMethod.GET)
    public  DeferredResult<ResponseEntity<String>> test() {
        DeferredResult<ResponseEntity<String>> df = new DeferredResult<>();
        df.setResult(ResponseEntity.status(202).body("deferred"));
        return df;
    }

    @RequestMapping("/secured")
    public String secured() {
        return  "secured";
    }

    @RequestMapping(path = {"/wildcard/{param}/{numericId:[\\d]+}", "/foobar"})
    public String wildcardMapping(@PathVariable String param,  @PathVariable long numericId) {
        return "wildcard";
    }

    @RequestMapping(path = "redirect")
    public ModelAndView redirect(HttpServletResponse response) {
        return new ModelAndView("redirect:/sync");
    }

    @RequestMapping(path = "forward")
    public ModelAndView forward(HttpServletResponse response) {
        return new ModelAndView("forward:/sync");
    }

    @RequestMapping("/exception")
    public String exception() throws Exception {
        throw new Exception(EXCEPTION_MESSAGE);
    }

    @RequestMapping("/mappedException")
    public String businessException() {
        throw new MappedException(MAPPED_EXCEPTION_MESSAGE);
    }

    @ResponseStatus(value = HttpStatus.CONFLICT, reason = "Data integrity violation")
    @ExceptionHandler(MappedException.class)
    public void mappedExceptionHandler(MappedException ex) {
        // Nothing to do
    }

    public static class MappedException extends RuntimeException {
        public MappedException() {}

        public MappedException(String message) {
            super(message);
        }
    }
}
