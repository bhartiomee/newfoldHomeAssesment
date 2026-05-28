package com.example.loanaccount.api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.concurrent.Executor;

public class ExecutorDispatchingHandler implements HttpHandler {
    private final HttpHandler delegate;
    private final Executor executor;

    public ExecutorDispatchingHandler(HttpHandler delegate, Executor executor) {
        this.delegate = delegate;
        this.executor = executor;
    }

    @Override
    public void handle(HttpExchange exchange) {
        executor.execute(() -> {
            try {
                delegate.handle(exchange);
            } catch (IOException exception) {
                throw new IllegalStateException(exception);
            }
        });
    }
}
