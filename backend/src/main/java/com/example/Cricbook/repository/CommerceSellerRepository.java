package com.example.Cricbook.repository;

import com.example.Cricbook.entity.CommerceSeller;
import com.example.Cricbook.enums.CommerceSellerStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommerceSellerRepository extends JpaRepository<CommerceSeller, Long> {
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByGstNumberIgnoreCase(String gstNumber);
    List<CommerceSeller> findByStatusOrderByCreatedAtDesc(CommerceSellerStatus status);
    List<CommerceSeller> findAllByOrderByCreatedAtDesc();
}
