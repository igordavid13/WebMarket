package br.gov.caixa.webmarket.gravador.application.controller;

import br.gov.caixa.webmarket.contracts.GravarPedidoCommand;
import br.gov.caixa.webmarket.gravador.application.service.GravadorService;
import br.gov.caixa.webmarket.contracts.PedidoView;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

@RestController
@RequestMapping("/interno/pedidos")
public class GravadorController {

    private final GravadorService gravador;

    public GravadorController(GravadorService gravador) {
        this.gravador = gravador;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<Void> gravar(@RequestBody GravarPedidoCommand command) {
        gravador.gravar(command);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<PedidoView> buscar(@PathVariable UUID id) {
        return gravador.buscar(id).map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
