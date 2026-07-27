package com.eneik.generated.service;

import com.eneik.generated.campaign.repository.LeadRepository;
import com.eneik.generated.repository.DialogueStateRepository;
import com.eneik.generated.repository.TGAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CampaignStateService {

    private final TGAccountRepository tgAccountRepository;
    private final LeadRepository leadRepository;
    private final DialogueStateRepository dialogueStateRepository;

    public CampaignStateService(
            TGAccountRepository tgAccountRepository,
            LeadRepository leadRepository,
            DialogueStateRepository dialogueStateRepository) {
        this.tgAccountRepository = tgAccountRepository;
        this.leadRepository = leadRepository;
        this.dialogueStateRepository = dialogueStateRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int updateTGAccountStatusAtomic(Long id, String expectedStatus, String newStatus) {
        return tgAccountRepository.updateStatusAtomic(id, expectedStatus, newStatus);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int updateLeadStatusAtomic(Long id, String expectedStatus, String newStatus) {
        return leadRepository.updateStatusAtomic(id, expectedStatus, newStatus);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int updateDialogueStateAtomic(
            Long id,
            boolean expectedRequired,
            int expectedTurnsCount,
            boolean required,
            int turnsCount) {
        return dialogueStateRepository.updateStateAtomic(id, expectedRequired, expectedTurnsCount, required, turnsCount);
    }
}
