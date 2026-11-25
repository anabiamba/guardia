package br.com.guardia.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import br.com.guardia.model.VinculoFcm;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VinculoFcmRepository extends JpaRepository<VinculoFcm, String> {

    @Query("""
            select v from VinculoFcm v, Sessao s where v.sessaoHash = s.tokenHash
            and s.usuarioId = :usuario and s.revogadaEm is null and s.expiraEm > :agora and v.token is not null
            """)
    List<VinculoFcm> buscarVinculosAtivos(@Param("usuario") Long usuario, @Param("agora") Instant agora);

    @Query("""
            select v from VinculoFcm v, Sessao s where v.sessaoHash = s.tokenHash
            and v.sessaoHash = :sessao and v.instalacaoHash = :instalacao and s.usuarioId = :usuario
            and s.revogadaEm is null and s.expiraEm > :agora and v.token is not null
            """)
    Optional<VinculoFcm> buscarDestinoVigente(@Param("sessao") String sessao, @Param("instalacao") String instalacao,
                                           @Param("usuario") Long usuario, @Param("agora") Instant agora);

    @Modifying
    @Query("""
            update VinculoFcm v set v.token = null, v.tokenHash = null
            where v.sessaoHash = :sessao and v.instalacaoHash = :instalacao and v.token = :token
            """)
    void limparTokenInvalido(@Param("sessao") String sessao, @Param("instalacao") String instalacao, @Param("token") String token);

    @Modifying
    @Query(value = """
            INSERT INTO vinculos_fcm (instalacao_hash, sessao_hash)
            VALUES (:instalacao, :sessao)
            ON DUPLICATE KEY UPDATE sessao_hash = :sessao, token = NULL, token_hash = NULL
            """, nativeQuery = true)
    void assumirInstalacao(@Param("instalacao") String instalacao, @Param("sessao") String sessao);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<VinculoFcm> findBySessaoHash(String sessaoHash);

    @Modifying
    @Query("update VinculoFcm v set v.token = null, v.tokenHash = null where v.sessaoHash = :sessao")
    void limparDaSessao(@Param("sessao") String sessao);

    @Query("""
            select v.token from VinculoFcm v, Sessao s
            where v.sessaoHash = s.tokenHash and s.usuarioId = :usuario
            and s.revogadaEm is null and s.expiraEm > :agora and v.token is not null
            """)
    List<String> buscarTokensAtivos(@Param("usuario") Long usuarioId, @Param("agora") Instant agora);

    @Query("""
            select count(v) from VinculoFcm v, Sessao s
            where v.sessaoHash = s.tokenHash and s.usuarioId = :usuario
            and v.instalacaoHash = :instalacao and v.token = :token
            and s.revogadaEm is null and s.expiraEm > :agora
            """)
    long contarDestinoTesteAtivo(@Param("usuario") Long usuarioId,
                                @Param("instalacao") String instalacaoHash,
                                @Param("token") String token,
                                @Param("agora") Instant agora);
}
