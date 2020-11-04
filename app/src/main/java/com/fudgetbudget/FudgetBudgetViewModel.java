package com.fudgetbudget;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.fudgetbudget.model.ProjectedTransaction;
import com.fudgetbudget.model.RecordedTransaction;
import com.fudgetbudget.model.Transaction;

import java.time.LocalDate;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class FudgetBudgetViewModel<T extends Transaction> extends ViewModel {
    private final FudgetBudgetRepo<T> mModelRepo;

    private final MutableLiveData<Double> cBalanceThreshold;
    private final MutableLiveData<Integer> cNumProjectionPeriods;

    private MutableLiveData<Map<UUID, Transaction>> cCachedTransactionsById;
    private MutableLiveData<Map<UUID, List<T>>> cCachedProjectionsById;
    private MutableLiveData<Map<LocalDate, List<T>>> cCachedProjectionsByPeriod;
    private MutableLiveData<Map<LocalDate, List<RecordedTransaction>>> cCachedRecordsByPeriod;

    private MutableLiveData<List<LocalDate>> mProjectedPeriods;
    private Map<LocalDate, MutableLiveData<LinkedList<String>>> mProjectedTransactionKeysByPeriod;
    private Map<String, Map.Entry<MutableLiveData<T>, MutableLiveData<Double>>> mProjectedTransactionWithBalance;

    private MutableLiveData<List<LocalDate>> mRecordPeriods;
    private Map<LocalDate, MutableLiveData<List<String>>> mRecordKeysByPeriod;
    private Map<String, MutableLiveData<T>> mRecordsByKey;



    public FudgetBudgetViewModel(FudgetBudgetRepo repo){
        mModelRepo = repo;

        //observable cached objects initialized with data read from the repo
        //after that, methods update/delete/reconcile/reset, will post changes to these cached values
        //      the goal is that the viewmodel shouldn't have to reread and rebuild the entire data model once it's initialized
        //changes to cached values are responsible for modifying the model values as needed
        //implementors of viewmodel will only access the model values, which are also observable
        //      when model values are changed, their observer will call its observe callback and change view as needed


        Map<UUID, Transaction> cachedTransactions = new HashMap<>();
        mModelRepo.getTransactions().getValue().forEach( transaction -> cachedTransactions.put(transaction.getId(), transaction));

        Map<UUID, List<T>> cachedProjections = new HashMap<>();
        cachedTransactions.forEach( (id, transaction) -> cachedProjections.put(id, mModelRepo.getProjectionsByTransactionId(id).getValue()));

        Map<LocalDate, List<T>> cachedProjectionsByPeriod = new HashMap<>();
        cachedProjections.forEach( (id, projectionList) -> {
            projectionList.forEach( projection -> {
                LocalDate projectionPeriod = ((LocalDate) projection.getProperty(R.string.date_tag)).withDayOfMonth(1);
                if(cachedProjectionsByPeriod.get(projectionPeriod) == null) cachedProjectionsByPeriod.put(projectionPeriod, new ArrayList<>());
                List<T> periodProjections = cachedProjectionsByPeriod.get(projectionPeriod);
                periodProjections.add(projection);
                cachedProjectionsByPeriod.replace(projectionPeriod, periodProjections);
            });
        });

        Map<LocalDate, List<RecordedTransaction>> cachedRecords = new HashMap<>();
        mModelRepo.getRecords().getValue().forEach( record -> {
            LocalDate recordPeriod = ((LocalDate)record.getProperty(R.string.date_tag)).withDayOfMonth(1);
            if(cachedRecords.get( recordPeriod ) == null) cachedRecords.put(recordPeriod, new ArrayList<>());
            List<RecordedTransaction> periodRecords = cachedRecords.get(recordPeriod);
            periodRecords.add(record);
            cachedRecords.replace(recordPeriod, periodRecords);
        });

        cCachedTransactionsById = new MutableLiveData<>(cachedTransactions);
        cCachedProjectionsById = new MutableLiveData<>(cachedProjections);
        cCachedProjectionsByPeriod = new MutableLiveData<>(cachedProjectionsByPeriod);
        cCachedRecordsByPeriod = new MutableLiveData<>(cachedRecords);

        cBalanceThreshold = new MutableLiveData<>((Double)mModelRepo.getSettingsObject(R.string.setting_value_balance_threshold).getValue());
        cNumProjectionPeriods = new MutableLiveData<>((Integer)mModelRepo.getSettingsObject(R.string.setting_value_periods_to_project).getValue());

        cCachedTransactionsById.observeForever( transactionMap -> {
            //post changes to model data as needed

            //the map contains all transactions with their id as the key
            //isolate transactions which have been modified

            //new transactions will need their projections added to the projection cache
            //removed transactions will need their projections removed from the projection cache
            //
        });
        cCachedProjectionsById.observeForever( projectionMap -> {
            //post changes to model data as needed

        });
        cCachedProjectionsByPeriod.observeForever( projectionPeriodMap -> {
            //post changes to model data as needed

        });
        cCachedRecordsByPeriod.observeForever( recordsPeriodMap -> {
            //post changes to model data as needed

        });
        cBalanceThreshold.observeForever( threshold -> {
            //post changes to model data as needed

        });
        cNumProjectionPeriods.observeForever( numPeriodsToProject -> {
            //post changes to model data as needed

        });

    }



//    private Map<Integer, List<Transaction>> cacheTransactions() {
////        ArrayList<Transaction> transactions = new ArrayList<Transaction>(mModelRepo.getActiveTransactions());
//        List<Transaction> income = (List<Transaction>) mModelRepo.getTransactionsByType(R.string.transaction_type_income).getValue();
//        List<Transaction> expense = (List<Transaction>) mModelRepo.getTransactionsByType(R.string.transaction_type_expense).getValue();
//        Map<Integer, List<Transaction>> cache = new HashMap<>();
//
//        cache.put(R.string.transaction_type_expense, expense);
//        cache.put(R.string.transaction_type_income, income);
//        mCachedTransactionsByType.postValue(cache);
//        mCachedTransactionsByType.observeForever( transactionMap -> mExecutor.execute(() -> buildProjectionData(transactionMap)));
//        return cache;
//    }
//    private Map<LocalDate, List<RecordedTransaction>> cacheRecords() {
//        List<RecordedTransaction> allRecords = mModelRepo.getRecordedTransactions();
//        Map<LocalDate, List<RecordedTransaction>> recordsByPeriod = new HashMap<>();
//        Map<String, MutableLiveData<RecordedTransaction>> recordsByKey = new HashMap<>();
//
//        allRecords.forEach( record -> {
//            LocalDate recordPeriodDate = ((LocalDate) record.getProperty(R.string.date_tag)).withDayOfMonth(1);
//            List<RecordedTransaction> periodRecords = recordsByPeriod.get(recordPeriodDate);
//
//            if(periodRecords == null) {
//                periodRecords = new ArrayList<>();
//                periodRecords.add(record);
//                recordsByPeriod.put( recordPeriodDate, periodRecords );
//            }
//            else {
//                periodRecords.add(record);
//                recordsByPeriod.replace( recordPeriodDate, periodRecords );
//            }
//
//            mRecordPeriods.postValue(Arrays.asList(recordsByPeriod.keySet().stream().toArray(LocalDate[]::new)));
//
//        });
//
//        recordsByPeriod.forEach( (period, recordList) -> {
//            MutableLiveData<List<String>> periodKeys = mRecordKeysByPeriod.get(period);
//            if(periodKeys == null) periodKeys = new MutableLiveData<>(new ArrayList<>());
//
//        });
//
//
//        return recordsByPeriod;
//    }


    public LiveData<Map<UUID, Transaction>> getCachedTransactions(){ return cCachedTransactionsById; }
    public LiveData<Map<LocalDate, List<RecordedTransaction>>> getCachedRecords(){ return cCachedRecordsByPeriod; }


    private void buildProjectionData(Map<Integer, List<Transaction>> transactionMap){

        //get all the transactions into a list
        List<Transaction> transactionList = new ArrayList<>();
        transactionMap.values().forEach(transactionList::addAll);

        //project the transactions and sort by period into a map
        Map<LocalDate, List<T>> projectionMap = new HashMap<>();
        transactionList.stream().map(transaction -> mModelRepo.getProjectionsByTransactionId(transaction.getId()).getValue()).forEach( projectionList ->
            projectionList.forEach( projection -> {
                        LocalDate periodDate = ((LocalDate) projection.getProperty(R.string.date_tag)).withDayOfMonth(1);
                        List<T> periodProjections = projectionMap.get(periodDate);

                        if(periodProjections == null) {
                            periodProjections = new ArrayList<>();
                            periodProjections.add((T) projection);
                            projectionMap.put(periodDate, periodProjections);
                        }else {
                            periodProjections.add((T) projection);
                            projectionMap.replace(periodDate, periodProjections);
                        }

                    }));


        //projectionMap should now contain all the projections in a map sorted by period
        //  if cachedPeriods contains periods not in projectionMap, they should be removed
        //  if projection map contains periods not in cachedPeriods, they should be added to cachedPeriods
        //post cachedPeriods to mProjectedPeriods
        List<LocalDate> cachedPeriods = mProjectedPeriods.getValue();
        cachedPeriods.removeIf( period -> projectionMap.get(period) == null );
        projectionMap.forEach( (period, periodProjections) -> { if(!cachedPeriods.contains(period)) cachedPeriods.add(period); });
        if(!cachedPeriods.containsAll(mProjectedPeriods.getValue())
                || !mProjectedPeriods.getValue().containsAll(cachedPeriods))
            mProjectedPeriods.postValue(cachedPeriods);


        //cachedPeriods now contains only the periods to be projected
        //  if cachedKeys contains periods not in cachedPeriods, they should be removed
        //  if cachedPeriods contains periods not in cachedKeys, they should be added to cachedKeys
        //now cachedKeys should contain the same periods as cachedPeriods
        //  for each period in cachedKeys we need to update the keyList with the projections from projectionMap
        //      if cachedKeyList of the period contains a key not in the projectionKeyList, it should be removed from cachedKeyList
        //      if projectionKeyList contains a key not found in cachedKeyList, it should be added to cachedKeylist
        Map<LocalDate, MutableLiveData<LinkedList<String>>> cachedKeys = mProjectedTransactionKeysByPeriod;
        List<LocalDate> removeCachedKeys = new ArrayList<>();
        cachedPeriods.forEach( period -> { if(cachedKeys.get(period) == null) cachedKeys.put(period, new MutableLiveData<>(new LinkedList<>())); });
        cachedKeys.forEach( (period, keyList) -> { if( !cachedPeriods.contains(period) ) removeCachedKeys.add(period); });
        removeCachedKeys.forEach( staleKey -> cachedKeys.remove(staleKey));

        cachedKeys.forEach( (period, keyList) -> {
            List<T> periodProjections = projectionMap.get(period);
            Collections.sort(periodProjections);
            LinkedList<String> projectionKeyList = new LinkedList<>(Arrays.asList(periodProjections.stream().map( projection -> {
                String keyString = projection.getId().toString();
                if(projection instanceof ProjectedTransaction)
                    keyString += ":" + ((ProjectedTransaction)projection).getScheduledProjectionDate().toString();
                return keyString;
            }).toArray(String[]::new)));
            LinkedList<String> cachedKeyList = keyList.getValue();
            List<String> removeFromCache = new ArrayList<>();
            cachedKeyList.forEach( cachedKey -> { if(!projectionKeyList.contains(cachedKey)) removeFromCache.add(cachedKey); });
            projectionKeyList.forEach( projectionKey -> { if(!cachedKeyList.contains(projectionKey)) cachedKeyList.add(projectionKey); });
            removeFromCache.forEach( key -> cachedKeyList.remove(key));
//                Collections.sort( cachedKeyList, (key, next) -> {
//                    String[] keySplit = key.split(":");
//                    String[] nextSplit = next.split(":");
//
//                    if( keySplit.length != 2 && nextSplit.length != 2 ) return
//                });
            if(!cachedKeys.get(period).getValue().containsAll(cachedKeyList)
                    || !cachedKeyList.containsAll(cachedKeys.get(period).getValue()))
                cachedKeys.get(period).postValue(cachedKeyList);

        });
        mProjectedTransactionKeysByPeriod = cachedKeys;




        //cachedKeys now contain only the projectionKeys for the current build with periods matching cachedPeriods
        //now I need to go through the cachedProjectionsWithBalances and update cachedProjections and corresponding cachedBalances
        //  duplicate cachedProjectionsWithBalance
        //  go through entire set of cachedKeys, in order by period
        //  for each cachedKey:
        //      if the cachedProjectionWithBalance is not found, add entry to cachedProjectionsWithBalance
        //      if the cachedProjectionWithBalance is found remove it from the duplicate
        //          if the cachedProjection is not equivalent to projection, post updated projection
        //          if the cachedBalance is not the same as indexBalance, post updated balance
        //  whatever remaining in duplicate should be removed from cachedProjectionsWithBalance
        Map<String, Map.Entry<MutableLiveData<T>, MutableLiveData<Double>>> cachedProjectionsWithBalance = mProjectedTransactionWithBalance;
        List<String> keyHolder = new ArrayList<>(cachedProjectionsWithBalance.keySet());
        Double indexBalance = getCurrentBalance();

        for(Map.Entry<LocalDate, MutableLiveData<LinkedList<String>>> keyPeriodEntry : cachedKeys.entrySet()){
            LocalDate period = keyPeriodEntry.getKey();
            LinkedList<String> keys = keyPeriodEntry.getValue().getValue();
            for(String key : keys){
                T projection = projectionMap.get(period).stream().filter( proj -> {
                    String stringKey = proj.getId().toString();
                    if(proj instanceof ProjectedTransaction) stringKey += ":" + ((ProjectedTransaction)proj).getScheduledProjectionDate().toString();
                    return stringKey.contentEquals(key);
                }).findFirst().get();

                double balance;
                if(projection.getIncomeFlag()) balance = indexBalance + (Double)projection.getProperty(R.string.amount_tag);
                else balance = indexBalance - (Double) projection.getProperty(R.string.amount_tag);
                indexBalance = balance;

                Map.Entry<MutableLiveData<T>, MutableLiveData<Double>> cachedEntry = cachedProjectionsWithBalance.get(key);
                if(cachedEntry == null){
                    //entry not found, need to create and add to cachedProjectionsWIthBalance
                    Map.Entry<MutableLiveData<T>, MutableLiveData<Double>> projectionEntry = new AbstractMap.SimpleEntry<>(new MutableLiveData<>(projection), new MutableLiveData<>(balance));
                    cachedProjectionsWithBalance.put(key, projectionEntry);
                }
                else {
                    //engtry found, need to check projection and balance for possible updates
                    MutableLiveData<T> cachedProjection = cachedEntry.getKey();
                    MutableLiveData<Double> cachedBalance = cachedEntry.getValue();


                    if(!cachedProjection.equals(projection)) cachedProjection.postValue(projection);
                    if(!cachedBalance.getValue().equals(balance)) cachedBalance.postValue(balance);
                }
                keyHolder.remove(key);
            }
        }
        keyHolder.forEach(cachedProjectionsWithBalance::remove);
        mProjectedTransactionWithBalance = cachedProjectionsWithBalance;

    }


    //methods for accessing projection model data by view implementing viewmodel
    public LiveData<List<LocalDate>> getProjectedPeriods(){
        return mProjectedPeriods;
    }
    public LiveData<LinkedList<String>> getProjectionDataKeys(LocalDate periodDate){
        return mProjectedTransactionKeysByPeriod.get(periodDate);
    }
    public LiveData<T> getProjection(String projectionKey){
        Map.Entry<MutableLiveData<T>, MutableLiveData<Double>> foundProjection = mProjectedTransactionWithBalance.get(projectionKey);
        if(foundProjection == null) return null;
        return foundProjection.getKey();
    }
    public LiveData<Double> getProjectedBalance(String projectionKey){
        return mProjectedTransactionWithBalance.get(projectionKey).getValue();
    }



    //methods for accessing record model data by view implementing viewmodel
    public LiveData<List<LocalDate>> getRecordPeriods() { return mRecordPeriods; }
    public LiveData<List<String>> getRecordPeriodKeys(LocalDate period) { return mRecordKeysByPeriod.get(period); }
    public LiveData<T> getRecord(String key) { return mRecordsByKey.get(key); }



    //methods for modifying stored data and resulting models: first performs the storage op, then updates the cached models
    public boolean update(Object object) {
        if(mModelRepo.update( object )){
            //post changes to cached values as needed

            return true;
        }
        return false;
    }
    public boolean delete(Object object) {
        if(mModelRepo.delete( object )){
            //post changes to cached values as needed

            return true;
        }
        return false;
    }
    public boolean reset(Object object) {
        if(mModelRepo.reset( object )){
            //post changes to cached values as needed

            return true;
        }
        return false;
    }
    public boolean reconcile(Object object){
        if(mModelRepo.reconcile( object )){
            //post changes to cached values as needed

            return true;
        }
        return false;
    }



    public Double getCurrentBalance() { return mModelRepo.getCurrentBalance().getValue(); }
    public Object getSettingsObject(int resourceId){ return mModelRepo.getSettingsObject(resourceId).getValue(); }

}

