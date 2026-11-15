package br.gov.caixa.webmarket.pedido.infrastructure.security;

import br.gov.caixa.webmarket.pedido.infrastructure.repository.UsuarioRepository;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsStringIgnoringCase;
import static org.hamcrest.CoreMatchers.equalTo;

@QuarkusTest
public class AutenticacaoTest {

    @Inject
    UsuarioRepository usuarioRepository;

    @Test
    @DisplayName("Critério 5: Deve retornar 401 com desafio Basic quando não enviar credencial")
    public void semCredencialDevolve401ComDesafioBasic() {
        given()
                .when().get("/pedidos")
                .then()
                .statusCode(401)
                .header("WWW-Authenticate", containsStringIgnoringCase("basic"));
    }

    @Test
    @DisplayName("Critério 5: Deve retornar 401 quando a senha estiver incorreta")
    public void senhaErradaDevolve401() {
        given()
                .auth().preemptive().basic("maria", "senha_errada")
                .when().get("/pedidos")
                .then()
                .statusCode(401);
    }

    @Test
    @DisplayName("Critério 5: Deve retornar 200 com credencial válida cadastrada no banco")
    public void credencialCorretaDevolve200() {
        given()
                .auth().preemptive().basic("maria", "maria123")
                .when().get("/pedidos")
                .then()
                .statusCode(200);
    }

    @Test
    @DisplayName("Critério 5: Senha deve estar persistida no banco como hash BCrypt ($2a$)")
    public void senhaFicaGuardadaComoHashBcrypt() {
        var usuario = usuarioRepository.findByUsername("maria").orElseThrow();
        Assertions.assertNotEquals("maria123", usuario.getSenha());
        Assertions.assertTrue(usuario.getSenha().startsWith("$2a$"), "Hash deve usar formato BCrypt");
    }

    @Test
    @DisplayName("Critério 5 e 6: Health check continua aberto sem exigir autenticação")
    public void healthContinuaAbertoSemLogin() {
        given()
                .when().get("/q/health/live")
                .then()
                .statusCode(200)
                .body("status", equalTo("UP"));
    }
}