package br.gov.caixa.webmarket.pedido.infrastructure.error;

import br.gov.caixa.webmarket.pedido.domain.exception.PedidoNaoEncontradoException;
import br.gov.caixa.webmarket.pedido.domain.exception.ProdutoIndisponivelNegocioException;
import br.gov.caixa.webmarket.pedido.domain.exception.ServicoProdutoIndisponivelException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import java.util.List;
import io.quarkus.security.ForbiddenException;

public class ExceptionMappers {

    private static final int UNPROCESSABLE_ENTITY = 422;

    @ServerExceptionMapper
    public RestResponse<ErroResposta> validacao(ConstraintViolationException e) {
        List<String> mensagens = e.getConstraintViolations().stream()
                .map(ExceptionMappers::descrever)
                .sorted()
                .toList();
        return resposta(new ErroResposta(400, "Requisição inválida", mensagens));
    }

    @ServerExceptionMapper
    public RestResponse<ErroResposta> naoEncontrado(PedidoNaoEncontradoException e) {
        return resposta(ErroResposta.de(404, "Pedido não encontrado", e.getMessage()));
    }

    @ServerExceptionMapper
    public RestResponse<ErroResposta> regraNegocio(ProdutoIndisponivelNegocioException e) {
        return resposta(ErroResposta.de(UNPROCESSABLE_ENTITY, "Produto indisponível", e.getMessage()));
    }

    @ServerExceptionMapper
    public RestResponse<ErroResposta> servicoIndisponivel(ServicoProdutoIndisponivelException e) {
        return resposta(ErroResposta.de(503, "Serviço de produtos indisponível", e.getMessage()));
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