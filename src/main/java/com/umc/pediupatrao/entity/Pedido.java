package com.umc.pediupatrao.entity;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@Document(collection = "pedidos")
public class Pedido {
    @Id
    private String id;

    private String clienteId;
    private String clienteNome; // guardado aqui pra lista e busca

    private List<ItemPedido> itens;

    private int subtotal; // soma dos itens
    private int descontoPercentual; // 0 a 20
    private int valorDesconto;
    private int valorTotal; // subtotal - desconto

    private StatusPedido status;

    // entrada
    private LocalDateTime dataHoraEntrada;
    private String responsavelEntrada;
}