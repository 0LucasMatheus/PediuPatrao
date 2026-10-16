package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.AcaoAuditoria;
import com.umc.pediupatrao.entity.Auditoria;
import com.umc.pediupatrao.entity.CampoAlterado;
import com.umc.pediupatrao.repository.AuditoriaRepository;
import com.umc.pediupatrao.security.UsuarioLogado;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.bson.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class AuditoriaService {

    // campos que nunca vao pro log
    private static final List<String> CAMPOS_IGNORADOS = List.of("_id", "_class", "password", "senha");

    private final AuditoriaRepository auditoriaRepository;

    public AuditoriaService(AuditoriaRepository auditoriaRepository) {
        this.auditoriaRepository = auditoriaRepository;
    }

    public Page<Auditoria> listar(Pageable pageable) {
        return auditoriaRepository.findAll(pageable);
    }

    // chamado pelo listener, antigo e novo sao o documento do mongo inteiro
    public void registrar(AcaoAuditoria acao, String entidade, String entidadeId, Document antigo, Document novo) {
        List<CampoAlterado> alteracoes = compararDocumentos(antigo, novo);

        // alterou mas nenhum campo mudou de verdade, n precisa logar
        if (acao == AcaoAuditoria.ALTERACAO && alteracoes.isEmpty()) {
            return;
        }

        Auditoria auditoria = new Auditoria();
        auditoria.setUsuario(UsuarioLogado.username());
        auditoria.setPerfil(UsuarioLogado.perfil());
        auditoria.setAcao(acao);
        auditoria.setEntidade(entidade);
        auditoria.setEntidadeId(entidadeId);
        auditoria.setAlteracoes(alteracoes);
        auditoria.setDataHora(LocalDateTime.now());

        // se mudou algum campo de justificativa copia pro log
        for (CampoAlterado campo : alteracoes) {
            if (campo.getCampo().toLowerCase().contains("justificativa")) {
                auditoria.setJustificativa(campo.getValorNovo());
            }
        }

        auditoriaRepository.save(auditoria);
    }

    // olha todos os campos dos dois e guarda so os que tao diferentes
    private List<CampoAlterado> compararDocumentos(Document antigo, Document novo) {
        Set<String> campos = new LinkedHashSet<>();
        campos.addAll(antigo.keySet());
        campos.addAll(novo.keySet());

        List<CampoAlterado> alteracoes = new ArrayList<>();
        for (String campo : campos) {
            if (CAMPOS_IGNORADOS.contains(campo)) {
                continue;
            }

            String valorAnterior = paraTexto(antigo.get(campo));
            String valorNovo = paraTexto(novo.get(campo));

            if (!Objects.equals(valorAnterior, valorNovo)) {
                // telefone n aparece inteiro no log
                if (campo.equals("telefone")) {
                    valorAnterior = mascararTelefone(valorAnterior);
                    valorNovo = mascararTelefone(valorNovo);
                }
                alteracoes.add(new CampoAlterado(campo, valorAnterior, valorNovo));
            }
        }
        return alteracoes;
    }

    private String paraTexto(Object valor) {
        if (valor == null) {
            return null;
        }
        return valor.toString();
    }

    // (11) 99999-1234 vira *****-1234
    private String mascararTelefone(String telefone) {
        if (telefone == null) {
            return null;
        }
        String numeros = telefone.replaceAll("\\D", "");
        if (numeros.length() < 4) {
            return "****";
        }
        return "*****-" + numeros.substring(numeros.length() - 4);
    }
}
