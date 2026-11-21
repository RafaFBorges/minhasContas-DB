package com.minhascontasdb.service;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RoleTests {
  private static Set<String> reachableFrom(Role role) {
    RoleHierarchy hierarchy = RoleHierarchyImpl.fromHierarchy(Role.hierarchy());
    Collection<? extends GrantedAuthority> reachable = hierarchy.getReachableGrantedAuthorities(
        List.of(new SimpleGrantedAuthority(role.authority())));

    return reachable.stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet());
  }

  // System > Admin > Suporte > Usuário: cada papel também tem o que os de baixo têm, e nunca o contrário
  @Test
  void eachRoleInheritsTheOnesBelowAndNeverTheOnesAbove() {
    assertEquals(Set.of("ROLE_USER"), reachableFrom(Role.USER));
    assertEquals(Set.of("ROLE_SUPPORT", "ROLE_USER"), reachableFrom(Role.SUPPORT));
    assertEquals(Set.of("ROLE_ADMIN", "ROLE_SUPPORT", "ROLE_USER"), reachableFrom(Role.ADMIN));
    assertEquals(Set.of("ROLE_SYSTEM", "ROLE_ADMIN", "ROLE_SUPPORT", "ROLE_USER"), reachableFrom(Role.SYSTEM));
  }
}
