package com.umc.pediupatrao.service;

import com.umc.pediupatrao.dto.ItemPedidoRequest;
import com.umc.pediupatrao.dto.PedidoRequest;
import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.repository.ClienteRepository;
import com.umc.pediupatrao.repository.PedidoRepository;
import com.umc.pediupatrao.repository.ProdutoRepository;
import com.umc.pediupatrao.security.UsuarioLogado;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

@Service
public class PedidoService {

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private ProdutoRepository produtoRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    private static final int TAMANHO_PAGINA = 20;

    public Pedido criarPedido(PedidoRequest request) {
        if (request.clienteId() == null || request.clienteId().isBlank()) {
            throw new IllegalArgumentException("Selecione o cliente.");
        }
        Cliente cliente = clienteRepository.findById(request.clienteId())
                .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado."));

        if (request.itens() == null || request.itens().isEmpty()) {
            throw new IllegalArgumentException("O pedido precisa ter pelo menos um item.");
        }

        // monta os itens com o preco do banco
        List<ItemPedido> itens = new ArrayList<>();
        int subtotal = 0;

        for (ItemPedidoRequest itemRequest : request.itens()) {
            if (itemRequest.quantidade() <= 0) {
                throw new IllegalArgumentException("A quantidade de cada item precisa ser maior que zero.");
            }

            Produto produto = produtoRepository.findById(itemRequest.produtoId())
                    .orElseThrow(() -> new IllegalArgumentException("Produto não encontrado."));

            if (produto.getAtivo() == null || !produto.getAtivo()) {
                throw new IllegalArgumentException("O produto " + produto.getNome() + " está inativo.");
            }

            ItemPedido item = new ItemPedido();
            item.setProdutoId(produto.getId());
            item.setNomeProduto(produto.getNome());
            item.setPrecoUnitario(produto.getPreco());
            item.setQuantidade(itemRequest.quantidade());
            item.setSubtotal(produto.getPreco() * itemRequest.quantidade());

            itens.add(item);
            subtotal = subtotal + item.getSubtotal();
        }

        Pedido pedido = new Pedido();
        pedido.setClienteId(cliente.getId());
        pedido.setClienteNome(cliente.getNome());
        pedido.setItens(itens);

        // desconto comeca zerado, so o gerente aplica depois
        pedido.setSubtotal(subtotal);
        pedido.setDescontoPercentual(0);
        pedido.setValorDesconto(0);
        pedido.setValorTotal(subtotal);

        // entrada, quem registrou e quando
        pedido.setStatus(StatusPedido.RECEBIDO);
        pedido.setDataHoraEntrada(LocalDateTime.now());
        pedido.setResponsavelEntrada(UsuarioLogado.username());

        return pedidoRepository.save(pedido);
    }

    public List<Pedido> listarPedidos() {
        return pedidoRepository.findAll();
    }

    // lista da tela, com busca, filtro, ordenacao e paginacao
    public Page<Pedido> buscar(String cliente, String status, LocalDate dia, String ordem, int pagina) {
        Query query = new Query();

        // so os pedidos que entraram nesse dia, de 00:00 ate 00:00 do dia seguinte
        LocalDateTime inicioDoDia = dia.atStartOfDay();
        LocalDateTime inicioDoProximoDia = dia.plusDays(1).atStartOfDay();
        query.addCriteria(Criteria.where("dataHoraEntrada").gte(inicioDoDia).lt(inicioDoProximoDia));

        if (cliente != null && !cliente.isBlank()) {
            // tipo um ILIKE '%texto%' do sql
            query.addCriteria(Criteria.where("clienteNome").regex(Pattern.quote(cliente), "i"));
        }
        if (status != null && !status.isBlank()) {
            query.addCriteria(Criteria.where("status").is(status));
        }

        long total = mongoTemplate.count(query, Pedido.class);

        Pageable paginacao = PageRequest.of(pagina, TAMANHO_PAGINA, ordenacao(ordem));
        query.with(paginacao);
        List<Pedido> pedidos = mongoTemplate.find(query, Pedido.class);

        return new PageImpl<>(pedidos, paginacao, total);
    }

    // o que vem do select de ordenar, o padrao e mais recentes primeiro
    private Sort ordenacao(String ordem) {
        if ("antigos".equals(ordem)) {
            return Sort.by("dataHoraEntrada").ascending();
        }
        if ("maior_valor".equals(ordem)) {
            return Sort.by("valorTotal").descending();
        }
        if ("menor_valor".equals(ordem)) {
            return Sort.by("valorTotal").ascending();
        }
        return Sort.by("dataHoraEntrada").descending();
    }

    public Pedido atualizarStatus(String id, StatusPedido status) {
        Pedido pedido = pedidoRepository.findById(id).orElseThrow();
        pedido.setStatus(status);
        return pedidoRepository.save(pedido);
    }

}
