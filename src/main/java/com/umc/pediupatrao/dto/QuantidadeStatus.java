package com.umc.pediupatrao.dto;

import com.umc.pediupatrao.entity.StatusPedido;

// quantos pedidos tem em cada status
public record QuantidadeStatus(StatusPedido status, int quantidade) {
}
