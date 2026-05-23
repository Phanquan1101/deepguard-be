package com.deepguard.billing.service.impl;

import com.deepguard.billing.dto.response.PricingPlanResponse;
import com.deepguard.billing.mapper.PricingPlanMapper;
import com.deepguard.billing.repository.PricingPlanRepository;
import com.deepguard.billing.service.PricingPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PricingPlanServiceImpl implements PricingPlanService {

    private final PricingPlanRepository pricingPlanRepository;
    private final PricingPlanMapper pricingPlanMapper;

    @Override
    @Transactional(readOnly = true)
    public List<PricingPlanResponse> getAllPlans() {
        return pricingPlanMapper.toResponseList(pricingPlanRepository.findAllByOrderByPriceAsc());
    }
}
