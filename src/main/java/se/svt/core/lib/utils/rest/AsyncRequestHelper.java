package se.svt.core.lib.utils.rest;


import se.svt.core.lib.utils.rest.support.SyntheticParametrizedTypeReference;
import se.svt.core.lib.utils.rest.util.OptionalTypeFutureAdapter;
import se.svt.core.lib.utils.rest.util.ResponseFutureAdapter;
import se.svt.core.lib.utils.rest.util.TypeUtils;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.client.AsyncRestTemplate;

import java.lang.reflect.Method;
import java.util.Optional;

import static se.svt.core.lib.utils.rest.support.SyntheticParametrizedTypeReference.fromMethodReturnType;

@RequiredArgsConstructor
class AsyncRequestHelper {

    private final AsyncRestTemplate asyncRestTemplate;

    ListenableFuture<?> executeAsyncRequest(Method method, RequestEntity<Object> requestEntity) {
        SyntheticParametrizedTypeReference<?> wrappedReturnType = fromMethodReturnType(method).getTypeArgument(0);
        if (TypeUtils.typeIsAnyOf(wrappedReturnType, HttpEntity.class, ResponseEntity.class)) {
            SyntheticParametrizedTypeReference<?> responseType = wrappedReturnType.getTypeArgument(0);
            return sendAsyncRequest(requestEntity, responseType);
        }
        ListenableFuture<? extends ResponseEntity<?>> listenableFuture = sendAsyncRequest(requestEntity, wrappedReturnType);
        if (TypeUtils.typeIs(wrappedReturnType, Optional.class)) {
            return createOptionalTypeAdapter(listenableFuture);
        }
        return createObjectAdapter(listenableFuture);
    }

    private ListenableFuture<? extends ResponseEntity<?>> sendAsyncRequest(RequestEntity<Object> requestEntity,
                                                                           SyntheticParametrizedTypeReference<?> responseType) {
        return asyncRestTemplate.exchange(requestEntity.getUrl(), requestEntity.getMethod(), requestEntity, responseType);
    }

    @SuppressWarnings("unchecked")
    private static OptionalTypeFutureAdapter<Optional<?>> createOptionalTypeAdapter(ListenableFuture<? extends ResponseEntity<?>> listenableFuture) {
        return new OptionalTypeFutureAdapter<>((ListenableFuture<ResponseEntity<Optional<?>>>) listenableFuture);
    }

    @SuppressWarnings("unchecked")
    private static ResponseFutureAdapter<Object> createObjectAdapter(ListenableFuture<? extends ResponseEntity<?>> listenableFuture) {
        return new ResponseFutureAdapter<>((ListenableFuture<ResponseEntity<Object>>) listenableFuture);
    }
}