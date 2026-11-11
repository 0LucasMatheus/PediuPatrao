package com.umc.pediupatrao.dto;

import java.time.LocalDate;

// um ponto do grafico de vendas, periodo e o primeiro dia do dia/semana/mes
public record VendaPeriodo(LocalDate periodo, int faturamento, int pedidos) {
}
