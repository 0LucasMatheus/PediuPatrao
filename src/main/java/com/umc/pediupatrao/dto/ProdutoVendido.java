package com.umc.pediupatrao.dto;

import lombok.Getter;
import lombok.Setter;

// uma linha do ranking de produtos mais vendidos
@Getter
@Setter
public class ProdutoVendido {
    private String produtoId;
    private String nome;
    private int quantidade;
    private int faturamento;
}