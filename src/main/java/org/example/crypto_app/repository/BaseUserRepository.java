package org.example.crypto_app.repository;

import org.example.crypto_app.model.BaseUser;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BaseUserRepository extends JpaRepository<BaseUser, Long>{
    BaseUser findByUsername(String username);
}
