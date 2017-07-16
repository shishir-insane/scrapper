package com.sk.babylon.log;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class LoggerAspect {

    @Pointcut("@annotation(org.springframework.web.bind.annotation.RequestMapping)")
    public void requestMapping() {
    }

    @Pointcut("@annotation(com.sk.babylon.log.Profiled)")
    public void profiled() {

    }

    @Around("requestMapping() || profiled()")
    public Object profile(final ProceedingJoinPoint pjp) throws Throwable {
        final StopWatch sw = new StopWatch();
        final String className = pjp.getTarget().getClass().getSimpleName();
        final String methodName = pjp.getSignature().getName();
        final String name = className + "." + methodName;
        log.debug("call " + name);
        try {
            sw.start();
            return pjp.proceed();
        } finally {
            sw.stop();
            log.debug("exit " + name + " [" + sw.getTotalTimeMillis() + "ms]");
        }
    }

}