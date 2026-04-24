package com.hjo2oa.data.connector.infrastructure;

import com.hjo2oa.data.connector.domain.ConnectorFailureReason;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.springframework.stereotype.Component;

@Component
public class JavaNetHttpConnectivityClient implements HttpConnectivityClient {

    @Override
    public int execute(HttpRequestSpec requestSpec) {
        try {
            HttpClient httpClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofMillis(requestSpec.timeoutRetryConfig().connectTimeoutMs()))
                    .build();
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(requestSpec.url()))
                    .timeout(Duration.ofMillis(requestSpec.timeoutRetryConfig().readTimeoutMs()));
            requestSpec.headers().forEach(requestBuilder::header);
            requestBuilder.method(requestSpec.method(), HttpRequest.BodyPublishers.noBody());
            HttpResponse<Void> response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.discarding());
            return response.statusCode();
        } catch (IllegalArgumentException ex) {
            throw ConnectorConnectivityException.of(ConnectorFailureReason.CONFIGURATION_ERROR, "HTTP 连接参数非法", ex);
        } catch (IOException ex) {
            throw ConnectorConnectivityException.of(ConnectorFailureReason.NETWORK_UNREACHABLE, "HTTP 目标不可达", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw ConnectorConnectivityException.of(ConnectorFailureReason.UNKNOWN, "HTTP 连接测试被中断", ex);
        }
    }
}
