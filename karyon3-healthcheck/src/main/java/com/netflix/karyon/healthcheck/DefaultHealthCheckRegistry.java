package com.netflix.karyon.healthcheck;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import javax.inject.Named;
import javax.inject.Singleton;

import com.google.inject.Binding;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;

@Singleton
public class DefaultHealthCheckRegistry implements HealthCheckRegistry {
    private ConcurrentMap<String, HealthCheck> healthChecks = new ConcurrentHashMap<String, HealthCheck>();
    private final long cacheInterval;
    
    @Inject
    public DefaultHealthCheckRegistry(Injector injector, HealthCheckConfiguration config) {
        cacheInterval = config.getCacheInterval();
        
        for (Binding<HealthCheck> binding : injector.findBindingsByType(TypeLiteral.get(HealthCheck.class))) {
            Key<HealthCheck> key = binding.getKey();
            // Ignore the top-level HealthCheck binding
            if (key.getAnnotationType() == null) {
                continue;
            }
            else if (key.getAnnotationType().equals(Named.class)) {
                healthChecks.put(
                        ((Named)key.getAnnotation()).value(), 
                        wrap(binding.getProvider().get()));
            }
            else if (key.getAnnotationType().equals(com.google.inject.name.Named.class)) {
                healthChecks.put(
                        ((com.google.inject.name.Named)key.getAnnotation()).value(), 
                        wrap(binding.getProvider().get()));
            }
            else {
                healthChecks.put(
                        key.getAnnotation().toString(), 
                        wrap(binding.getProvider().get()));
            }
        }
    }
    
    private HealthCheck wrap(HealthCheck delegate) {
        return new CachingHealthCheck(delegate, cacheInterval, TimeUnit.SECONDS);
    }
    
    @Override
    public Map<String, HealthCheck> getHealthChecks() {
        return Collections.unmodifiableMap(healthChecks);
    }
}