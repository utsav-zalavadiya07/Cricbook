package com.example.Cricbook.specification;

import com.example.Cricbook.dto.TurfSearchRequest;
import com.example.Cricbook.entity.Turf;
import jakarta.persistence.criteria.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TurfSpecificationTest {

    @Mock
    private Root<Turf> root;

    @Mock
    private CriteriaQuery<?> query;

    @Mock
    private CriteriaBuilder cb;

    @Mock
    private Path<Object> path;

    @Mock
    private Predicate predicate;

    @Mock
    private Expression<String> stringExpression;

    @BeforeEach
    void setUp() {
        // Common stubs used across tests
        when(cb.and(any(Predicate[].class))).thenReturn(predicate);
    }

    @Test
    void testFilter_withCity_addsEqualPredicate() {
        TurfSearchRequest request = new TurfSearchRequest();
        request.setCity("Mumbai");

        when(root.get("city")).thenReturn(path);
        when(cb.lower(any())).thenReturn(stringExpression);
        when(cb.equal(any(), anyString())).thenReturn(predicate);

        Specification<Turf> spec = TurfSpecification.filter(request);
        Predicate result = spec.toPredicate(root, query, cb);

        assertNotNull(result);
        verify(cb).equal(any(), eq("mumbai")); // city is lowercased
    }

    @Test
    void testFilter_withMinPrice_addsGreaterThanOrEqualPredicate() {
        TurfSearchRequest request = new TurfSearchRequest();
        request.setMinPrice(new BigDecimal("500"));

        when(root.<BigDecimal>get("pricePerHour")).thenReturn((Path) path);
        when(cb.greaterThanOrEqualTo(any(), any(BigDecimal.class))).thenReturn(predicate);

        Specification<Turf> spec = TurfSpecification.filter(request);
        Predicate result = spec.toPredicate(root, query, cb);

        assertNotNull(result);
        verify(cb).greaterThanOrEqualTo(any(), eq(new BigDecimal("500")));
    }

    @Test
    void testFilter_withMaxPrice_addsLessThanOrEqualPredicate() {
        TurfSearchRequest request = new TurfSearchRequest();
        request.setMaxPrice(new BigDecimal("2000"));

        when(root.<BigDecimal>get("pricePerHour")).thenReturn((Path) path);
        when(cb.lessThanOrEqualTo(any(), any(BigDecimal.class))).thenReturn(predicate);

        Specification<Turf> spec = TurfSpecification.filter(request);
        Predicate result = spec.toPredicate(root, query, cb);

        assertNotNull(result);
        verify(cb).lessThanOrEqualTo(any(), eq(new BigDecimal("2000")));
    }

    @Test
    void testFilter_withNoFilters_returnsAndWithNoPredicates() {
        TurfSearchRequest request = new TurfSearchRequest();
        // No city, no price, no amenities

        Specification<Turf> spec = TurfSpecification.filter(request);
        Predicate result = spec.toPredicate(root, query, cb);

        assertNotNull(result);
        // cb.and() called with empty array → no predicates added
        verify(cb).and(new Predicate[0]);
    }

    @Test
    void testFilter_withCityAndPriceRange_addsBothPredicates() {
        TurfSearchRequest request = new TurfSearchRequest();
        request.setCity("Delhi");
        request.setMinPrice(new BigDecimal("300"));
        request.setMaxPrice(new BigDecimal("1500"));

        when(root.get("city")).thenReturn(path);
        when(root.<BigDecimal>get("pricePerHour")).thenReturn((Path) path);
        when(cb.lower(any())).thenReturn(stringExpression);
        when(cb.equal(any(), anyString())).thenReturn(predicate);
        when(cb.greaterThanOrEqualTo(any(), any(BigDecimal.class))).thenReturn(predicate);
        when(cb.lessThanOrEqualTo(any(), any(BigDecimal.class))).thenReturn(predicate);

        Specification<Turf> spec = TurfSpecification.filter(request);
        Predicate result = spec.toPredicate(root, query, cb);

        assertNotNull(result);
        verify(cb).equal(any(), eq("delhi"));
        verify(cb).greaterThanOrEqualTo(any(), eq(new BigDecimal("300")));
        verify(cb).lessThanOrEqualTo(any(), eq(new BigDecimal("1500")));
    }

    @Test
    void testFilter_withAmenities_addsJoinPredicate() {
        TurfSearchRequest request = new TurfSearchRequest();
        request.setAmenities(List.of("Cricket Net", "Parking"));

        Join<Object, Object> join = mock(Join.class);
        Path<Object> amenityPath = mock(Path.class);
        when(root.join("amenities")).thenReturn(join);
        when(join.get("amenity")).thenReturn(amenityPath);
        when(amenityPath.in(any(List.class))).thenReturn(predicate);

        Specification<Turf> spec = TurfSpecification.filter(request);
        Predicate result = spec.toPredicate(root, query, cb);

        assertNotNull(result);
        verify(root).join("amenities");
    }
}
