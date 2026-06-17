package com.example.Cricbook.specification;

import com.example.Cricbook.dto.TurfSearchRequest;
import com.example.Cricbook.entity.Turf;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class TurfSpecification {

    public static Specification<Turf> filter(TurfSearchRequest request) {
        return (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            if (request.getCity() != null && !request.getCity().isEmpty()) {
                predicates.add(cb.equal(cb.lower(root.get("city")), request.getCity().toLowerCase()));
            }

            if (request.getMinPrice() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("pricePerHour"), request.getMinPrice()));
            }

            if (request.getMaxPrice() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("pricePerHour"), request.getMaxPrice()));
            }
            
            // Note: amenities filtering can be complex if it's a list. 
            // We can add it here if needed, but the prompt's example didn't include it directly in the builder.
            // For now, sticking to the provided implementation and adding basic amenities filtering if present.
            
            if (request.getAmenities() != null && !request.getAmenities().isEmpty()) {
                 // Using a join to filter by amenities. Since an amenity has a name, we can do an in clause.
                 predicates.add(root.join("amenities").get("amenity").in(request.getAmenities()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
