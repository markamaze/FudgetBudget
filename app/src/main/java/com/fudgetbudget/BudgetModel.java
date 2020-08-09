package com.fudgetbudget;

import android.content.Context;

import com.fudgetbudget.model.ProjectedTransaction;
import com.fudgetbudget.model.RecordedTransaction;
import com.fudgetbudget.model.Transaction;

import java.time.LocalDate;
import java.time.MonthDay;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

class BudgetModel<T extends Transaction> {
    private final int periodsToProject;
    private final StorageControl storage;

    private Double lowestProjectedBalance;
    private LocalDate lowestProjectedBalanceDate;

    private ArrayList<Object[]> projectionsWithBalancesByPeriod;
    private LinkedHashMap<LocalDate, ArrayList<T>> projectedTransactionsByPeriod;
    private LinkedHashMap<LocalDate, ArrayList<Double>> projectedBalancesByPeriod;


    BudgetModel(Context context) {
        this.storage = new StorageControl( context );
        this.periodsToProject = (int) storage.readSettingsValue("projection_periods_to_project");
        setProjectedTransactionsByPeriod();
        setProjectedBalancesByPeriod();
        setProjectionsWithBalancesByPeriod();
    }


    //access projected transactions with balances grouped by period
    ArrayList<Object[]> getProjectionsWithBalancesByPeriod(){ return this.projectionsWithBalancesByPeriod; }
    private void setProjectionsWithBalancesByPeriod(){
        ArrayList<Object[]> result = new ArrayList<>(  );
        LinkedHashMap<LocalDate, ArrayList<Double>> balances = getProjectedBalancesByPeriod();
        LinkedHashMap<LocalDate, ArrayList<T>> projectedTransactions = getProjectedTransactionsByPeriod();
        final Double[] indexBal = {getCurrentBalance()};
        balances.forEach( (balancePeriodDate, balanceList) -> {
            ArrayList<T> periodTransactions = projectedTransactions.get( balancePeriodDate );
            LinkedHashMap<Transaction, Double> periodProjections = new LinkedHashMap<>(  );

            for( Transaction transaction : periodTransactions) {
                Double endBal;
                if(transaction.getIncomeFlag()) endBal = indexBal[0] + (Double)transaction.getProperty( R.string.amount_tag );
                else endBal = indexBal[0] - (Double)transaction.getProperty( R.string.amount_tag );
                periodProjections.put( transaction, endBal );
                indexBal[0] = endBal;

                setProjectedBalanceFlags(indexBal[0], (LocalDate)transaction.getProperty( R.string.date_tag ));
            }

            Object[] projectionItemData = new Object[]{balancePeriodDate, balanceList, periodProjections};
            result.add( projectionItemData );
        } );

        this.projectionsWithBalancesByPeriod = result;
    }

    private LinkedHashMap<LocalDate, ArrayList<Double>> getProjectedBalancesByPeriod(){ return this.projectedBalancesByPeriod; }
    private void setProjectedBalancesByPeriod(){
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


        this.projectedBalancesByPeriod = result;
    }

