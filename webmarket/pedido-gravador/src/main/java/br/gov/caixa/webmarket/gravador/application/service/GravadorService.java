package br.gov.caixa.webmarket.gravador.application.service;

import br.com.caixa.pix.contracts.PedidoGravadoEvent;
import br.com.caixa.pix.contracts.PedidoView;
import br.com.caixa.pix.contracts.GravarPedidoCommand;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class GravadorService {

    private final PedidoRepository repository;
    private final EventoPublisher eventos;

    public GravadorService(PedidoRepository repository, EventoPublisher eventos) {
        this.repository = repository;
        this.eventos = eventos;
    }

    public boolean gravar(GravarPedidoCommand command) {
        if (repository.existsById(command.identificadorPedido())) {
            return false;
        }
        repository.save(PedidoEntity.de(command.identificadorPedido(), command.dados()));
        eventos.publicar(new PedidoGravadoEvent(command.identificadorPedido(), LocalDateTime.now()));
        return true;
    }

    public Optional<PedidoView> buscar(UUID id) {
        return repository.findById(id).map(this::toView);
    }

    private PedidoView toView(PedidoEntity e) {
        return new PedidoView(e.getId(), e.getNome(), e.getNumeroDocumento(),
                e.getValorTransacao(), e.getChavePixDestino(),
                e.getDataHoraTransacao(), e.getDataHoraGravacao());
    }
}
