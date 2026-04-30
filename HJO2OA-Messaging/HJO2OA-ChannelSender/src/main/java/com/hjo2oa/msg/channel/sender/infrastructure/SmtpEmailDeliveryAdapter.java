package com.hjo2oa.msg.channel.sender.infrastructure;

import com.hjo2oa.msg.channel.sender.application.ChannelDeliveryAdapter;
import com.hjo2oa.msg.channel.sender.application.ChannelDeliveryRequest;
import com.hjo2oa.msg.channel.sender.application.ChannelDeliveryResult;
import com.hjo2oa.msg.channel.sender.domain.ChannelEndpointStatus;
import com.hjo2oa.msg.channel.sender.domain.ChannelType;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Component;

@Component
public class SmtpEmailDeliveryAdapter implements ChannelDeliveryAdapter {

    @Override
    public ChannelType channelType() {
        return ChannelType.EMAIL;
    }

    @Override
    public ChannelDeliveryResult send(ChannelDeliveryRequest request) {
        if (request.endpoint() == null) {
            return ChannelDeliveryResult.failure("MSG_ENDPOINT_UNAVAILABLE", "No enabled SMTP endpoint is configured", null);
        }
        if (request.endpoint().status() != ChannelEndpointStatus.ENABLED) {
            return ChannelDeliveryResult.failure("MSG_ENDPOINT_DISABLED", "Endpoint is disabled", null);
        }
        URI uri = toSmtpUri(request.endpoint().configRef());
        if (uri == null || uri.getHost() == null) {
            return ChannelDeliveryResult.failure(
                    "MSG_ENDPOINT_CONFIG_INVALID",
                    "Email configRef must be smtp://host:port?from=sender@example.com",
                    null
            );
        }
        String from = resolveFrom(uri);
        if (from == null || request.recipientId() == null || request.recipientId().isBlank()) {
            return ChannelDeliveryResult.failure("MSG_EMAIL_ADDRESS_INVALID", "Email sender or recipient is blank", null);
        }
        try {
            deliver(uri, from, request);
            return ChannelDeliveryResult.success(
                    "smtp:" + request.deliveryTaskId(),
                    "{\"channel\":\"EMAIL\",\"status\":\"ACCEPTED\"}"
            );
        } catch (IOException ex) {
            return ChannelDeliveryResult.failure("MSG_SMTP_FAILED", ex.getMessage(), null);
        }
    }

    private void deliver(URI uri, String from, ChannelDeliveryRequest request) throws IOException {
        int port = uri.getPort() > 0 ? uri.getPort() : 25;
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(uri.getHost(), port), 5000);
            socket.setSoTimeout(5000);
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
            expect(reader, 220);
            command(writer, "HELO hjo2oa.local");
            expect(reader, 250);
            command(writer, "MAIL FROM:<" + from + ">");
            expect(reader, 250);
            command(writer, "RCPT TO:<" + request.recipientId() + ">");
            expect(reader, 250, 251);
            command(writer, "DATA");
            expect(reader, 354);
            command(writer, "Subject: " + sanitizeHeader(request.title()));
            command(writer, "Content-Type: text/plain; charset=UTF-8");
            command(writer, "");
            command(writer, request.body() == null ? "" : request.body());
            if (request.deepLink() != null && !request.deepLink().isBlank()) {
                command(writer, "");
                command(writer, request.deepLink());
            }
            command(writer, ".");
            expect(reader, 250);
            command(writer, "QUIT");
        }
    }

    private void command(BufferedWriter writer, String command) throws IOException {
        writer.write(command);
        writer.write("\r\n");
        writer.flush();
    }

    private void expect(BufferedReader reader, int... acceptedCodes) throws IOException {
        String line = reader.readLine();
        if (line == null || line.length() < 3) {
            throw new IOException("SMTP server closed the connection");
        }
        int code = Integer.parseInt(line.substring(0, 3));
        for (int acceptedCode : acceptedCodes) {
            if (code == acceptedCode) {
                return;
            }
        }
        throw new IOException("SMTP server returned " + line);
    }

    private URI toSmtpUri(String configRef) {
        if (configRef == null || configRef.isBlank()) {
            return null;
        }
        URI uri = URI.create(configRef.trim());
        return "smtp".equalsIgnoreCase(uri.getScheme()) ? uri : null;
    }

    private String resolveFrom(URI uri) {
        String query = uri.getQuery();
        if (query == null) {
            return null;
        }
        for (String part : query.split("&")) {
            String[] tokens = part.split("=", 2);
            if (tokens.length == 2 && "from".equals(tokens[0])) {
                return tokens[1];
            }
        }
        return null;
    }

    private String sanitizeHeader(String value) {
        return value == null ? "" : value.replace("\r", " ").replace("\n", " ");
    }
}
