package com.umc.pediupatrao.service;

import com.umc.pediupatrao.dto.ResumoDashboard;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.StatusPedido;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

// contas do dashboard, so pedido finalizado conta como venda
@Service
public class DashboardService {

    @Autowired
    private MongoTemplate mongoTemplate;

    private static final int MAXIMO_DIAS = 366;

    public ResumoDashboard resumo(LocalDate inicio, LocalDate fim) {
        validarPeriodo(inicio, fim);

        List<Pedido> pedidos = pedidosDoPeriodo(inicio, fim);

        int faturamento = 0;
        int descontos = 0;
        int finalizados = 0;
        int cancelados = 0;
        int emAndamento = 0;

        for (Pedido pedido : pedidos) {
            if (pedido.getStatus() == StatusPedido.FINALIZADO) {
                finalizados++;
                faturamento += pedido.getValorTotal();
                descontos += pedido.getValorDesconto();
            } else if (pedido.getStatus() == StatusPedido.CANCELADO) {
                cancelados++;
            } else {
                emAndamento++;
            }
        }

        int ticketMedio = 0;
        if (finalizados > 0) {
            ticketMedio = faturamento / finalizados;
        }

        double taxaCancelamento = 0;
        if (pedidos.size() > 0) {
            taxaCancelamento = cancelados * 100.0 / pedidos.size();
            // deixa so 1 casa, 16.666 vira 16.7
            taxaCancelamento = Math.round(taxaCancelamento * 10) / 10.0;
        }

        // periodo anterior do mesmo tamanho, 30 dias compara com os 30 de antes
        long dias = ChronoUnit.DAYS.between(inicio, fim) + 1;
        LocalDate fimAnterior = inicio.minusDays(1);
        LocalDate inicioAnterior = fimAnterior.minusDays(dias - 1);
        int faturamentoAnterior = somarFaturamento(pedidosDoPeriodo(inicioAnterior, fimAnterior));

        return new ResumoDashboard(inicio, fim, faturamento, ticketMedio, descontos,
                pedidos.size(), finalizados, cancelados, emAndamento, taxaCancelamento, faturamentoAnterior);
    }

    // da 400 pelo ApiExceptionHandler
    private void validarPeriodo(LocalDate inicio, LocalDate fim) {
        if (inicio.isAfter(fim)) {
            throw new IllegalArgumentException("A data inicial não pode ser depois da data final.");
        }
        if (ChronoUnit.DAYS.between(inicio, fim) + 1 > MAXIMO_DIAS) {
            throw new IllegalArgumentException("O período pode ter no máximo 1 ano.");
        }
    }

    // pedidos que entraram entre o inicio e o fim, o dia do fim conta inteiro
    private List<Pedido> pedidosDoPeriodo(LocalDate inicio, LocalDate fim) {
        Query query = new Query();
        query.addCriteria(Criteria.where("dataHoraEntrada")
                .gte(inicio.atStartOfDay())
                .lt(fim.plusDays(1).atStartOfDay()));
        return mongoTemplate.find(query, Pedido.class);
    }

    private int somarFaturamento(List<Pedido> pedidos) {
        int total = 0;
        for (Pedido pedido : pedidos) {
            if (pedido.getStatus() == StatusPedido.FINALIZADO) {
                total += pedido.getValorTotal();
            }
        }
        return total;
    }
}
