package com.fudgetbudget;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.fudgetbudget.model.ProjectedTransaction;
import com.fudgetbudget.model.RecordedTransaction;
import com.fudgetbudget.model.Transaction;

import java.time.LocalDate;
import java.time.MonthDay;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class FudgetBudgetRepo {
    private final StorageControl storage;

    private MutableLiveData<List<Transaction>> cachedTransactions;
    private MutableLiveData<Map<UUID, List<ProjectedTransaction>>> cachedProjections;
    private MutableLiveData<Map<UUID, List<RecordedTransaction>>> cachedRecords;
    private Map<Integer, MutableLiveData<Object>> cachedSettings;
    private MutableLiveData<Double> cachedBalance;

    private MutableLiveData<LinkedHashMap<LocalDate, List<ProjectedTransaction>>> mProjectionsByPeriod;
    private MutableLiveData<Map<LocalDate, List<RecordedTransaction>>> mRecordsByPeriod;

    public FudgetBudgetRepo(String filesDir) {
        storage = new StorageControl( filesDir );

        cachedSettings = new HashMap<>();
        cachedSettings.put(R.string.setting_value_periods_to_project, new MutableLiveData<>(storage.readSettingsValue("projection_periods_to_project")));
        cachedSettings.put(R.string.setting_value_balance_threshold, new MutableLiveData<>(storage.readSettingsValue("balance_threshold")));

        ArrayList<Transaction> storedTransactions = new ArrayList<>();
        storedTransactions.addAll( storage.readTransactions(R.string.transaction_type_expense));
        storedTransactions.addAll( storage.readTransactions(R.string.transaction_type_income));
        cachedTransactions = new MutableLiveData<>(storedTransactions);

        Map<UUID, List<ProjectedTransaction>> storedProjections = new HashMap<>();
        storedTransactions.forEach( transaction -> storedProjections.put(transaction.getId(), createProjectionsList(transaction)));
        cachedProjections = new MutableLiveData<>(storedProjections);

        Map<UUID, List<RecordedTransaction>> storedRecords = new HashMap<>();
        storage.readRecords(R.string.records_type_all).forEach( record -> {
            if(storedRecords.get(record.getId()) == null) storedRecords.put(record.getId(), new ArrayList<>());

            List<RecordedTransaction> transactionRecords = storedRecords.get(record.getId());
            transactionRecords.add(record);
            storedRecords.replace(record.getId(), transactionRecords);
        });
        cachedRecords = new MutableLiveData<>(storedRecords);

        final Double[] calcultatedBalance = {0.0};
        storedRecords.values().forEach( transactionRecords -> transactionRecords.forEach( record -> {
            if(record.getIncomeFlag()) calcultatedBalance[0] += (Double)record.getProperty(R.string.amount_tag);
            else calcultatedBalance[0] -= (Double)record.getProperty(R.string.amount_tag);
        }));
        cachedBalance = new MutableLiveData<>(calcultatedBalance[0]);


        //changes to transactions will require changing the cachedProjections
        cachedTransactions.observeForever( transactions -> {
            Map<UUID, List<ProjectedTransaction>> projectionCache = cachedProjections.getValue();
            transactions.forEach( transaction -> {
                if(projectionCache.get(transaction.getId()) == null) projectionCache.put(transaction.getId(), createProjectionsList(transaction));
                else projectionCache.replace(transaction.getId(), createProjectionsList(transaction));
            });
            cachedProjections.postValue(projectionCache);
        });

        //changes to cachedProjections will require rebuilding the mProjectionMap
        cachedProjections.observeForever( projectionMap -> {
            if (mProjectionsByPeriod == null) mProjectionsByPeriod = new MutableLiveData<>(new LinkedHashMap<>());

            HashMap<LocalDate, List<ProjectedTransaction>> projections = new HashMap<>();

            projectionMap.values().forEach( projectionSet -> projectionSet.forEach( projectedTransaction -> {
                LocalDate projectionPeriod = ((LocalDate)projectedTransaction.getProperty(R.string.date_tag)).withDayOfMonth(1);

                if(!projections.containsKey(projectionPeriod)) projections.put(projectionPeriod, null);
                List<ProjectedTransaction> periodProjections = projections.get(projectionPeriod);
                if(periodProjections == null) periodProjections = new ArrayList<>();
                periodProjections.add(projectedTransaction);
                projections.replace(projectionPeriod, periodProjections);

            }));
            LinkedHashMap<LocalDate, List<ProjectedTransaction>> sortedProjections = new LinkedHashMap<>(projections.size());
            projections.keySet().stream().sorted( (cur, next) -> cur.compareTo(next)).forEach( period -> {
                List<ProjectedTransaction> projectedTransactions = projections.get(period);
                projectedTransactions.sort( (cur, next) -> ((LocalDate)cur.getProperty(R.string.date_tag)).compareTo(((LocalDate)next.getProperty(R.string.date_tag))));

                sortedProjections.put(period, projectedTransactions);
            });

            mProjectionsByPeriod.postValue(sortedProjections);
        });

        //changes to cachedRecords will require rebuilding the mRecordMap
        cachedRecords.observeForever( recordsMap -> {
            if(mRecordsByPeriod == null) mRecordsByPeriod = new MutableLiveData<>(new HashMap());

            Map<LocalDate, List<RecordedTransaction>> recordsByPeriod = new HashMap<>();
            recordsMap.values().forEach( recordsSet -> recordsSet.forEach( record -> {
                LocalDate recordsPeriod = ((LocalDate)record.getProperty(R.string.date_tag)).withDayOfMonth(1);

                if(!recordsByPeriod.containsKey(recordsPeriod)) recordsByPeriod.put(recordsPeriod, null);

                List<RecordedTransaction> periodRecords = recordsByPeriod.get(recordsPeriod);
                if(periodRecords == null) periodRecords = new ArrayList<>();

                periodRecords.add(record);
                Collections.sort(periodRecords);
                recordsByPeriod.replace(recordsPeriod, periodRecords);
            }));
            mRecordsByPeriod.postValue(recordsByPeriod);
        });

        //changes to cachedBalance shouldn't effect anything in the repository
        cachedBalance.observeForever( balance -> {

        });
        cachedSettings.get(R.string.setting_value_periods_to_project).observeForever( periodsToProject -> {
            Map<UUID, List<ProjectedTransaction>> cache = new HashMap<>();
            cachedTransactions.getValue().forEach( transaction -> {
                List<ProjectedTransaction> updatedProjections = createProjectionsList(transaction);
                cache.put(transaction.getId(), updatedProjections);
            });
            cachedProjections.postValue(cache);
        });
        cachedSettings.get(R.string.setting_value_balance_threshold).observeForever( balanceThreshold -> {
            System.out.println("stop");
            //changes to this value will require projections to reset their balance appearance as needed
        });
    }

    public LiveData<List<Transaction>> getTransactions(){ return cachedTransactions; }
    public LiveData<LinkedHashMap<LocalDate, List<ProjectedTransaction>>> getProjectionMap(){ return mProjectionsByPeriod; }
    public LiveData<Map<LocalDate, List<RecordedTransaction>>> getRecordsMap(){ return mRecordsByPeriod; }
    public LiveData<Object> getSettingsObject(int type){ return cachedSettings.get(type); }
    public LiveData<Double> getCurrentBalance(){ return cachedBalance; }

    private ArrayList<ProjectedTransaction> createProjectionsList(Transaction transaction) {
        ArrayList<ProjectedTransaction> projections = new ArrayList<>(  );
        String recurrance = (String) transaction.getProperty( R.string.recurrence_tag );

        if(recurrance != null && !recurrance.contentEquals( "0-0-0-0-0-0-0" )) {
            LocalDate indexDate = (LocalDate) transaction.getProperty( R.string.date_tag );
            LinkedList<ProjectedTransaction> storedProjectionList = storage.readProjections(transaction);
            LinkedHashMap<LocalDate, ProjectedTransaction> storedProjections = new LinkedHashMap<>();

            storedProjectionList.forEach( projection -> storedProjections.put(projection.getScheduledProjectionDate(), projection));


            if(storedProjections.size() > 0){
                LocalDate firstStoredProjectionDate = (LocalDate) storedProjections.keySet().toArray()[0];
                if(firstStoredProjectionDate.isBefore( indexDate )) indexDate = firstStoredProjectionDate;
            }

            LocalDate cuttoffDate = LocalDate.now();
            int periodsToProject = Integer.parseInt(getSettingsObject( R.string.setting_value_periods_to_project ).getValue().toString());
            cuttoffDate = cuttoffDate.with( MonthDay.of( cuttoffDate.getMonth(), cuttoffDate.lengthOfMonth() ) ).plusMonths( periodsToProject );

            while(indexDate != null && !indexDate.isAfter( cuttoffDate )){
                ProjectedTransaction projection = storedProjections.get( indexDate );

                if(projection == null) projection = ProjectedTransaction.getInstance( transaction, indexDate );
                projections.add( projection );
                indexDate = getNextProjectedDate(transaction.getProperty( R.string.recurrence_tag ).toString(), indexDate);
            }

        }
        else projections.add( ProjectedTransaction.getInstance(transaction, (LocalDate)transaction.getProperty(R.string.date_tag)) );

        return projections;
    }
    private LocalDate getNextProjectedDate(String recurrenceString, LocalDate indexDate) {
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


    public boolean update(Object object){
        if(object instanceof ProjectedTransaction) {
            if (storage.writeProjection( (ProjectedTransaction) object )){
                List<ProjectedTransaction> projections = Arrays.asList(cachedProjections.getValue().get(((ProjectedTransaction)object).getId()).stream().map(projectedTransaction -> {
                    if(projectedTransaction.getScheduledProjectionDate().isEqual(((ProjectedTransaction)object).getScheduledProjectionDate())){
                        if(projectedTransaction.compareTo((ProjectedTransaction)object) == 0) return projectedTransaction;
                        else return (ProjectedTransaction)object;
                    }
                    return projectedTransaction;
                }).toArray(ProjectedTransaction[]::new));
                projections.sort( (cur, next) -> ((LocalDate)cur.getProperty(R.string.date_tag)).compareTo(((LocalDate)next.getProperty(R.string.date_tag))));
                Map<UUID, List<ProjectedTransaction>> projectionMap = cachedProjections.getValue();
                projectionMap.replace(((ProjectedTransaction) object).getId(), projections);
                cachedProjections.postValue(projectionMap);

                return true;
            } else return false;
        }
        else if(object instanceof RecordedTransaction) {
            if(((LocalDate)((RecordedTransaction) object).getProperty(R.string.date_tag)).isAfter(LocalDate.now())) {
                return false;
            }
            if(storage.writeRecord( (RecordedTransaction) object )){
                Map<UUID, List<RecordedTransaction>> recordsCache = cachedRecords.getValue();
                List<RecordedTransaction> transactionRecords = recordsCache.get(((RecordedTransaction) object).getId());

                if(transactionRecords.contains((RecordedTransaction)object)) transactionRecords.remove(object);
                transactionRecords.add((RecordedTransaction)object);

                Collections.sort(transactionRecords);
                recordsCache.replace(((RecordedTransaction) object).getId(), transactionRecords);
                cachedRecords.postValue(recordsCache);
                return true;
            } else return false;
        }
        else if(object instanceof Transaction) {
            Transaction transaction = (Transaction) object;
            boolean clearProjections = transaction.getClearStoredProjectionsFlag();
            boolean deleteFailed = false;

            if(clearProjections) {
                for( ProjectedTransaction projection : storage.readProjections( transaction ) ){
                    if (!storage.deleteProjection( projection )) deleteFailed = true;
                }
            }
            if(deleteFailed) return false;
            else if (storage.writeTransaction( (Transaction) object )){
                List<Transaction> transactions = cachedTransactions.getValue();
                if(transactions.contains((Transaction)object)) transactions.remove((Transaction)object);
                transactions.add((Transaction)object);
                Collections.sort(transactions);
                cachedTransactions.postValue(transactions);
                return true;
            } else return false;
        }
        else if(object instanceof String[] && ((String[])object).length == 2){
            String key = ((String[])object)[0];
            String value = ((String[])object)[1];
            if(value.substring( 0, 1 ).contentEquals( "$" )) value = value.substring(1);
            if(storage.writeSettingsValue( key, value )){
                Object cachedValue;
                if(key.contentEquals("projection_periods_to_project")) {
                    cachedValue = cachedSettings.get(R.string.setting_value_periods_to_project).getValue();
                    if(!cachedValue.toString().contentEquals(value)) cachedSettings.get(R.string.setting_value_periods_to_project).postValue(value);
                }
                else if(key.contentEquals("balance_threshold")) {
                    cachedValue = cachedSettings.get(R.string.setting_value_balance_threshold).getValue();
                    if(!cachedValue.toString().contentEquals(value)) cachedSettings.get(R.string.setting_value_balance_threshold).postValue(value);
                }

                return true;
            } else return false;

        }
        else return false;
    }
    public boolean reconcile(Object object){
        if(object instanceof ProjectedTransaction){
            ProjectedTransaction projectedTransaction = (ProjectedTransaction) object;
            RecordedTransaction record = RecordedTransaction.getInstance(projectedTransaction);
            Transaction transaction = storage.readTransactions( record ).get( 0 );

            LocalDate newDate = getNextProjectedDate( transaction.getProperty( R.string.recurrence_tag ).toString(), ((LocalDate)transaction.getProperty( R.string.date_tag )));
            if(newDate != null) transaction.setProperty( R.string.date_tag, newDate);
            else {
                transaction.setProperty(R.string.date_tag, "");
                List<Transaction> transactions = cachedTransactions.getValue();
                transactions.remove(transaction);
                cachedTransactions.postValue(transactions);
            }

            storage.deleteProjection( projectedTransaction );
            storage.writeRecord( record );
            storage.writeTransaction( transaction );

            Map<UUID, List<ProjectedTransaction>> cache = cachedProjections.getValue();
            List<ProjectedTransaction> projections = cache.get(projectedTransaction.getId());
            projections.remove(projectedTransaction);
            cache.replace(projectedTransaction.getId(), projections);
            cachedProjections.postValue(cache);

            Map<UUID, List<RecordedTransaction>> cached = cachedRecords.getValue();
            List<RecordedTransaction> records = cached.get(projectedTransaction.getId());
            if(records == null) {
                records = new ArrayList<>();
                cached.put(projectedTransaction.getId(), records);
            }
            records.add(record);
            cached.replace(projectedTransaction.getId(), records);
            cachedRecords.postValue(cached);

            Double balance = cachedBalance.getValue();
            if(projectedTransaction.getIncomeFlag()) balance += Double.parseDouble(projectedTransaction.getProperty(R.string.amount_tag).toString());
            else balance -= Double.parseDouble(projectedTransaction.getProperty(R.string.amount_tag).toString());
            cachedBalance.postValue(balance);

            return true;
        }
        else return false;
    }
    public boolean delete(Object object){
        if(object instanceof RecordedTransaction) {
            if(storage.deleteRecord((RecordedTransaction) object)){
                Map<UUID, List<RecordedTransaction>> cache = cachedRecords.getValue();
                List<RecordedTransaction> records = cache.get(((RecordedTransaction) object).getId());
                records.remove((RecordedTransaction)object);
                cache.replace(((RecordedTransaction)object).getId(), records);
                cachedRecords.postValue(cache);

                Double balace = cachedBalance.getValue();
                if(((RecordedTransaction)object).getIncomeFlag()) balace -= Double.parseDouble(((RecordedTransaction) object).getProperty(R.string.amount_tag).toString());
                else balace += Double.parseDouble(((RecordedTransaction)object).getProperty(R.string.amount_tag).toString());
                cachedBalance.postValue(balace);


                return true;
            } else return false;
        }
        else if(object instanceof Transaction) {
            Transaction transaction = (Transaction) object;

            storage.readProjections( transaction ).forEach( projection -> {
                if(projection instanceof ProjectedTransaction)
                    storage.deleteProjection( (ProjectedTransaction)projection );
            } );
            transaction.setProperty( R.string.date_tag, "" );

            if(storage.writeTransaction( transaction )){
                List<Transaction> transactions = cachedTransactions.getValue();
                transactions.remove(transaction);
                cachedTransactions.postValue(transactions);

                Map<UUID, List<ProjectedTransaction>> projections = cachedProjections.getValue();
                projections.remove(transaction.getId());
                cachedProjections.postValue(projections);

                return true;
            } else return false;
        }
        else if(object instanceof Integer && ((Integer)object) == R.string.transaction_type_all){
            cachedTransactions.getValue().forEach( transaction -> storage.deleteTransaction(transaction));
            cachedTransactions.postValue(new ArrayList<>());
            return true;
        }
        else return false;
    }
    public boolean reset(Object object){
        if(object instanceof ProjectedTransaction) {
            ProjectedTransaction projectedTransaction = (ProjectedTransaction) object;

            projectedTransaction.setProperty( R.string.amount_tag, projectedTransaction.getScheduledAmount() );
            projectedTransaction.setProperty( R.string.date_tag, projectedTransaction.getScheduledProjectionDate() );
            projectedTransaction.setProperty( R.string.note_tag, projectedTransaction.getScheduledNote() );

            if(storage.writeProjection( projectedTransaction )){
                Map<UUID, List<ProjectedTransaction>> cache = cachedProjections.getValue();
                List<ProjectedTransaction> projections = cache.get(projectedTransaction.getId());
                if(projections.contains(projectedTransaction)) projections.remove(projectedTransaction);
                projections.add(projectedTransaction);
                Collections.sort(projections);
                cache.replace(projectedTransaction.getId(), projections);
                cachedProjections.postValue(cache);

                return true;
            } else return false;
        }
        else if(object instanceof Transaction){
            Transaction transaction = (Transaction)object;
            Map<UUID, List<ProjectedTransaction>> cache = cachedProjections.getValue();
            List<ProjectedTransaction> projections = cache.get(transaction.getId());
            projections.forEach( projectedTransaction -> {
                projectedTransaction.setProperty( R.string.amount_tag, projectedTransaction.getScheduledAmount() );
                projectedTransaction.setProperty( R.string.date_tag, projectedTransaction.getScheduledProjectionDate() );
                projectedTransaction.setProperty( R.string.note_tag, projectedTransaction.getScheduledNote() );

                storage.writeProjection(projectedTransaction);
            });
            Collections.sort(projections);
            cache.replace(transaction.getId(), projections);
            cachedProjections.postValue(cache);
            return true;
        }
        else return false;
    }

}