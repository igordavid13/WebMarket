package br.gov.caixa.webmarket.produto.application.controller;

import br.gov.caixa.webmarket.produto.application.service.ProdutoCatalogoService;
import br.gov.caixa.webmarket.produto.application.view.ProdutoView;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/produtos")
public class ProdutoController {
    private final ProdutoCatalogoService catalogo;

    public ProdutoController(ProdutoCatalogoService catalogo) {
        this.catalogo = catalogo;
    }

    @GetMapping
    public List<ProdutoView> listar() {
        return catalogo.listar();
    }

    @GetMapping("/{id}")
    public ProdutoView buscar(@PathVariable Integer id) {
        return catalogo.buscar(id);
    }
}
