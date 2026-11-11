package com.umc.pediupatrao.service;

import com.umc.pediupatrao.dto.ProdutoVendido;
import com.umc.pediupatrao.dto.QuantidadeStatus;
import com.umc.pediupatrao.dto.ResumoDashboard;
import com.umc.pediupatrao.dto.VendaHora;
import com.umc.pediupatrao.dto.VendaDia;
import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.repository.ProdutoRepository;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
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

    @Autowired
    private ProdutoRepository produtoRepository;

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

    // faturamento e qtd de pedidos finalizados em cada dia
    public List<VendaDia> vendas(LocalDate inicio, LocalDate fim) {
        validarPeriodo(inicio, fim);

        // monta todos os dias antes, assim os sem venda aparecem zerados
        List<LocalDate> dias = new ArrayList<>();
        LocalDate dia = inicio;
        while (!dia.isAfter(fim)) {
            dias.add(dia);
            dia = dia.plusDays(1);
        }

        // cada posicao e um dia da lista de cima
        int[] faturamento = new int[dias.size()];
        int[] pedidos = new int[dias.size()];

        for (Pedido pedido : pedidosDoPeriodo(inicio, fim)) {
            if (pedido.getStatus() == StatusPedido.FINALIZADO) {
                int posicao = dias.indexOf(pedido.getDataHoraEntrada().toLocalDate());
                faturamento[posicao] += pedido.getValorTotal();
                pedidos[posicao]++;
            }
        }

        List<VendaDia> vendas = new ArrayList<>();
        for (int i = 0; i < dias.size(); i++) {
            vendas.add(new VendaDia(dias.get(i), faturamento[i], pedidos[i]));
        }
        return vendas;
    }

    // vendas de cada hora do dia somando todos os dias do periodo, mostra o horario de pico
    public List<VendaHora> vendasPorHora(LocalDate inicio, LocalDate fim) {
        validarPeriodo(inicio, fim);

        // posicao 0 e meia noite, posicao 23 e 23h
        int[] faturamento = new int[24];
        int[] pedidos = new int[24];

        for (Pedido pedido : pedidosDoPeriodo(inicio, fim)) {
            if (pedido.getStatus() == StatusPedido.FINALIZADO) {
                int hora = pedido.getDataHoraEntrada().getHour();
                faturamento[hora] += pedido.getValorTotal();
                pedidos[hora]++;
            }
        }

        List<VendaHora> vendas = new ArrayList<>();
        for (int hora = 0; hora < 24; hora++) {
            vendas.add(new VendaHora(hora, faturamento[hora], pedidos[hora]));
        }
        return vendas;
    }

    // conta todos os pedidos do periodo por status, aqui entra todo mundo n so finalizado
    public List<QuantidadeStatus> status(LocalDate inicio, LocalDate fim) {
        validarPeriodo(inicio, fim);

        StatusPedido[] todosStatus = StatusPedido.values();
        // ordinal e a posicao do status no enum, RECEBIDO 0, EM_PREPARACAO 1...
        int[] quantidade = new int[todosStatus.length];

        for (Pedido pedido : pedidosDoPeriodo(inicio, fim)) {
            quantidade[pedido.getStatus().ordinal()]++;
        }

        // todos os status aparecem mesmo zerados, na ordem do fluxo
        List<QuantidadeStatus> resultado = new ArrayList<>();
        for (StatusPedido status : todosStatus) {
            resultado.add(new QuantidadeStatus(status, quantidade[status.ordinal()]));
        }
        return resultado;
    }

    // soma os itens dos pedidos finalizados por produto e ordena do mais vendido pro menos
    public List<ProdutoVendido> produtosMaisVendidos(LocalDate inicio, LocalDate fim, int limite, String tipo) {
        validarPeriodo(inicio, fim);
        if (limite < 1 || limite > 50) {
            throw new IllegalArgumentException("O limite tem que ser entre 1 e 50.");
        }

        // com tipo escolhido so conta os produtos desse tipo
        List<String> idsDoTipo = new ArrayList<>();
        boolean filtrarTipo = tipo != null && !tipo.isBlank();
        if (filtrarTipo) {
            for (Produto produto : produtoRepository.findByTipoIgnoreCase(tipo)) {
                idsDoTipo.add(produto.getId());
            }
        }

        List<ProdutoVendido> ranking = new ArrayList<>();

        for (Pedido pedido : pedidosDoPeriodo(inicio, fim)) {
            if (pedido.getStatus() != StatusPedido.FINALIZADO) {
                continue;
            }
            for (ItemPedido item : pedido.getItens()) {
                if (filtrarTipo && !idsDoTipo.contains(item.getProdutoId())) {
                    continue;
                }

                // procura se o produto ja ta no ranking
                ProdutoVendido encontrado = null;
                for (ProdutoVendido produto : ranking) {
                    if (produto.getProdutoId().equals(item.getProdutoId())) {
                        encontrado = produto;
                    }
                }

                // primeira vez que aparece, entra zerado
                // nome que ficou guardado no item, assim produto apagado ainda aparece
                if (encontrado == null) {
                    encontrado = new ProdutoVendido();
                    encontrado.setProdutoId(item.getProdutoId());
                    encontrado.setNome(item.getNomeProduto());
                    ranking.add(encontrado);
                }

                encontrado.setQuantidade(encontrado.getQuantidade() + item.getQuantidade());
                encontrado.setFaturamento(encontrado.getFaturamento() + item.getSubtotal());
            }
        }

        // maior quantidade primeiro, se empatar quem faturou mais
        ranking.sort((a, b) -> {
            if (a.getQuantidade() != b.getQuantidade()) {
                return b.getQuantidade() - a.getQuantidade();
            }
            return b.getFaturamento() - a.getFaturamento();
        });

        if (ranking.size() > limite) {
            return ranking.subList(0, limite);
        }
        return ranking;
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
