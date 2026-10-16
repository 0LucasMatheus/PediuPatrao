package com.umc.pediupatrao.repository;

import com.umc.pediupatrao.entity.Produto;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProdutoRepository extends MongoRepository<Produto, String> {

    // o spring monta a query pelo nome do metodo
    // 10 primeiros, nome contendo o texto sem ligar pra maiuscula, e so os ativos
    List<Produto> findTop10ByNomeContainingIgnoreCaseAndAtivoTrue(String nome);
}