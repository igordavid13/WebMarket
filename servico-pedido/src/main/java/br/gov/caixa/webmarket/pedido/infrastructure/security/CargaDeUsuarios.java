package br.gov.caixa.webmarket.pedido.infrastructure.security;

import br.gov.caixa.webmarket.pedido.domain.entity.Usuario;
import br.gov.caixa.webmarket.pedido.infrastructure.repository.UsuarioRepository;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class CargaDeUsuarios {

    @Inject
    UsuarioRepository usuarioRepository;

    @Transactional
    void carregar(@Observes StartupEvent evento) {
        usuarioRepository.deleteAll();
        usuarioRepository.persist(new Usuario("maria", BcryptUtil.bcryptHash("maria123"), "cliente"));
        usuarioRepository.persist(new Usuario("joao", BcryptUtil.bcryptHash("joao123"), "cliente"));
        usuarioRepository.persist(new Usuario("admin", BcryptUtil.bcryptHash("admin123"), "admin"));
    }
}