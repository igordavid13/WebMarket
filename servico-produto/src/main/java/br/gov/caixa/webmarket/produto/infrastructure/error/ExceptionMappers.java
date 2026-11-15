package br.gov.caixa.webmarket.produto.infrastructure.error;

import br.gov.caixa.webmarket.produto.domain.exception.EstoqueInsuficienteException;
import br.gov.caixa.webmarket.produto.domain.exception.ProdutoDuplicadoException;
import br.gov.caixa.webmarket.produto.domain.exception.ProdutoNaoEncontradoException;
import io.quarkus.security.ForbiddenException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import java.util.List;

public class ExceptionMappers {

    @ServerExceptionMapper
    public RestResponse<ErroResposta> validacao(ConstraintViolationException e) {
        List<String> mensagens = e.getConstraintViolations().stream()
                .map(ExceptionMappers::descrever)
                .sorted()
                .toList();
        return resposta(new ErroResposta(400, "Requisição inválida", mensagens));
    }

    @ServerExceptionMapper
    public RestResponse<ErroResposta> naoEncontrado(ProdutoNaoEncontradoException e) {
        return resposta(ErroResposta.de(404, "Produto não encontrado", e.getMessage()));
    }

    @ServerExceptionMapper
    public RestResponse<ErroResposta> duplicado(ProdutoDuplicadoException e) {
        return resposta(ErroResposta.de(409, "Produto duplicado", e.getMessage()));
    }

    @ServerExceptionMapper
    public RestResponse<ErroResposta> estoqueInsuficiente(EstoqueInsuficienteException e) {
        return resposta(ErroResposta.de(409, "Estoque insuficiente", e.getMessage()));
    }

    private static String descrever(ConstraintViolation<?> v) {
        String campo = "";
        for (Path.Node node : v.getPropertyPath()) {
            campo = node.getName();
        }
        return campo.isBlank() ? v.getMessage() : campo + ": " + v.getMessage();
    }

    private static RestResponse<ErroResposta> resposta(ErroResposta erro) {
        return RestResponse.ResponseBuilder.<ErroResposta>create(erro.status()).entity(erro).build();
    }

    @ServerExceptionMapper
    public RestResponse<ErroResposta> acessoNegado(ForbiddenException e) {
        String mensagem = (e.getMessage() == null || e.getMessage().isBlank())
                ? "Seu papel não permite esta operação"
                : e.getMessage();
        return resposta(ErroResposta.de(403, "Acesso negado", mensagem));
    }
}