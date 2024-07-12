package org.nkumar.utilities.traceall;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

@Aspect
public class AllTracingAspect {
    @Pointcut("execution(* *(..)) || execution(*.new(..))")
    public void tracingPoints() {
    }

    @Around("tracingPoints()")
    public Object aroundTracingPoints(final ProceedingJoinPoint joinPoint) throws Throwable {
        final JoinPoint.StaticPart joinPointStaticPart = joinPoint.getStaticPart();
        Trace.traceEntry(joinPointStaticPart);
        final Object returnValue;
        try {
            returnValue = joinPoint.proceed(joinPoint.getArgs());
        } catch (Throwable ex) {
            Trace.traceExit(joinPointStaticPart, false);
            throw ex;
        }
        Trace.traceExit(joinPointStaticPart, true);
        return returnValue;
    }
}
