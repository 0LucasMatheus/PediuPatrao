package com.umc.pediupatrao.listener;

import com.umc.pediupatrao.entity.AcaoAuditoria;
import com.umc.pediupatrao.service.AuditoriaService;
import java.util.List;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.BeforeDeleteEvent;
import org.springframework.data.mongodb.core.mapping.event.BeforeSaveEvent;
import org.springframework.stereotype.Component;

// listener que faz tipo trigger do banco com o spring chamando sozinho antes de todo save e delete
@Component
public class AuditoriaListener extends AbstractMongoEventListener<Object> {

    // collections que sao auditadas
    private static final List<String> COLLECTIONS_AUDITADAS = List.of("clientes", "pedidos");

    private final MongoTemplate mongoTemplate;
    private final AuditoriaService auditoriaService;

    public AuditoriaListener(MongoTemplate mongoTemplate, AuditoriaService auditoriaService) {
        this.mongoTemplate = mongoTemplate;
        this.auditoriaService = auditoriaService;
    }

    // inclusao e alteracao
    @Override
    public void onBeforeSave(BeforeSaveEvent<Object> event) {
        String collection = event.getCollectionName();
        if (!COLLECTIONS_AUDITADAS.contains(collection)) {
            return;
        }

        Document novo = event.getDocument();

        // registro novo ainda n tem id, entao gera aqui pra ja ter o id no log
        if (novo.get("_id") == null) {
            novo.put("_id", new ObjectId());
            String id = novo.get("_id").toString();
            auditoriaService.registrar(AcaoAuditoria.INCLUSAO, collection, id, new Document(), novo);
            return;
        }

        // ja existe, pega como ta no banco agora pra comparar
        String id = novo.get("_id").toString();
        Document antigo = buscarNoBanco(collection, novo.get("_id"));
        auditoriaService.registrar(AcaoAuditoria.ALTERACAO, collection, id, antigo, novo);
    }

    // exclusao
    @Override
    public void onBeforeDelete(BeforeDeleteEvent<Object> event) {
        String collection = event.getCollectionName();
        if (!COLLECTIONS_AUDITADAS.contains(collection)) {
            return;
        }

        // aqui o documento e o filtro do delete
        Document filtro = event.getDocument();
        Document antigo = mongoTemplate.getCollection(collection).find(filtro).first();
        if (antigo == null) {
            return;
        }

        String id = antigo.get("_id").toString();
        auditoriaService.registrar(AcaoAuditoria.EXCLUSAO, collection, id, antigo, new Document());
    }

    private Document buscarNoBanco(String collection, Object id) {
        Document antigo = mongoTemplate.getCollection(collection).find(new Document("_id", id)).first();
        if (antigo == null) {
            return new Document();
        }
        return antigo;
    }
}