package com.funwallet.backend.repository;

import com.funwallet.backend.model.AppConfig;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AppConfigRepository extends CrudRepository<AppConfig, Long> {
}
