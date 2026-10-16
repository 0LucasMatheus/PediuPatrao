package com.umc.pediupatrao.dto;

// o que chega de cada item, so o produto e a quantidade
public record ItemPedidoRequest(String produtoId, int quantidade) {
}