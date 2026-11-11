package com.cinemind.gateway.error;

import java.net.ConnectException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import reactor.core.publisher.Mono;

/**
 * Turns downstream connection failures into a clean JSON 503 instead of the
 * default HTML/500 error page. Runs before Boot's DefaultErrorWebExceptionHandler
 * (order -1).
 */
@Component
@Order(-2)
public class GatewayErrorHandler implements ErrorWebExceptionHandler {

    private final ObjectMapper objectMapper;

    public GatewayErrorHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        if (exchange.getResponse().isCommitted()) {
            return Mono.error(ex);
        }

        HttpStatus status = resolveStatus(ex);
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        // ISO-8601 string keeps serialization independent of Jackson's
        // JavaTimeModule registration (Instant needs that module).
        ApiErrorResponse body = new ApiErrorResponse(
                Instant.now().toString(),
                status.value(),
                status.getReasonPhrase(),
                messageFor(status),
                exchange.getRequest().getPath().value());

        byte[] payload;
        try {
            payload = objectMapper.writeValueAsBytes(body);
        } catch (JsonProcessingException e) {
            payload = ("{\"status\":" + status.value() + ",\"message\":\"Unexpected gateway error\"}")
                    .getBytes(StandardCharsets.UTF_8);
        }

        return exchange.getResponse()
                .writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(payload)));
    }

    /** Package-visible for unit tests. Walks the cause chain, bounded to avoid cycles. */
    static HttpStatus resolveStatus(Throwable ex) {
        Throwable current = ex;
        for (int depth = 0; current != null && depth < 12; depth++, current = current.getCause()) {
            String type = current.getClass().getName();
            String message = String.valueOf(current.getMessage()).toLowerCase();

            boolean downstreamUnavailable = current instanceof ConnectException
                    || type.contains("WebClientRequestException")
                    || type.contains("NoReachableHostException")
                    || type.contains("ServiceUnavailableException")
                    || message.contains("unable to find instance")
                    || message.contains("no instances available")
                    || (type.contains("NotFoundException") && message.contains("instance"));

            if (downstreamUnavailable) {
                return HttpStatus.SERVICE_UNAVAILABLE;
            }
            if (current instanceof ResponseStatusException responseStatusException) {
                int code = responseStatusException.getStatusCode().value();
                return HttpStatus.resolve(code) != null
                        ? HttpStatus.valueOf(code)
                        : HttpStatus.INTERNAL_SERVER_ERROR;
            }
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private static String messageFor(HttpStatus status) {
        if (status == HttpStatus.SERVICE_UNAVAILABLE) {
            return "Downstream service is unavailable. Please try again shortly.";
        }
        if (status == HttpStatus.NOT_FOUND) {
            return "No gateway route matches this request.";
        }
        return "Unexpected gateway error.";
    }
}
