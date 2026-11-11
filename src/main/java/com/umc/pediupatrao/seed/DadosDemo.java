package com.umc.pediupatrao.seed;

import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.repository.ClienteRepository;
import com.umc.pediupatrao.repository.PedidoRepository;
import com.umc.pediupatrao.repository.ProdutoRepository;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// seed de pedidos de mentira pra demonstrar o dashboard
// so roda com SEED_DEMO=true no .env e so uma vez, igual o DataInitializer dos usuarios
@Configuration
public class DadosDemo {

    @Value("${app.seed-demo}")
    private boolean ligado;

    // marca dos pedidos de demo, assim da pra saber quais sao
    private static final String RESPONSAVEL = "demo";

    // quantos dias pra tras contando hoje
    private static final int DIAS = 40;

    // horarios que mais sai pedido, repetido = mais chance de ser sorteado, pico as 20h
    private static final int[] HORAS = {11, 12, 12, 13, 18, 18, 19, 19, 19, 20, 20, 20, 20, 21, 21, 21, 22, 22, 23};

    private static final String[] JUSTIFICATIVAS = {
        "Cliente desistiu do pedido",
        "Endereço fora da área de entrega",
        "Cliente não atendeu o entregador",
        "Produto acabou no estoque"
    };

    private static final int[] DESCONTOS = {5, 10, 15, 20};

    // numero fixo no random, assim toda vez que rodar gera os mesmos pedidos
    private final Random sorteio = new Random(42);

    @Bean
    CommandLineRunner popularPedidosDemo(PedidoRepository pedidoRepository,
            ClienteRepository clienteRepository, ProdutoRepository produtoRepository) {
        return args -> {
            if (!ligado) {
                return;
            }
            if (pedidoRepository.existsByResponsavelEntrada(RESPONSAVEL)) {
                System.out.println("Pedidos de demo ja existem, n criou de novo");
                return;
            }

            List<Cliente> clientes = clienteRepository.findAll();
            List<Produto> produtos = new ArrayList<>();
            for (Produto produto : produtoRepository.findAll()) {
                if (produto.getAtivo() != null && produto.getAtivo()) {
                    produtos.add(produto);
                }
            }
            if (clientes.isEmpty() || produtos.isEmpty()) {
                System.out.println("Precisa de pelo menos 1 cliente e 1 produto ativo pro seed de demo");
                return;
            }

            // sempre os ultimos 40 dias ate hoje, n depende de ter pedido no banco
            LocalDate hoje = LocalDate.now();
            LocalDate dia = hoje.minusDays(DIAS - 1);

            int criados = 0;
            while (!dia.isAfter(hoje)) {
                int quantidade = pedidosNoDia(dia);
                for (int i = 0; i < quantidade; i++) {
                    Pedido pedido = montarPedido(dia, clientes, produtos);
                    // horario sorteado de hoje que ainda n chegou fica de fora
                    if (pedido != null) {
                        pedidoRepository.save(pedido);
                        criados++;
                    }
                }
                dia = dia.plusDays(1);
            }

            System.out.println("Seed de demo criou " + criados + " pedidos");
        };
    }

    // sexta e sabado tem mais movimento
    private int pedidosNoDia(LocalDate dia) {
        int quantidade = 4 + sorteio.nextInt(5);
        if (dia.getDayOfWeek() == DayOfWeek.FRIDAY || dia.getDayOfWeek() == DayOfWeek.SATURDAY) {
            quantidade += 4;
        }
        return quantidade;
    }

    private Pedido montarPedido(LocalDate dia, List<Cliente> clientes, List<Produto> produtos) {
        int hora = HORAS[sorteio.nextInt(HORAS.length)];
        LocalDateTime entrada = dia.atTime(hora, sorteio.nextInt(60));
        LocalDateTime agora = LocalDateTime.now();
        if (entrada.isAfter(agora)) {
            return null;
        }

        Cliente cliente = clientes.get(sorteio.nextInt(clientes.size()));

        // de 1 a 3 produtos diferentes, cada um com 1 a 3 unidades
        // preco vem do produto e fica congelado no item, igual no PedidoService
        List<ItemPedido> itens = new ArrayList<>();
        int subtotal = 0;
        int quantosItens = 1 + sorteio.nextInt(3);
        for (int i = 0; i < quantosItens; i++) {
            Produto produto = produtos.get(sorteio.nextInt(produtos.size()));

            // se sorteou um produto que ja ta no pedido pula
            boolean jaTem = false;
            for (ItemPedido item : itens) {
                if (item.getProdutoId().equals(produto.getId())) {
                    jaTem = true;
                }
            }
            if (jaTem) {
                continue;
            }

            ItemPedido item = new ItemPedido();
            item.setProdutoId(produto.getId());
            item.setNomeProduto(produto.getNome());
            item.setPrecoUnitario(produto.getPreco());
            item.setQuantidade(1 + sorteio.nextInt(3));
            item.setSubtotal(produto.getPreco() * item.getQuantidade());
            itens.add(item);
            subtotal += item.getSubtotal();
        }

        Pedido pedido = new Pedido();
        pedido.setClienteId(cliente.getId());
        pedido.setClienteNome(cliente.getNome());
        pedido.setItens(itens);
        pedido.setSubtotal(subtotal);
        pedido.setDescontoPercentual(0);
        pedido.setValorDesconto(0);
        pedido.setValorTotal(subtotal);
        pedido.setDataHoraEntrada(entrada);
        pedido.setResponsavelEntrada(RESPONSAVEL);

        // uns 15% ganham desconto, mesma conta do aplicarDesconto
        if (sorteio.nextInt(100) < 15) {
            int percentual = DESCONTOS[sorteio.nextInt(DESCONTOS.length)];
            int valorDesconto = (int) Math.round(subtotal * percentual / 100.0);
            pedido.setDescontoPercentual(percentual);
            pedido.setValorDesconto(valorDesconto);
            pedido.setValorTotal(subtotal - valorDesconto);
        }

        // pedido de menos de 1 hora atras ainda ta andando na cozinha
        if (entrada.isAfter(agora.minusHours(1))) {
            StatusPedido[] andamento = {StatusPedido.RECEBIDO, StatusPedido.EM_PREPARACAO, StatusPedido.PRONTO};
            pedido.setStatus(andamento[sorteio.nextInt(andamento.length)]);
            return pedido;
        }

        // uns 8% cancelados, o resto finalizado
        if (sorteio.nextInt(100) < 8) {
            pedido.setStatus(StatusPedido.CANCELADO);
            pedido.setDataHoraCancelamento(entrada.plusMinutes(5 + sorteio.nextInt(15)));
            pedido.setResponsavelCancelamento(RESPONSAVEL);
            pedido.setJustificativaCancelamento(JUSTIFICATIVAS[sorteio.nextInt(JUSTIFICATIVAS.length)]);
        } else {
            // saida uns 30 a 50 min depois da entrada
            pedido.setStatus(StatusPedido.FINALIZADO);
            pedido.setDataHoraSaida(entrada.plusMinutes(30 + sorteio.nextInt(21)));
            pedido.setResponsavelSaida(RESPONSAVEL);
        }
        return pedido;
    }
}
