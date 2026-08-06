package com.fit.subscription.service;

import com.fit.subscription.entity.Plan;
import com.fit.subscription.exception.ResourceNotFoundException;
import com.fit.subscription.repository.PlanRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PlanService {
    private final PlanRepository planRepository;

    //constructor injection
    public PlanService(PlanRepository planrepository){
        this.planRepository = planrepository;
    }

    public Plan createPlan(Plan plan){
        if(planRepository.findByName(plan.getName()).isPresent()){
            throw new IllegalArgumentException(" Plan already exists");
        }
        return planRepository.save(plan);

    }

    public List<Plan> getAllPlans(){
        return planRepository.findAll();
    }

    public Plan getPlanById(Long id){
        return planRepository.findById(id).
                orElseThrow(() -> new ResourceNotFoundException("Plan not found"));
    }

    public void deletePlan(Long id){
        if(!planRepository.existsById(id)){
            throw new ResourceNotFoundException("Plan does not exist, cannot delete");
        }
        planRepository.deleteById(id);
    }

    public Plan updatePlan(Long id, Plan updatedPlan){
        Plan existingPlan = planRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Plan id does not exist"));

        if (!existingPlan.getName().equals(updatedPlan.getName())
                && planRepository.findByName(updatedPlan.getName()).isPresent()) {
            throw new IllegalArgumentException("Plan name already exists");
        }

        existingPlan.setName(updatedPlan.getName());
        existingPlan.setPrice(updatedPlan.getPrice());
        existingPlan.setActive(updatedPlan.getActive());
        existingPlan.setDescription(updatedPlan.getDescription());
        existingPlan.setTier(updatedPlan.getTier());
        existingPlan.setDurationDays(updatedPlan.getDurationDays());

        return planRepository.save(existingPlan);
    }
}


