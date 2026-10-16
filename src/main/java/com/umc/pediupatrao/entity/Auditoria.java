package com.umc.pediupatrao.entity;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@Document(collection = "auditoria")
public class Auditoria {

    @Id
    private String id;

    private String usuario; // quem fez
    private Perfil perfil;

    private AcaoAuditoria acao; // oque fez

    private String entidade; // onde fez, nome da collection
    private String entidadeId; // qual id

    private List<CampoAlterado> alteracoes; // diff so dos campos que mudaram

    private String justificativa; // so quando a regra pede

    private LocalDateTime dataHora; // quando fez
}
