package com.umc.pediupatrao.dto;

import java.time.LocalDate;

// um ponto do grafico de vendas, um por dia
public record VendaDia(LocalDate dia, int faturamento, int pedidos) {
}
