package com.fit.subscription.repository;

import com.fit.subscription.entity.AddOn;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AddOnRepository extends JpaRepository<AddOn, Long> {
    List<AddOn> findByActiveTrue();

    Optional<AddOn> findByNameIgnoreCaseAndActiveTrue(String name);

}
