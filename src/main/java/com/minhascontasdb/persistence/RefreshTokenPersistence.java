package com.minhascontasdb.persistence;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.minhascontasdb.service.RefreshToken;

@Repository
public interface RefreshTokenPersistence extends JpaRepository<RefreshToken, Long> {
  Optional<RefreshToken> findByTokenHash(String tokenHash);

  // Atômico: de dois pedidos simultâneos com o mesmo token, só um recebe 1. É isso que impede o token de ser usado duas vezes
  @Modifying
  @Transactional
  @Query("update RefreshToken t set t.revokedAt = :now where t.tokenHash = :hash and t.revokedAt is null and t.expiresAt > :now")
  int revokeIfActive(@Param("hash") String hash, @Param("now") Instant now);

  // Logout: apaga a cadeia inteira do token (ele pode já ter sido trocado: o sucessor também sai). A linha some em vez de
  // ficar "revogada" para repetir um token encerrado não ser tratado como reuso
  @Modifying
  @Transactional
  @Query("delete from RefreshToken t where t.chainId = (select r.chainId from RefreshToken r where r.tokenHash = :hash)")
  int deleteChainOf(@Param("hash") String hash);

  // Teto de sessões: a cadeia inteira sai, não só a linha ativa. As linhas já trocadas dela, se ficassem, fariam um cliente expulso
  // que ainda usa um token antigo ser tratado como reuso (e perder todas as sessões)
  @Modifying
  @Transactional
  @Query("delete from RefreshToken t where t.chainId in :chainIds")
  int deleteChains(@Param("chainIds") Collection<UUID> chainIds);

  // Encerra todas as sessões do usuário. Apaga (em vez de marcar como revogado) para os tokens não parecerem "recém-trocados"
  @Modifying
  @Transactional
  @Query("delete from RefreshToken t where t.userId = :userId")
  int deleteAllOfUser(@Param("userId") Long userId);

  // Sessões ativas (não trocadas e não vencidas) do usuário, da mais nova para a mais antiga
  List<RefreshToken> findByUserIdAndRevokedAtIsNullAndExpiresAtAfterOrderByIdDesc(Long userId, Instant now);

  // Limpeza: vencidos, e os já trocados há tempo suficiente (eles só servem para detectar a cópia de um token)
  @Modifying
  @Transactional
  @Query("delete from RefreshToken t where t.expiresAt < :now or t.revokedAt < :rotatedBefore")
  int deleteStale(@Param("now") Instant now, @Param("rotatedBefore") Instant rotatedBefore);
}
