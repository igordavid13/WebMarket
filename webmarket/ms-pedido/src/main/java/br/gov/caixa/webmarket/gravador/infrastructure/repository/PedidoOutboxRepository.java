package br.gov.caixa.webmarket.gravador.infrastructure.repository;

import br.gov.caixa.webmarket.gravador.domain.entity.PedidoOutboxEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface PedidoOutboxRepository extends JpaRepository<PedidoOutboxEntity, UUID> {
    @Query("select evento from PedidoOutboxEntity evento where evento.publicadoEm is null order by evento.criadoEm")
    List<PedidoOutboxEntity> buscarPendentes();
}
