package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.dto.PedidoRequest;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.service.PedidoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    @Autowired
    private PedidoService pedidoService;

    // recebe o PedidoRequest e n o Pedido, entao o json n consegue mandar total nem preco
    @PreAuthorize("hasAnyRole('GERENTE', 'ATENDENTE')")
    @PostMapping
    public ResponseEntity<Pedido> criarPedido(@RequestBody PedidoRequest request) {
        Pedido criado = pedidoService.criarPedido(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(criado);
    }

    @GetMapping
    public List<Pedido> listarPedidos() {
        return pedidoService.listarPedidos();
    }

    @PreAuthorize("hasAnyRole('GERENTE', 'ATENDENTE')")
    @PutMapping("/{id}/status")
    public Pedido atualizarStatus(@PathVariable String id, @RequestParam StatusPedido status) {
        return pedidoService.atualizarStatus(id, status);
    }

    // so gerente, nem o admin pode
    @PreAuthorize("hasRole('GERENTE')")
    @PutMapping("/{id}/cancelar")
    public Pedido cancelar(@PathVariable String id, @RequestParam String justificativa) {
        return pedidoService.cancelar(id, justificativa);
    }

    // so gerente, nem o admin pode
    @PreAuthorize("hasRole('GERENTE')")
    @PutMapping("/{id}/desconto")
    public Pedido aplicarDesconto(@PathVariable String id, @RequestParam int percentual) {
        return pedidoService.aplicarDesconto(id, percentual);
    }

    @PreAuthorize("hasAnyRole('GERENTE', 'ATENDENTE')")
    @PutMapping("/{id}/voltar")
    public Pedido voltarStatus(@PathVariable String id) {
        return pedidoService.voltarStatus(id);
    }
}
