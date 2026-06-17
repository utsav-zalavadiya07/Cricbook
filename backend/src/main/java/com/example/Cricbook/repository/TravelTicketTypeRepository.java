package com.example.Cricbook.repository;

import com.example.Cricbook.entity.TravelTicketType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TravelTicketTypeRepository extends JpaRepository<TravelTicketType, Long> {
    List<TravelTicketType> findByEventIdOrderByPriceAsc(Long eventId);
}
