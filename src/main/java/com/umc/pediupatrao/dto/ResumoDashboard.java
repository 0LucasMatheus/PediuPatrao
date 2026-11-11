package com.umc.pediupatrao.dto;

import java.time.LocalDate;

// numeros dos cards do dashboard
public record ResumoDashboard(
        LocalDate inicio,
        LocalDate fim,
        int faturamento,
        int ticketMedio,
        int descontos,
        int totalPedidos,
        int finalizados,
        int cancelados,
        int emAndamento,
        double taxaCancelamento,
        int faturamentoAnterior) {
}