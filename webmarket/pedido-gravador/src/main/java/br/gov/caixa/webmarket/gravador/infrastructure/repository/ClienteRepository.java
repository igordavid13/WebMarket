package br.gov.caixa.webmarket.gravador.infrastructure.repository;

import br.gov.caixa.webmarket.gravador.domain.entity.ClienteEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClienteRepository extends JpaRepository<ClienteEntity, Integer> {
    Optional<ClienteEntity> findByCpf(String cpf);
}
