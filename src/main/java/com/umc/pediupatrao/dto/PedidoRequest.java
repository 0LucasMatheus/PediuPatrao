package com.umc.pediupatrao.dto;

import java.util.List;

// o que chega pra criar um pedido
// n tem campo de total nem de preco, entao n tem como mandar um valor inventado
public record PedidoRequest(String clienteId, List<ItemPedidoRequest> itens) {
}