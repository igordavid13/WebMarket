package br.gov.caixa.webmarket.produto.infrastructure.repository;

import br.gov.caixa.webmarket.produto.domain.entity.OutboxEventoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface OutboxEventoRepository extends JpaRepository<OutboxEventoEntity, UUID> {
    @Query("select e from OutboxEventoEntity e where e.publicadoEm is null order by e.criadoEm")
    List<OutboxEventoEntity> buscarPendentes();
}