    LinkedHashMap<LocalDate, ArrayList<T>> getProjectedTransactionsByPeriod(){ return projectedTransactionsByPeriod; }
    private void setProjectedTransactionsByPeriod(){
        LinkedHashMap<LocalDate, ArrayList<T>> result = new LinkedHashMap<>();
        ArrayList<T> allProjections = new ArrayList<>(  );
        LocalDate cutoffDate = this.getCutoffDate();
        LocalDate currentDate = LocalDate.now();
        LocalDate firstProjectedDate;


        storage.readTransactions( "all_active" ).forEach( transaction ->
                allProjections.addAll( getProjections( transaction ) ) );

        firstProjectedDate = currentDate.with( MonthDay.of( currentDate.getMonth(), 1 ) );


        for( int index = 0; index <= this.periodsToProject; index ++){
            LocalDate indexDate = firstProjectedDate.plusMonths( index );
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
                    firstProjectedDate = firstProjectedDate.minusMonths( index );
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

        this.projectedTransactionsByPeriod = result;
    }

    private LocalDate getCutoffDate() {
        LocalDate currentDate = LocalDate.now();
        return currentDate.with( MonthDay.of( currentDate.getMonth(), currentDate.lengthOfMonth() ) ).plusMonths( this.periodsToProject );
    }
    private void setProjectedBalanceFlags(Double balance, LocalDate balanceDate){
        ArrayList<RecordedTransaction> records = getRecords( "all" );
        if(this.lowestProjectedBalance == null) {
            this.lowestProjectedBalance = getCurrentBalance();
            if(records.isEmpty()) this.lowestProjectedBalanceDate = LocalDate.now();
            else this.lowestProjectedBalanceDate = (LocalDate)(records.get( records.size()-1 ).getProperty( R.string.date_tag ));
        }
        if(balance < this.lowestProjectedBalance) {
            this.lowestProjectedBalance = balance;
            this.lowestProjectedBalanceDate = balanceDate;
        }

        //add setting of flags for notification of going below threshold or zero balances
    }


    //access all transaction objects: Transaction, Records, Projections
    ArrayList<RecordedTransaction> getRecords(Object type){
        return storage.readRecords( type );
    }
    ArrayList<Transaction> getTransactionsByType(Object type){
        return storage.readTransactions( type );
    }
    ArrayList<T> getProjections(Transaction transaction) {
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
        int period = Integer.parseInt( recurrenceGroups[2] );
        int onDayType = Integer.parseInt( recurrenceGroups[3] );
        int endRecurrenceBit = Integer.parseInt( recurrenceGroups[4] );
        String endParameterType = recurrenceGroups[5];
        String endParameterValue = recurrenceGroups[6];

        LocalDate nextDate;
        if(recurrenceBit == 0) nextDate = null;
        else if(period == 0) nextDate = indexDate.plusDays( frequency );
        else if(period == 1) nextDate = indexDate.plusWeeks( frequency );
        else if(period == 2){
            if(onDayType == 0) nextDate = indexDate.plusMonths( frequency );
            else if(onDayType == 1){
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
        else if(period == 3) nextDate = indexDate.plusYears( frequency );
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



    //perform create, update, delete actions on storage
    boolean update(Object object){
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
    boolean reconcile(Object object){
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
    boolean delete(Object object){
        if(object instanceof ProjectedTransaction) return storage.deleteProjection((ProjectedTransaction) object);
        else if(object instanceof RecordedTransaction) return storage.deleteRecord((RecordedTransaction) object);
        else if(object instanceof Transaction) {
            Transaction transaction = (Transaction) object;

            getProjections( transaction ).forEach( projection -> storage.deleteProjection( (ProjectedTransaction)projection ) );
            transaction.setProperty( R.string.date_tag, "" );

            return storage.writeTransaction( transaction );
        }
        else return false;
    }


    //access information on projected balances
    Double getExpendableValue() {
        Double thresholdAmount = getThresholdValue();
        Double lowestProjected = this.lowestProjectedBalance;

        if(lowestProjected == null || lowestProjected <= thresholdAmount) return 0.0;
        return lowestProjected - thresholdAmount;
    }
    Double getCurrentBalance() {
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
    Double getThresholdValue(){ return (Double) storage.readSettingsValue( "balance_threshold" ); }
    Double getProjectedAverageValue(int type){
        //for now, just exclude the first period because of the issues with carrying the balance for the period following reconciling a projection
        LinkedHashMap<LocalDate, ArrayList<T>> projectionsByPeriod = getProjectedTransactionsByPeriod();
        boolean firstPeriodSkipped = false;
        Double result = 0.0;
        Double[] totals = new Double[projectionsByPeriod.size()];
        for(int i = 0; i < totals.length; i++) totals[i] = 0.0;

        int index = -1;
        for(ArrayList<T> periodProjections : projectionsByPeriod.values()){
            if(!firstPeriodSkipped) firstPeriodSkipped = true;
            else for(T projection : periodProjections) {
                if( (projection.getIncomeFlag() && type == R.string.income_tag) || (!projection.getIncomeFlag() && type == R.string.expense_tag) )
                    totals[index] += (Double) projection.getProperty( R.string.amount_tag );
            }
            index++;
        }

        Double sum = 0.0;
        for( Double total : totals ) sum += total;
        result = sum / (totals.length - 1);

        return result;
    }

}