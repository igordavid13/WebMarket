package br.gov.caixa.webmarket.pedido.infrastructure.client;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@ApplicationScoped
public class ProdutoAuthHeaderFactory implements ClientHeadersFactory {

    @Override
    public MultivaluedMap<String, String> update(
            MultivaluedMap<String, String> incomingHeaders,
            MultivaluedMap<String, String> clientOutgoingHeaders) {
        MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
        String credencial = "admin:admin123";
        String encoded = Base64.getEncoder().encodeToString(credencial.getBytes(StandardCharsets.UTF_8));
        headers.add("Authorization", "Basic " + encoded);
        return headers;
    }
}