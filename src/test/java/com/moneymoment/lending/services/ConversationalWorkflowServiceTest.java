package com.moneymoment.lending.services;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ConversationalWorkflowServiceTest {

    @Test
    void parsesIndianAmountFormats() {
        assertEquals(250_000d, ConversationalWorkflowService.parseAmount("2.5 lakh"));
        assertEquals(10_000_000d, ConversationalWorkflowService.parseAmount("1 crore"));
        assertEquals(75_000d, ConversationalWorkflowService.parseAmount("75k"));
        assertEquals(125_000d, ConversationalWorkflowService.parseAmount("₹1,25,000"));
    }

    @Test
    void rejectsMessagesWithoutAnAmount() {
        assertEquals(0d, ConversationalWorkflowService.parseAmount("not decided"));
        assertEquals(0d, ConversationalWorkflowService.parseAmount(null));
    }
}
