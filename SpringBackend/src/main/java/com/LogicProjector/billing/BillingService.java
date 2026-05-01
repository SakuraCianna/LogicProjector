package com.LogicProjector.billing;

import org.springframework.stereotype.Service;

import com.LogicProjector.account.UserAccount;
import com.LogicProjector.exporttask.ExportTask;
import com.LogicProjector.generation.GenerationTask;

@Service
public class BillingService {

    private static final int GENERATION_CHARGE = 8;

    private final BillingRecordRepository billingRecordRepository;

    public BillingService(BillingRecordRepository billingRecordRepository) {
        this.billingRecordRepository = billingRecordRepository;
    }

    public int chargeForCompletedGeneration(UserAccount user, GenerationTask task) {
        int charge = GENERATION_CHARGE;
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

    public int generationCharge() {
        return GENERATION_CHARGE;
    }

    public void recordExportFreeze(UserAccount user, GenerationTask task, ExportTask exportTask) {
        billingRecordRepository.save(new BillingRecord(
                null,
                user,
                task,
                "EXPORT_FREEZE",
                -exportTask.getCreditsFrozen(),
                user.getCreditsBalance(),
                "Freeze credits for export task " + exportTask.getId()
        ));
    }

    public void settleExportCredits(UserAccount user, GenerationTask task, ExportTask exportTask, int actualCharge) {
        int adjustment = exportTask.getCreditsFrozen() - actualCharge;
        user.settleFrozenCredits(actualCharge, exportTask.getCreditsFrozen());
        billingRecordRepository.save(new BillingRecord(
                null,
                user,
                task,
                "EXPORT_SETTLEMENT",
                adjustment,
                user.getCreditsBalance(),
                "Settle export task " + exportTask.getId() + " with actual charge " + actualCharge
        ));
    }

    public void releaseExportCredits(UserAccount user, GenerationTask task, ExportTask exportTask) {
        user.releaseFrozenCredits(exportTask.getCreditsFrozen());
        billingRecordRepository.save(new BillingRecord(
                null,
                user,
                task,
                "EXPORT_REFUND",
                exportTask.getCreditsFrozen(),
                user.getCreditsBalance(),
                "Release frozen credits for export task " + exportTask.getId()
        ));
    }
}
