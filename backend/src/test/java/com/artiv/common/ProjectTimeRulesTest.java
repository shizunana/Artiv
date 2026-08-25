package com.artiv.common;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectTimeRulesTest {

    @Test
    void deadlineDateItselfRemainsRecruitableUntilTheNextDay() {
        assertFalse(ProjectTimeRules.isExpired(LocalDate.now()));
        assertTrue(ProjectTimeRules.isExpired(LocalDate.now().minusDays(1)));
    }

    @Test
    void editingRequiresAtLeastTwoDaysFromToday() {
        assertThrows(BusinessException.class,
                () -> ProjectTimeRules.requireEditableDeadline(LocalDate.now().plusDays(1)));
        ProjectTimeRules.requireEditableDeadline(LocalDate.now().plusDays(2));
    }
}
