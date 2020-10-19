package com.fudgetbudget;

import com.fudgetbudget.model.ProjectedTransaction;
import com.fudgetbudget.model.RecordedTransaction;
import com.fudgetbudget.model.Transaction;

import java.net.URI;
import java.time.LocalDate;
import java.time.MonthDay;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;

public class FudgetBudgetRepo<T extends Transaction> {
    private final StorageControl storage;
    private ArrayList<Transaction> transactions;
    private ArrayList<RecordedTransaction> records;
    private Double currentBalance;


    public FudgetBudgetRepo(String filesDir) {
        this.storage = new StorageControl( filesDir );

        transactions = getActiveTransactions();
        records = getRecordedTransactions();
        currentBalance = getCurrentBalance();
    }

    public ArrayList<Transaction> getActiveTransactions(){
        if(transactions == null){
            ArrayList<Transaction> result = new ArrayList<>( storage.readTransactions( "all_active" ) );
            Collections.sort( result );
            transactions = result;
        }
        return transactions;
    }
    public ArrayList<RecordedTransaction> getRecordedTransactions(){
        ArrayList<RecordedTransaction> result = new ArrayList<>( storage.readRecords( R.string.records_type_all ) );
        Collections.reverse( result );
        return result;
    }
    public LinkedHashMap<LocalDate, ProjectedTransaction> getTransactionProjections(Transaction transaction){
        return storage.readProjections( transaction );
    }
    public ArrayList<RecordedTransaction> getTransactionRecords(URI path){
        return storage.readRecords( path );
    }
    public Double getCurrentBalance(){
        if(currentBalance == null){
            ArrayList<RecordedTransaction> records = storage.readRecords( R.string.records_type_all );
            double balance = 0.00;

            for( RecordedTransaction recordedTransaction : records) {
                Double amount = (Double) recordedTransaction.getProperty( R.string.amount_tag );

                if(recordedTransaction.getIncomeFlag()) balance = balance + amount;
                else balance = balance - amount;
            }
            currentBalance = balance;
        }

        return currentBalance;
    }
    public Object getSettingsValue(int settingsResource){
        switch(settingsResource){
            case R.string.setting_value_periods_to_project:
                return Integer.parseInt( storage.readSettingsValue("projection_periods_to_project").toString() );
//                return 6;
            case R.string.setting_value_balance_threshold:
                return Double.parseDouble( storage.readSettingsValue( "balance_threshold" ).toString() );
            default: return null;
        }
    }

    private LocalDate projectToNextDate(String recurrenceString, LocalDate indexDate) {
        String[] recurrenceGroups = recurrenceString.split( "-" );
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
                    tempNextDate = tempNextDate.plusDays( 1 );
                }
                while( weekCount > 1) {
                    tempNextDate = tempNextDate.plusWeeks( 1 );
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


    //update to be sure that each method makes necessary changes to the cache
    //  without this, I will have to reread all files way too much
    public boolean update(Object object){
        if(object instanceof ProjectedTransaction) {
            return storage.writeProjection( (ProjectedTransaction) object );
        }
        else if(object instanceof RecordedTransaction) {
            //TODO: handle user trying to update record to future date
            return storage.writeRecord( (RecordedTransaction) object );
        }
        else if(object instanceof Transaction) {
            Transaction transaction = (Transaction) object;
            boolean clearProjections = transaction.getClearStoredProjectionsFlag();
            boolean deleteFailed = false;

            if(clearProjections) {
                for( ProjectedTransaction projection : storage.readProjections( transaction ).values() ){
                    if (!storage.deleteProjection( projection )) deleteFailed = true;
                }

            }
            if(deleteFailed) return false;
            else return storage.writeTransaction( (Transaction) object );
        }
        else if(object instanceof String[] && ((String[])object).length == 2){
            String key = ((String[])object)[0];
            String value = ((String[])object)[1];
            if(value.substring( 0, 1 ).contentEquals( "$" )) {
                return storage.writeSettingsValue( key, value.substring( 1 ) );
            } else {
                return storage.writeSettingsValue( key, value );
            }
        }
        else return false;
    }
    public boolean reconcile(Object object){
        if(object instanceof ProjectedTransaction){
            ProjectedTransaction projectedTransaction = (ProjectedTransaction) object;
            RecordedTransaction record = RecordedTransaction.getInstance(projectedTransaction);
            Transaction transaction = storage.readTransactions( record ).get( 0 );

            transaction.setProperty( R.string.date_tag, projectToNextDate( transaction.getProperty( R.string.recurrence_tag ).toString(), ((LocalDate)transaction.getProperty( R.string.date_tag ))));

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
        else if(object instanceof Transaction) {
            Transaction transaction = (Transaction) object;

            //I could use getTransactionRecords to see if any records exist
            //  if so, save the transaction w/o a scheduled date
            //  if not just completely delete the transaction from storage
            getTransactionProjections( transaction ).values().forEach( projection -> {
                if(projection instanceof ProjectedTransaction)
                    storage.deleteProjection( (ProjectedTransaction)projection );
            } );
            transaction.setProperty( R.string.date_tag, "" );

            return storage.writeTransaction( transaction );
        }
        else return false;
    }
    public boolean reset(Object object){
        if(object instanceof ProjectedTransaction) {
            ProjectedTransaction projectedTransaction = (ProjectedTransaction) object;
            Double scheduledAmount = projectedTransaction.getScheduledAmount();
            LocalDate scheduledDate = projectedTransaction.getScheduledProjectionDate();
            String scheduledNote = projectedTransaction.getScheduledNote();

            projectedTransaction.setProperty( R.string.amount_tag, scheduledAmount );
            projectedTransaction.setProperty( R.string.date_tag, scheduledDate );
            projectedTransaction.setProperty( R.string.note_tag, scheduledNote );

            return storage.writeProjection( projectedTransaction );
        }
        else return false;
    }

    ArrayList<T> getProjections(Transaction transaction) {
        //this will take the transaction and return a list of all projected transactions to the cutoff date
        //list will generate projection when one is not found in storage, will load stored projection if found

        ArrayList<T> projections = new ArrayList<>(  );
        String recurrance = (String) transaction.getProperty( R.string.recurrence_tag );

        if(recurrance == null || recurrance.contentEquals( "0-0-0-0-0-0-0" )) projections.add( (T)transaction );
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
                indexDate = getNextProjectedDate(transaction.getProperty( R.string.recurrence_tag ).toString(), indexDate);
            }

        }

        return projections;
    }

    LocalDate getCutoffDate() {
        LocalDate currentDate = LocalDate.now();
        int periodsToProject = (int) getSettingsValue( R.string.setting_value_periods_to_project );
        return currentDate.with( MonthDay.of( currentDate.getMonth(), currentDate.lengthOfMonth() ) ).plusMonths( periodsToProject );
    }

    LocalDate getNextProjectedDate(String recurrenceString, LocalDate indexDate) {
        String[] recurrenceGroups = recurrenceString.split( "-" );
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
                    tempNextDate = tempNextDate.plusDays( 1 );
                }
                while( weekCount > 1) {
                    tempNextDate = tempNextDate.plusWeeks( 1 );
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

}