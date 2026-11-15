package br.gov.caixa.webmarket.produto.infrastructure.error;

import java.util.List;

public record ErroResposta(int status, String erro, List<String> mensagens) {
    public static ErroResposta de(int status, String erro, String mensagem) {
        return new ErroResposta(status, erro, List.of(mensagem));
    }
}