package org.example.crypto_app.repository;

import org.example.crypto_app.model.BaseUser;
import org.example.crypto_app.model.CryptoKey;
import org.example.crypto_app.model.UserFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface BaseUserRepository extends JpaRepository<BaseUser, Long>{
    BaseUser findByUsername(String username);

    @Transactional
    @Query("SELECT f FROM UserFile f WHERE f.id = ?1 AND f IN (SELECT u.files FROM BaseUser u WHERE u.id = ?2)")
    Optional<UserFile> getUserFile(Long userId, Long fileId);

    @Query("SELECT k FROM CryptoKey k WHERE k.id = ?2 AND k IN (SELECT u.keys FROM BaseUser u WHERE u.id = ?1)")
    Optional<CryptoKey> getKeyById(Long userId, Long keyId);

}
