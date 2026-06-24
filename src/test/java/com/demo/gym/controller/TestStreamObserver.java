package com.demo.gym.controller;

import io.grpc.stub.StreamObserver;

public class TestStreamObserver<T> implements StreamObserver<T> {
    private T response;
    private Throwable error;

    @Override
    public void onNext(T value) {
        this.response = value;
    }

    @Override
    public void onError(Throwable t) {
        this.error = t;
    }

    @Override
    public void onCompleted() {
    }

    public T getResponse() {
        return response;
    }

    public Throwable getError() {
        return error;
    }
}
