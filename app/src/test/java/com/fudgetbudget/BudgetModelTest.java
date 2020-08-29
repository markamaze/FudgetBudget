package com.fudgetbudget;

import com.fudgetbudget.model.Transaction;

import org.junit.Test;

import java.time.LocalDate;
import java.util.ArrayList;

import static org.junit.Assert.*;


public class BudgetModelTest {

    @Test
    public <T extends Transaction> void testGetNextProjectedDate(){
        BudgetModel<T> budgetModel = new BudgetModel<T>( null );

        LocalDate testDate = LocalDate.now();
        LocalDate nextOccurrence;
        String recurrenceString;


        recurrenceString = "1-1-1-1-0-0-0";
        nextOccurrence = budgetModel.getNextProjectedDate(recurrenceString, testDate );
        assertEquals( nextOccurrence, testDate.plusWeeks( 1 ) );

        recurrenceString = "1-1-2-0-0-0-0";
        nextOccurrence = budgetModel.getNextProjectedDate( recurrenceString, testDate );
        assertEquals( nextOccurrence, testDate.plusMonths( 1 ) );

        recurrenceString = "1-1-3-0-0-0-0";
        nextOccurrence = budgetModel.getNextProjectedDate( recurrenceString, testDate );
        assertEquals( nextOccurrence, testDate.plusYears( 1 ) );
    }
}