package com.umc.pediupatrao.entity;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@Document(collection = "produtos")
public class Produto {

    @Id
    private String id;
    private String nome;
    private String tipo; // Pizza, Esfiha, Bebida, Lanche
    private Integer preco; // em centavos, 4990 = R$ 49,90
    private Boolean ativo;
}
