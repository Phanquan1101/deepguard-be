package com.deepguard.billing.config;

import com.deepguard.billing.entity.CreditRule;
import com.deepguard.billing.enums.ActionType;
import com.deepguard.billing.repository.CreditRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Configuration
@RequiredArgsConstructor
public class CreditRuleInitializer {

    private static final int DEFAULT_IMAGE_SCAN_COST = 5;
    private static final int DEFAULT_VIDEO_SCAN_COST = 20;
    private static final int DEFAULT_AUDIO_SCAN_COST = 10;

    private final CreditRuleRepository creditRuleRepository;

    @Bean
    CommandLineRunner initCreditRules() {
        return args -> {

            List<ActionType> requiredRules = List.of(ActionType.IMAGE_SCAN, ActionType.VIDEO_SCAN, ActionType.AUDIO_SCAN);

            List<CreditRule> existingRules = creditRuleRepository.findAll();

            Set<ActionType> existingTypes = existingRules.stream()
                    .map(CreditRule::getActionType)
                    .collect(Collectors.toSet());

            boolean missingRule = requiredRules.stream().anyMatch(type -> !existingTypes.contains(type));

            if (missingRule) {
                // DELETE ALL
                creditRuleRepository.deleteAll();

                // INSERT DEFAULT RULES
                List<CreditRule> defaultRules = List.of(

                        CreditRule.builder()
                                .actionType(ActionType.IMAGE_SCAN)
                                .creditCost(DEFAULT_IMAGE_SCAN_COST)
                                .description("Credit cost for image scan")
                                .isActive(true)
                                .build(),

                        CreditRule.builder()
                                .actionType(ActionType.VIDEO_SCAN)
                                .creditCost(DEFAULT_VIDEO_SCAN_COST)
                                .description("Credit cost for video scan")
                                .isActive(true)
                                .build(),

                        CreditRule.builder()
                                .actionType(ActionType.AUDIO_SCAN)
                                .creditCost(DEFAULT_AUDIO_SCAN_COST)
                                .description("Credit cost for audio scan")
                                .isActive(true)
                                .build()
                );

                creditRuleRepository.saveAll(defaultRules);

                System.out.println("Default credit rules initialized.");
            }
        };
    }
}
