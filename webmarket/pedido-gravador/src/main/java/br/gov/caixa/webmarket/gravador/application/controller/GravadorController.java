package br.gov.caixa.webmarket.gravador.application.controller;

import br.com.caixa.pix.contracts.ComprovanteView;
import br.com.caixa.pix.contracts.GravarComprovanteCommand;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/interno/pedidos")
public class GravadorController {

    private final GravadorService gravador;

    public GravadorController(GravadorService gravador) {
        this.gravador = gravador;
    }

    @PostMapping
    public ResponseEntity<Void> gravar(@RequestBody GravarPedidoCommand command) {
        gravador.gravar(command);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ComprovanteView> buscar(@PathVariable UUID id) {
        return gravador.buscar(id).map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
