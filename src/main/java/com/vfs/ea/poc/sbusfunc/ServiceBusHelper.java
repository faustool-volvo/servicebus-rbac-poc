package com.vfs.ea.poc.sbusfunc;

import java.time.Duration;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Locale;

import javax.enterprise.context.ApplicationScoped;

import com.azure.core.credential.TokenCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.azure.identity.ManagedIdentityCredentialBuilder;
import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.github.javafaker.Faker;

@ApplicationScoped
public class ServiceBusHelper {

    protected TokenCredential getCredential(ServiceBusParams params) {

        switch (params.getAccountType()) {
            case ServicePrincipal:
                return new ClientSecretCredentialBuilder()
                        .clientId(params.getClientId())
                        .tenantId(params.getTenantId())
                        .clientSecret(params.getClientSecret())
                        .build();
            case ManagedIdentity:
                return new ManagedIdentityCredentialBuilder()
                        .clientId(params.getClientId())
                        .build();
            default:
                throw new IllegalArgumentException("Invalid account type: " + params.getAccountType().name());
        }
    }

    public String send(ServiceBusParams params) {
        final String message;
        if (params.getMessage() == null || params.getMessage().trim().isEmpty()) {
            var faker = new Faker(Locale.US);
            message = faker.chuckNorris().fact();
        } else {
            message = params.getMessage();
        }
        try (var sender = new ServiceBusClientBuilder()
                .credential(this.getCredential(params))
                .fullyQualifiedNamespace(params.getNamespace())
                .sender()
                .topicName(params.getTopicName())
                .buildClient()) {
            sender.sendMessage(new ServiceBusMessage(message));
        }
        return message;
    }

    public Collection<String> receive(ServiceBusParams params) {
        try (var receiver = new ServiceBusClientBuilder()
                .credential(getCredential(params))
                .fullyQualifiedNamespace(params.getNamespace())
                .receiver()
                .topicName(params.getTopicName())
                .subscriptionName(params.getSubscription())
                .buildClient()) {
            final var messages = new LinkedList<String>();

            receiver.receiveMessages(Integer.MAX_VALUE, Duration.ofSeconds(1L)).forEach(m -> {
                messages.add(m.getBody().toString());
                receiver.complete(m);
            });

            return messages;
        }
    }

}
