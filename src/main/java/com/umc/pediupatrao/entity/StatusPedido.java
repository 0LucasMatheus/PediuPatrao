package com.umc.pediupatrao.entity;

import java.util.List;

// etapas do pedido, na ordem
public enum StatusPedido {
    RECEBIDO,
    EM_PREPARACAO,
    PRONTO,
    SAIU_PARA_ENTREGA,
    RETIRADO,
    FINALIZADO,
    CANCELADO;

    // pra onde cada status pode ir no fluxo normal
    // cancelado n ta aqui pq e uma acao separada so do gerente
    public List<StatusPedido> proximos() {
        return switch (this) {
            case RECEBIDO -> List.of(EM_PREPARACAO);
            case EM_PREPARACAO -> List.of(PRONTO);
            case PRONTO -> List.of(SAIU_PARA_ENTREGA, RETIRADO);
            case SAIU_PARA_ENTREGA, RETIRADO -> List.of(FINALIZADO);
            case FINALIZADO, CANCELADO -> List.of();
        };
    }

    // pra corrigir clique errado, so da pra voltar antes da saida
    // depois que saiu n volta, se n daria pra cancelar pedido que ja saiu
    public StatusPedido anterior() {
        return switch (this) {
            case EM_PREPARACAO -> RECEBIDO;
            case PRONTO -> EM_PREPARACAO;
            default -> null;
        };
    }

    public boolean podeIrPara(StatusPedido novo) {
        return proximos().contains(novo);
    }

    // ainda n saiu nem foi cancelado, so assim pode cancelar ou dar desconto
    public boolean antesDaSaida() {
        return this == RECEBIDO || this == EM_PREPARACAO || this == PRONTO;
    }

    // saiu pra entrega ou foi retirado, ai tem que registrar a saida
    public boolean ehSaida() {
        return this == SAIU_PARA_ENTREGA || this == RETIRADO;
    }
}