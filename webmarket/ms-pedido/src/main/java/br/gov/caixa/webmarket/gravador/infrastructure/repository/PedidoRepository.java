package br.gov.caixa.webmarket.gravador.infrastructure.repository;

import br.gov.caixa.webmarket.gravador.domain.entity.PedidoEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface PedidoRepository extends JpaRepository<PedidoEntity, UUID> {
}