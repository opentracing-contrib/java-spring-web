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
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.servlet.ModelAndView;

import io.opentracing.ActiveSpan;
import io.opentracing.Span;
import io.opentracing.Tracer;

/**
 * @author Pavol Loffay
 */
@RestController
@RequestMapping("/")
public class TestController {

    public static final String EXCEPTION_MESSAGE = "exceptionMessage";
    public static final String MAPPED_EXCEPTION_MESSAGE = "mappedExceptionMessage";

    private Tracer tracer;
    private RestTemplate restTemplate;

    @Autowired
    public TestController(Tracer tracer, RestTemplate restTemplate){
        this.tracer = tracer;
        this.restTemplate = restTemplate;
    }

    @RequestMapping("/health")
    public String health() {
        return "Woohoo!";
    }

    @RequestMapping("/sync")
    public String sync() {
        verifyActiveSpan();
        return "sync";
    }

    @GetMapping(path = "/view")
    public ModelAndView view() {
        verifyActiveSpan();
        ModelAndView staticView = new ModelAndView();
        staticView.setViewName("staticView");
        return staticView;
    }

    @RequestMapping("/localSpan")
    public String localSpan(HttpServletRequest request) {
        verifyActiveSpan();
        io.opentracing.Tracer.SpanBuilder spanBuilder = tracer.buildSpan("localSpan");
        Span localSpan = spanBuilder
                .startManual();
        localSpan.finish();

        return "sync";
    }

    @RequestMapping("/async")
    public Callable<String> async() {
        verifyActiveSpan();
        final ActiveSpan.Continuation cont = tracer.activeSpan().capture();
        return new Callable<String>() {
            public String call() throws Exception {
                try (ActiveSpan span = cont.activate()) {
                    if (tracer.activeSpan() == null) {
                        throw new RuntimeException("No active span");
                    }
                    Thread.sleep(1000);
                    return "async";
                }
            }
        };
    }

    @RequestMapping(value = "/asyncDeferred", method = RequestMethod.GET)
    public  DeferredResult<ResponseEntity<String>> test() {
        verifyActiveSpan();
        DeferredResult<ResponseEntity<String>> df = new DeferredResult<>();
        df.setResult(ResponseEntity.status(202).body("deferred"));
        return df;
    }

    @RequestMapping("/secured")
    public String secured() {
        verifyActiveSpan();
        return  "secured";
    }

    @RequestMapping(path = {"/wildcard/{param}/{numericId:[\\d]+}", "/foobar"})
    public String wildcardMapping(@PathVariable String param,  @PathVariable long numericId) {
        verifyActiveSpan();
        return "wildcard";
    }

    @RequestMapping(path = "redirect")
    public ModelAndView redirect(HttpServletResponse response) {
        verifyActiveSpan();
        return new ModelAndView("redirect:/sync");
    }

    @RequestMapping(path = "forward")
    public ModelAndView forward(HttpServletResponse response) {
        verifyActiveSpan();
        return new ModelAndView("forward:/sync");
    }

    @RequestMapping("/exception")
    public String exception() throws Exception {
        verifyActiveSpan();
        throw new Exception(EXCEPTION_MESSAGE);
    }

    @RequestMapping("/mappedException")
    public String businessException() {
        verifyActiveSpan();
        throw new MappedException(MAPPED_EXCEPTION_MESSAGE);
    }

    @ResponseStatus(value = HttpStatus.CONFLICT, reason = "Data integrity violation")
    @ExceptionHandler(MappedException.class)
    public void mappedExceptionHandler(MappedException ex) {
        verifyActiveSpan();

        // Nothing to do
    }

    private void verifyActiveSpan() {
        if (tracer.activeSpan() == null) {
            throw new RuntimeException("No active span");
        }
    }

    public static class MappedException extends RuntimeException {
        public MappedException() {}

        public MappedException(String message) {
            super(message);
        }
    }
}
