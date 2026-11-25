package br.com.guardia.repository;

import java.util.Optional;

import br.com.guardia.model.Sessao;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessaoRepository extends JpaRepository<Sessao, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select s from Sessao s where s.usuarioId = :usuario order by s.tokenHash")
    java.util.List<Sessao> bloquearDaConta(@org.springframework.data.repository.query.Param("usuario") Long usuario);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select s from Sessao s where s.tokenHash = :hash")
    Optional<Sessao> bloquearPorHash(@org.springframework.data.repository.query.Param("hash") String hash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Sessao> findByRevogacaoHash(String revogacaoHash);
}
