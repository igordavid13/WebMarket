package br.gov.caixa.webmarket.produto.infrastructure.repository;

import br.gov.caixa.webmarket.produto.domain.entity.EventoProcessadoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EventoProcessadoRepository extends JpaRepository<EventoProcessadoEntity, UUID> {
}
