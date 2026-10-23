package br.gov.caixa.webmarket.produto.infrastructure.repository;

import br.gov.caixa.webmarket.produto.domain.entity.ProdutoEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ProdutoRepository extends JpaRepository<ProdutoEntity, Integer> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from ProdutoEntity p where p.id in :ids order by p.id")
    List<ProdutoEntity> buscarTodosParaAtualizacao(@Param("ids") Collection<Integer> ids);
}
