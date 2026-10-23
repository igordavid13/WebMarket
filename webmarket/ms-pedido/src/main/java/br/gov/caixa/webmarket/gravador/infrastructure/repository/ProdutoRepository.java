package br.gov.caixa.webmarket.gravador.infrastructure.repository;

import br.gov.caixa.webmarket.gravador.domain.entity.ProdutoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface ProdutoRepository extends JpaRepository<ProdutoEntity, Integer> {
    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ProdutoEntity> findById(Integer id);
}
