package com.example.Cricbook.repository;

import com.example.Cricbook.entity.StaffEquipmentItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StaffEquipmentItemRepository extends JpaRepository<StaffEquipmentItem, Long> {
    List<StaffEquipmentItem> findByCenterIdInOrderByNameAsc(List<Long> centerIds);
    List<StaffEquipmentItem> findByStatusOrderByNameAsc(String status);
}
