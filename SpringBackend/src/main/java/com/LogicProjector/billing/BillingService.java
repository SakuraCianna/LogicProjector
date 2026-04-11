package com.LogicProjector.billing;

import org.springframework.stereotype.Service;

import com.LogicProjector.account.UserAccount;
import com.LogicProjector.generation.GenerationTask;

@Service
public class BillingService {

    private final BillingRecordRepository billingRecordRepository;

    public BillingService(BillingRecordRepository billingRecordRepository) {
        this.billingRecordRepository = billingRecordRepository;
    }

    public int chargeForCompletedGeneration(UserAccount user, GenerationTask task) {
        int charge = 8;
        user.debitCredits(charge);
        billingRecordRepository.save(new BillingRecord(
                null,
                user,
                task,
                "USAGE",
                -charge,
                user.getCreditsBalance(),
                "Algorithm visualization generation"
        ));
        return charge;
    }
}
