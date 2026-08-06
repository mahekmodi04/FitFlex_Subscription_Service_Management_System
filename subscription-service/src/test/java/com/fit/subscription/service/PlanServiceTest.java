package com.fit.subscription.service;

import com.fit.subscription.entity.Plan;
import com.fit.subscription.exception.ResourceNotFoundException;
import com.fit.subscription.repository.PlanRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlanServiceTest {

    @Mock
    private PlanRepository planRepository;

    @InjectMocks
    private PlanService planService;

    private Plan createPlan() {
        Plan plan = new Plan();
        plan.setId(1L);
        plan.setName("Gold");
        plan.setPrice(BigDecimal.valueOf(999));
        plan.setDescription("Gold Plan");
        plan.setDurationDays(30);
        plan.setActive(true);
        return plan;
    }

    @Test
    void createPlan_ShouldSaveSuccessfully() {

        Plan plan = createPlan();

        when(planRepository.findByName("Gold"))
                .thenReturn(Optional.empty());

        when(planRepository.save(plan))
                .thenReturn(plan);

        Plan result = planService.createPlan(plan);

        assertNotNull(result);
        assertEquals("Gold", result.getName());

        verify(planRepository).save(plan);
    }

    @Test
    void createPlan_ShouldThrow_WhenNameAlreadyExists() {

        Plan plan = createPlan();

        when(planRepository.findByName("Gold"))
                .thenReturn(Optional.of(plan));

        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class,
                        () -> planService.createPlan(plan));

        assertEquals(" Plan already exists", exception.getMessage());

        verify(planRepository, never()).save(any());
    }

    @Test
    void getPlanById_ShouldReturnPlan() {

        Plan plan = createPlan();

        when(planRepository.findById(1L))
                .thenReturn(Optional.of(plan));

        Plan result = planService.getPlanById(1L);

        assertEquals("Gold", result.getName());

        verify(planRepository).findById(1L);
    }

    @Test
    void getPlanById_ShouldThrow_WhenNotFound() {

        when(planRepository.findById(1L))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception =
                assertThrows(ResourceNotFoundException.class,
                        () -> planService.getPlanById(1L));

        assertEquals("Plan not found", exception.getMessage());
    }

    @Test
    void getAllPlans_ShouldReturnAllPlans() {

        List<Plan> plans = List.of(createPlan());

        when(planRepository.findAll())
                .thenReturn(plans);

        List<Plan> result = planService.getAllPlans();

        assertEquals(1, result.size());

        verify(planRepository).findAll();
    }

    @Test
    void deletePlan_ShouldDeleteSuccessfully() {

        when(planRepository.existsById(1L))
                .thenReturn(true);

        planService.deletePlan(1L);

        verify(planRepository).deleteById(1L);
    }

    @Test
    void deletePlan_ShouldThrow_WhenPlanNotFound() {

        when(planRepository.existsById(1L))
                .thenReturn(false);

        ResourceNotFoundException exception =
                assertThrows(ResourceNotFoundException.class,
                        () -> planService.deletePlan(1L));

        assertEquals(
                "Plan does not exist, cannot delete",
                exception.getMessage()
        );

        verify(planRepository, never()).deleteById(any());
    }

    @Test
    void updatePlan_ShouldUpdateSuccessfully() {

        Plan existing = createPlan();

        Plan updated = createPlan();
        updated.setName("Premium");
        updated.setPrice(BigDecimal.valueOf(1499));

        when(planRepository.findById(1L))
                .thenReturn(Optional.of(existing));

        when(planRepository.findByName("Premium"))
                .thenReturn(Optional.empty());

        when(planRepository.save(any(Plan.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Plan result = planService.updatePlan(1L, updated);

        assertEquals("Premium", result.getName());
        assertEquals(BigDecimal.valueOf(1499), result.getPrice());

        verify(planRepository).save(existing);
    }

    @Test
    void updatePlan_ShouldThrow_WhenDuplicateNameExists() {

        Plan existing = createPlan();

        Plan updated = createPlan();
        updated.setName("Premium");

        Plan duplicate = createPlan();
        duplicate.setName("Premium");

        when(planRepository.findById(1L))
                .thenReturn(Optional.of(existing));

        when(planRepository.findByName("Premium"))
                .thenReturn(Optional.of(duplicate));

        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class,
                        () -> planService.updatePlan(1L, updated));

        assertEquals(
                "Plan name already exists",
                exception.getMessage()
        );

        verify(planRepository, never()).save(any());
    }

    @Test
    void updatePlan_ShouldThrow_WhenPlanDoesNotExist() {

        when(planRepository.findById(1L))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception =
                assertThrows(ResourceNotFoundException.class,
                        () -> planService.updatePlan(1L, createPlan()));

        assertEquals(
                "Plan id does not exist",
                exception.getMessage()
        );
    }
}
