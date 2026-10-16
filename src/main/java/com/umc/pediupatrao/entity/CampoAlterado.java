package com.umc.pediupatrao.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

// um campo que mudou, com o valor de antes e o de depois
@Getter
@AllArgsConstructor
public class CampoAlterado {
    private String campo;
    private String valorAnterior;
    private String valorNovo;
}
