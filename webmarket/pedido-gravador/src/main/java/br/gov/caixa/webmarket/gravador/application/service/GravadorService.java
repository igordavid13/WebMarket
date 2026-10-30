package br.gov.caixa.webmarket.gravador.application.service;

import br.gov.caixa.webmarket.contracts.GravarPedidoCommand;
import br.gov.caixa.webmarket.contracts.PedidoView;
import br.gov.caixa.webmarket.gravador.domain.entity.EventoPendenteEntity;
import br.gov.caixa.webmarket.gravador.domain.entity.PedidoEntity;
import br.gov.caixa.webmarket.gravador.infrastructure.repository.PedidoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class GravadorService {
    private final PedidoRepository repository;
    private final EntityManager entityManager;

    public GravadorService(PedidoRepository repository, EntityManager entityManager) {
        this.repository = repository;
        this.entityManager = entityManager;
    }

    @Transactional
    public boolean gravar(@NotNull @Valid GravarPedidoCommand command) {
        if (repository.findByIdOptional(command.identificadorPedido()).isPresent()) {
            return false;
        }
        repository.persist(PedidoEntity.de(command.identificadorPedido(), command.dados()));
        entityManager.persist(new EventoPendenteEntity(command.identificadorPedido(), command.dados().dataPedido()));
        return true;
    }

    @Transactional
    public Optional<PedidoView> buscar(UUID id) {
        return repository.findByIdOptional(id).map(PedidoEntity::toView);
    }
}