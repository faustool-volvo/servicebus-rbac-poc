package com.vfs.ea.poc.sbusfunc;

import javax.inject.Inject;

import io.quarkus.funqy.Funq;

public class ServiceBusRBACPOCFunction {

    private final ServiceBusHelper sbus;

    @Inject
    public ServiceBusRBACPOCFunction(ServiceBusHelper sbus) {
        this.sbus = sbus;
    }

    @Funq
    public String funqyHello() {
        return "hello funqy";
    }

    @Funq
    public FunctionResponse send(ServiceBusParams params) {
        try {
            String message = sbus.send(params);
            return FunctionResponse.success(message);
        } catch (Exception e) {
            return FunctionResponse.error(e);
        }
    }

    @Funq
    public FunctionResponse receive(ServiceBusParams params) {
        if (params.getSubscription() == null || params.getSubscription().trim().isEmpty()) {
            return FunctionResponse.error("Missing subscription");
        }
        try {
            return FunctionResponse.success(sbus.receive(params));
        } catch (Exception e) {
            return FunctionResponse.error(e);
        }
    }
}
