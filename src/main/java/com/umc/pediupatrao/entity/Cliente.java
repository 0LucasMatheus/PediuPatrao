package com.umc.pediupatrao.entity;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@Document(collection = "clientes")
public class Cliente {

    @Id
    private String id;
    private String nome;
    private String telefone;

    // Endereço completo
    private String cep;
    private String logradouro;
    private String numero;
    private String complemento;
    private String bairro;
    private String cidade;
    private String estado;

    // Mantido para compatibilidade com dados antigos
    private String endereco;
}
