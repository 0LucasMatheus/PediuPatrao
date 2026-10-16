package com.umc.pediupatrao.repository;

import com.umc.pediupatrao.entity.Auditoria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.Repository;

// usa o Repository vazio em vez do MongoRepository pq so precisa do save
public interface AuditoriaRepository extends Repository<Auditoria, String> {

    Auditoria save(Auditoria auditoria);

    Page<Auditoria> findAll(Pageable pageable);
}