package com.anuhcollections.backend.address;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AddressRepository
        extends JpaRepository<Address, Long> {

    List<Address> findAllByUser_Id(Long userId);

    Optional<Address> findByIdAndUser_Id(Long addressId, Long userId);

    Optional<Address> findFirstByUser_IdOrderByIdAsc(Long userId);

    boolean existsByUser_Id(Long userId);
}