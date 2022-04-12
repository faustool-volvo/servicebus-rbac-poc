package com.vfs.ea.poc.sbusfunc;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FunctionResponse {

    private static final Logger logger = LoggerFactory.getLogger(FunctionResponse.class);

    private final boolean success;
    private final Collection<String> messages;

    public FunctionResponse(boolean success, Collection<String> messages) {
        this.success = success;
        this.messages = messages;
    }

    public static FunctionResponse success(String message) {
        return new FunctionResponse(true, message);
    }

    public static FunctionResponse success(Collection<String> messages) {
        return new FunctionResponse(true, messages);
    }

    public static FunctionResponse error(String message) {
        return new FunctionResponse(false, message);
    }

    public static FunctionResponse error(Exception error) {
        try (var sw = new StringWriter()) {
            error.printStackTrace(new PrintWriter(sw));
            return new FunctionResponse(false, sw.toString());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return new FunctionResponse(false, error.getMessage());
        }
    }

    private FunctionResponse(boolean success, String ... messages) {
        this.success = success;
        this.messages = Arrays.asList(messages);
    }

    public boolean isSuccess() {
        return success;
    }

    public Collection<String> getMessages() {
        return messages;
    }

    
}
