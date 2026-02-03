package cmc.aiq.aiq.repository;

import cmc.aiq.aiq.domain.AuthProvider;
import cmc.aiq.aiq.domain.Users;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsersRepository extends JpaRepository<Users, Long> {
    Optional<Users> findByEmail(String email);
    boolean existsByEmail(String email);
    Optional<Users> findByProviderAndProviderId(AuthProvider provider, String providerId);
    Optional<Users> findByProviderIdAndEmail(String providerId, String email);
    Optional<Users> findByEmailAndProvider(String email, AuthProvider provider);
}
