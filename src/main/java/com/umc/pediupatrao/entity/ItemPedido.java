package com.umc.pediupatrao.entity;

import lombok.Getter;
import lombok.Setter;

// item dentro do pedido, guarda nome e preco do produto na hora da venda
// assim se o produto mudar de preco depois o pedido antigo n muda
@Getter
@Setter
public class ItemPedido {
    private String produtoId;
    private String nomeProduto;
    private int precoUnitario;
    private int quantidade;
    private int subtotal; 
}