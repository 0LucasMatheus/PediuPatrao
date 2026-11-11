package com.umc.pediupatrao.dto;

// vendas numa hora do dia, hora vai de 0 a 23
public record VendaHora(int hora, int faturamento, int pedidos) {
}
