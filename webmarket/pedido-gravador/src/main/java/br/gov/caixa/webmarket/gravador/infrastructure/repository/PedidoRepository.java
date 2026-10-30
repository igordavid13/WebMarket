package br.gov.caixa.webmarket.gravador.infrastructure.repository;

import br.gov.caixa.webmarket.gravador.domain.entity.PedidoEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.UUID;

@ApplicationScoped
public class PedidoRepository implements PanacheRepositoryBase<PedidoEntity, UUID> {}