package com.fudgetbudget;

import android.content.Context;

import com.fudgetbudget.model.ProjectedTransaction;
import com.fudgetbudget.model.RecordedTransaction;
import com.fudgetbudget.model.Transaction;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.MonthDay;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class BudgetModel {
    private final String projectionPeriod;
    private final int periodsToProject;
    private final StorageControl storage;
    private Double lowestProjectedBalance;
    private ArrayList<Object[]> projectionsWithBalances;


    public BudgetModel(Context context) {
        this.storage = new StorageControl( context );
        this.projectionPeriod = (String) storage.readSettingsValue("projection_period");
        this.periodsToProject = (int) storage.readSettingsValue("projection_periods_to_project");
        setProjectionsWithBalances();
    }

    public ArrayList<Object[]> getProjectionsWithBalances(){ return this.projectionsWithBalances; }
    public void setProjectionsWithBalances(){
        ArrayList<Object[]> result = new ArrayList<>(  );
        LinkedHashMap<LocalDate, ArrayList<Double>> balances = getProjectedBalancesByPeriod();
        LinkedHashMap<LocalDate, ArrayList<Transaction>> projectedTransactions = getProjectedTransactionsByPeriod();
        final Double[] indexBal = {getCurrentBalance()};
        balances.forEach( (balancePeriodDate, balanceList) -> {
            ArrayList<Double> periodBalances = balanceList;
            ArrayList<Transaction> periodTransactions = projectedTransactions.get( balancePeriodDate );
            LinkedHashMap<Transaction, Double> periodProjections = new LinkedHashMap<>(  );

            for( Transaction transaction : periodTransactions) {
                Double endBal;
                if(transaction.getIncomeFlag()) endBal = indexBal[0] + (Double)transaction.getProperty( R.string.amount_tag );
                else endBal = indexBal[0] - (Double)transaction.getProperty( R.string.amount_tag );
                periodProjections.put( transaction, endBal );
                indexBal[0] = endBal;
                //TODO: use endBal to set flags for going below zero balance or threshold
            }

            Object[] projectionItemData = new Object[]{balancePeriodDate, periodBalances, periodProjections};
            result.add( projectionItemData );
        } );

        this.projectionsWithBalances = result;
    }
    public <T extends Transaction> LinkedHashMap<LocalDate, ArrayList<Double>> getProjectedBalancesByPeriod(){
        LinkedHashMap<LocalDate, ArrayList<Double>> result = new LinkedHashMap<>();
        double tempCurrentBalance = this.getCurrentBalance();

        LinkedHashMap<LocalDate, ArrayList<T>> projectedTransactionByPeriod = getProjectedTransactionsByPeriod();

        for( Map.Entry<LocalDate, ArrayList<T>> periodEntry: projectedTransactionByPeriod.entrySet()){
            LocalDate period = periodEntry.getKey();
            ArrayList<T> projectionList = periodEntry.getValue();
            double beginningBalance, endingBalance, incomeSum, expenseSum;

            beginningBalance = tempCurrentBalance;
            incomeSum = 0;
            expenseSum = 0;

            for(T projection : projectionList){
                if( projection.getIncomeFlag() ) incomeSum += (Double)projection.getProperty(R.string.amount_tag );
                else expenseSum += (Double)projection.getProperty( R.string.amount_tag );
            }

            endingBalance = beginningBalance + incomeSum - expenseSum;

            ArrayList<Double> balanceArray = new ArrayList<>(  );
            balanceArray.add(beginningBalance);
            balanceArray.add( incomeSum );
            balanceArray.add( expenseSum );
            balanceArray.add( endingBalance );

            result.put( period, balanceArray );
            tempCurrentBalance = endingBalance;
        }


        return result;
    }
    public <T extends Transaction> LinkedHashMap<LocalDate, ArrayList<T>> getProjectedTransactionsByPeriod(){
        LinkedHashMap<LocalDate, ArrayList<T>> result = new LinkedHashMap<>();
        ArrayList<T> allProjections = new ArrayList<>(  );
        LocalDate cutoffDate = this.getCutoffDate();
        LocalDate currentDate = LocalDate.now();
        LocalDate firstProjectedDate;


        storage.readTransactions( "all_active" ).forEach( transaction ->
                allProjections.addAll( getProjections( transaction ) ) );

        if(this.projectionPeriod.contentEquals( "days" )) firstProjectedDate = currentDate;
        else if(this.projectionPeriod.contentEquals( "weeks" )) firstProjectedDate = currentDate.with( DayOfWeek.MONDAY );
        else if(this.projectionPeriod.contentEquals( "months" )) firstProjectedDate = currentDate.with( MonthDay.of( currentDate.getMonth(), 1 ) );
        else if(this.projectionPeriod.contentEquals( "years" )) firstProjectedDate = currentDate.with( Month.JANUARY ).with( MonthDay.of( Month.JANUARY, 1 ));
        else throw new Error("invalid period set for: BudgetModel.projectionPeriod");


        for( int index = 0; index <= this.periodsToProject; index ++){
            LocalDate indexDate;
            if(this.projectionPeriod.contentEquals( "days" )) indexDate = firstProjectedDate.plusDays( index );
            else if(this.projectionPeriod.contentEquals( "weeks" )) indexDate = firstProjectedDate.plusWeeks( index );
            else if(this.projectionPeriod.contentEquals( "months" )) indexDate = firstProjectedDate.plusMonths( index );
            else if(this.projectionPeriod.contentEquals( "years" )) indexDate = firstProjectedDate.plusYears( index );
            else throw new Error("invalid period set for: BudgetModel.projectionPeriod");

            result.put( indexDate, new ArrayList<>(  ));
        }

        while(!allProjections.isEmpty()){
            T projection = allProjections.remove( 0 );
            LocalDate projectedDate = (LocalDate) projection.getProperty( R.string.date_tag );
            if (projectedDate.isAfter( cutoffDate )) continue;

            if(projectedDate.isBefore( firstProjectedDate )){
                int index = 0;
                while(projectedDate.isBefore( firstProjectedDate )){
                    LinkedHashMap<LocalDate, ArrayList<T>> result_copy = new LinkedHashMap<>(  );
                    index++;

                    if(this.projectionPeriod.contentEquals( "days" )) firstProjectedDate = firstProjectedDate.minusDays( index );
                    else if(this.projectionPeriod.contentEquals( "weeks" )) firstProjectedDate = firstProjectedDate.minusWeeks( index );
                    else if(this.projectionPeriod.contentEquals( "months" )) firstProjectedDate = firstProjectedDate.minusMonths( index );
                    else if(this.projectionPeriod.contentEquals( "years" )) firstProjectedDate = firstProjectedDate.minusYears( index );
                    else throw new Error("invalud property value for: BudgetModel.projectionPeriod");

                    result_copy.put( firstProjectedDate, new ArrayList<>(  ));
                    result.forEach( (date, projectionList) -> result_copy.put( date, projectionList ) );
                    result = result_copy;
                }

            }


            LocalDate resultInsertionDate = null;
            int keyIndex = 0;
            Object[] keySet = result.keySet().toArray();

            while(resultInsertionDate == null && keyIndex < keySet.length){
                LocalDate firstDayOfPeriod = ((LocalDate)keySet[keyIndex]);
                LocalDate firstDayOfNextPeriod;
                if(keyIndex == keySet.length -1) firstDayOfNextPeriod = cutoffDate;
                else firstDayOfNextPeriod = ((LocalDate)keySet[keyIndex + 1]);

                if( projectedDate.isAfter( firstDayOfPeriod.minusDays( 1 ) ) && projectedDate.isBefore( firstDayOfNextPeriod ) )
                    resultInsertionDate = (LocalDate) keySet[keyIndex];
                keyIndex++;
            }

            if(resultInsertionDate != null && !resultInsertionDate.isAfter( cutoffDate )){
                ArrayList<T> resultSet = result.get( resultInsertionDate );

                resultSet.add( projection );
                Collections.sort( resultSet );
                result.replace( resultInsertionDate, resultSet );
            }


        }

        return result;
    }

    public ArrayList<RecordedTransaction> getRecords(String type){
        return storage.readRecords( type );
    }

    public ArrayList<Transaction> getTransactionsByType(Object type){
        return storage.readTransactions( type );
    }

    public Double getCurrentBalance() {
        ArrayList<RecordedTransaction> records = storage.readRecords( "all" );
        double balance = 0.00;

        for( RecordedTransaction recordedTransaction : records) {
            boolean isIncome = recordedTransaction.getIncomeFlag();
            double amount = (Double) recordedTransaction.getProperty( R.string.amount_tag );

            if(isIncome) balance = balance + amount;
            else balance = balance - amount;
        }

        return balance;
    }

    public Double getLowestProjectedBalance(){
        if(this.lowestProjectedBalance == null) this.lowestProjectedBalance = getCurrentBalance();
        return this.lowestProjectedBalance;
    }
    public void setLowestProjectedBalance(Double lowBalance) { this.lowestProjectedBalance = lowBalance; }

    public Double getThresholdValue(){ return 200.0; }

    public LocalDate getCutoffDate() {
        LocalDate currentDate = LocalDate.now();
        switch (this.projectionPeriod) {
            case "days": return currentDate.plusDays( this.periodsToProject );
            case "weeks": return currentDate.with( DayOfWeek.SUNDAY ).plusWeeks( this.periodsToProject );
            case "months": return currentDate.with( MonthDay.of( currentDate.getMonth(), currentDate.lengthOfMonth() ) ).plusMonths( this.periodsToProject );
            case "years": return currentDate.with( MonthDay.of( Month.DECEMBER, 31)).plusYears( this.periodsToProject );
            default: return currentDate;
        }
    }


    public boolean update(Object object){
        if(object instanceof ProjectedTransaction) return storage.writeProjection( (ProjectedTransaction) object );
        else if(object instanceof RecordedTransaction) {
            //TODO: handle user trying to update record to future date
            return storage.writeRecord( (RecordedTransaction) object );
        }
        else if(object instanceof Transaction) {
            Transaction transaction = (Transaction) object;
            boolean clearProjections = transaction.getClearStoredProjectionsFlag();

            if(clearProjections) storage.readProjections( transaction ).values().forEach( projection ->
                    storage.deleteProjection( projection ) );

            return storage.writeTransaction( (Transaction) object );
        }
        else if(object instanceof String[] && ((String[])object).length == 2){
            String key = ((String[])object)[0];
            String value = ((String[])object)[1];
            return storage.writeSettingsValue(key, value);
        }
        else return false;
    }

    public boolean reconcile(Object object){
        //TODO: handle user trying to reconcile with a date in the future
        if(object instanceof ProjectedTransaction){
            ProjectedTransaction projectedTransaction = (ProjectedTransaction) object;
            RecordedTransaction record = RecordedTransaction.getInstance(projectedTransaction);
            Transaction transaction = storage.readTransactions( record ).get( 0 );

            transaction.setProperty( R.string.date_tag, getNextProjectedDate( transaction, ((LocalDate)transaction.getProperty( R.string.date_tag ))));

            storage.deleteProjection( projectedTransaction );
            storage.writeRecord( record );
            storage.writeTransaction( transaction );

            return true;
        }
        else if(object instanceof Transaction){
            Transaction transaction = (Transaction) object;
            RecordedTransaction record = RecordedTransaction.getInstance( transaction );

            transaction.setProperty( R.string.date_tag, "" );

            storage.writeRecord( record );
            storage.writeTransaction( transaction );

            return true;
        } else return false;
    }

    public boolean delete(Object object){
        if(object instanceof ProjectedTransaction) return storage.deleteProjection((ProjectedTransaction) object);
        else if(object instanceof RecordedTransaction) return storage.deleteRecord((RecordedTransaction) object);
        else if(object instanceof Transaction) return storage.deleteTransaction((Transaction) object);
        else return false;
    }

    public <T extends Transaction> ArrayList<T> getProjections(Transaction transaction) {
        //this will take the transaction and return a list of all projected transactions to the cutoff date
        //list will generate projection when one is not found in storage, will load stored projection if found

        ArrayList<T> projections = new ArrayList<>(  );
        String recurrance = transaction.getProperty( R.string.recurrence_tag ).toString();

        if(recurrance.contentEquals( "0-0-0-0-0-0-0" )) projections.add( (T)transaction );
        else {
            LocalDate indexDate = (LocalDate) transaction.getProperty( R.string.date_tag );
            LinkedHashMap<LocalDate, ProjectedTransaction> storedProjections = storage.readProjections( transaction );


            if(storedProjections.size() > 0){
                LocalDate firstStoredProjectionDate = (LocalDate) storedProjections.keySet().toArray()[0];
                if(firstStoredProjectionDate.isBefore( indexDate )) indexDate = firstStoredProjectionDate;
            }

            while(!indexDate.isAfter( getCutoffDate() )){
                ProjectedTransaction projection = storedProjections.get( indexDate );

                if(projection == null) projection = ProjectedTransaction.getInstance( transaction, indexDate );
                projections.add( (T) projection );
                indexDate = getNextProjectedDate(transaction, indexDate);
            }

        }

        return projections;
    }

    private LocalDate getNextProjectedDate(Transaction transaction, LocalDate indexDate) {
        String[] recurrenceGroups = transaction.getProperty(R.string.recurrence_tag).toString().split( "-" );
        int recurrenceBit = Integer.parseInt( recurrenceGroups[0] );
        int frequency = Integer.parseInt( recurrenceGroups[1] );
        String period = recurrenceGroups[2];
        String onDayType = recurrenceGroups[3];
        int endRecurrenceBit = Integer.parseInt( recurrenceGroups[4] );
        String endParameterType = recurrenceGroups[5];
        String endParameterValue = recurrenceGroups[6];

        LocalDate nextDate;
        if(recurrenceBit == 0) nextDate = null;
        else if(period.contentEquals( "Day" )) nextDate = indexDate.plusDays( frequency );
        else if(period.contentEquals( "Week" )) nextDate = indexDate.plusWeeks( frequency );
        else if(period.contentEquals("Month" )){
            if(onDayType.contentEquals( "DayOfMonth" )) nextDate = indexDate.plusMonths( frequency );
            else if(onDayType.contentEquals( "WeekDayOfMonth" )){
                int indexDayOfWeekValue = indexDate.getDayOfWeek().getValue();
                int tempDayValue = indexDate.getDayOfMonth();
                int weekCount = 1;

                while (tempDayValue > 7) {
                    tempDayValue = tempDayValue - 7;
                    weekCount++;
                }

                //create a temp next date set to the correct month, but starting on day 1
                //then increment until it is the correct day of the week
                //once on correct day of week, increment by week until count at 1
                //this should land us on the correct day (mostly: doesn't handle 5th and/or last day of week of the month
                LocalDate tempNextDate = indexDate.plusMonths( frequency ).withDayOfMonth( 1 );
                while( tempNextDate.getDayOfWeek().getValue() != indexDayOfWeekValue )  {
                    tempNextDate.plusDays( 1 );
                }
                while( weekCount > 1) {
                    tempNextDate.plusWeeks( 1 );
                    weekCount--;
                }

                nextDate = tempNextDate;
            }
            else nextDate = null;
        }
        else if(period.contentEquals( "Year" )) nextDate = indexDate.plusYears( frequency );
        else nextDate = null;

        if(endRecurrenceBit == 0) return nextDate;
        else if(endParameterType.contentEquals( "StopAfterDate" )){
            LocalDate endDate = LocalDate.parse( endParameterValue, DateTimeFormatter.BASIC_ISO_DATE );
            if(endDate.isAfter( nextDate )) return nextDate;
            else return null;
        }
        else if(endParameterType.contentEquals( "StopAfterOccurrenceCount" )){
            //don't have this setup for now, will require checking records and stored projections to get the count
            return nextDate;
        }
        else if(endParameterType.contentEquals( "StopAfterRecordedAmount" )){
            //don't have this setup for now, will require checking records and stored projections to get balance
            return nextDate;
        }
        else return nextDate;
    }

}