package com.fudgetbudget;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.fudgetbudget.model.ProjectedTransaction;
import com.fudgetbudget.model.RecordedTransaction;
import com.fudgetbudget.model.Transaction;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class FudgetBudgetViewModel extends ViewModel {
    private final FudgetBudgetRepo mModelRepo;

    private MutableLiveData<Double> cLowestProjectedBalance;
    private MutableLiveData<Double> cExpendableFunds;
    private MutableLiveData<Double> cCurrentBalance;
    private MutableLiveData<Double> cBalanceThreshold;
    private MutableLiveData<Object> cNumProjectionPeriods;

    private MutableLiveData<List<Transaction>> mTransactions;
    private MutableLiveData<List<LocalDate>> mProjectedPeriods;
    private Map<LocalDate, MutableLiveData<LinkedList<String>>> mProjectedTransactionKeysByPeriod;
    private Map<String, Map.Entry<MutableLiveData<ProjectedTransaction>, MutableLiveData<Double>>> mProjectedTransactionWithBalance;
    private MutableLiveData<List<LocalDate>> mRecordPeriods;
    private Map<LocalDate, MutableLiveData<LinkedList<String>>> mRecordsKeysByPeriod;
    private Map<String, MutableLiveData<RecordedTransaction>> mRecordsByKey;


    public FudgetBudgetViewModel(FudgetBudgetRepo repo){
        mModelRepo = repo;
        cLowestProjectedBalance = new MutableLiveData<>(mModelRepo.getCurrentBalance().getValue());

        mModelRepo.getTransactions().observeForever( transactions -> {
            if(mTransactions == null) mTransactions = new MutableLiveData<>();
            mTransactions.postValue(transactions);
        });
        mModelRepo.getProjectionMap().observeForever( projectionMap -> {
            if(mProjectedTransactionWithBalance == null) mProjectedTransactionWithBalance = new HashMap<>();
            if(mProjectedTransactionKeysByPeriod == null) mProjectedTransactionKeysByPeriod = new HashMap<>();
            if(mProjectedPeriods == null) mProjectedPeriods = new MutableLiveData<>(new ArrayList<>());

            //there is something wrong here regarding rebuilding mProjected* objects.
            final Double[] indexBalance = {mModelRepo.getCurrentBalance().getValue(), mModelRepo.getCurrentBalance().getValue()};
            projectionMap.forEach( (period, periodProjections) -> {
                if(!mProjectedPeriods.getValue().contains(period)){ //the period is not already included
                    List<LocalDate> periods = mProjectedPeriods.getValue();
                    periods.add(period);
                    Collections.sort(periodProjections);
                    LinkedList<String> periodProjectionKeys = new LinkedList<>(Arrays.asList(periodProjections.stream().sorted((cur, next) -> cur.compareTo(next)).map( projection -> projection.getId().toString() + ":" + projection.getScheduledProjectionDate().format(DateTimeFormatter.BASIC_ISO_DATE)).toArray(String[]::new)));

                    mProjectedTransactionKeysByPeriod.put(period, new MutableLiveData<>(periodProjectionKeys));
                    mProjectedPeriods.postValue(periods);
                }
                else { //the period is included
                    Collections.sort(periodProjections);
                    LinkedList<String> periodProjectionKeys = new LinkedList<>(Arrays.asList(periodProjections.stream().map( projection -> projection.getId().toString() + ":" + projection.getScheduledProjectionDate().format(DateTimeFormatter.BASIC_ISO_DATE)).toArray(String[]::new)));
                    if(!mProjectedTransactionKeysByPeriod.get(period).getValue().containsAll(periodProjectionKeys)
                            || !periodProjectionKeys.containsAll(mProjectedTransactionKeysByPeriod.get(period).getValue())){
                        mProjectedTransactionKeysByPeriod.get(period).postValue(periodProjectionKeys);
                    }
                }

                periodProjections.forEach( projectedTransaction -> {
                    String key = projectedTransaction.getId().toString()+":"+projectedTransaction.getScheduledProjectionDate().format(DateTimeFormatter.BASIC_ISO_DATE);

                    if(projectedTransaction.getIncomeFlag()) indexBalance[0] += (Double)projectedTransaction.getProperty(R.string.amount_tag);
                    else indexBalance[0] -= (Double) projectedTransaction.getProperty(R.string.amount_tag);

                    if(indexBalance[0] < indexBalance[1]) indexBalance[1] = indexBalance[0];

                    if(mProjectedTransactionWithBalance.get(key) == null) mProjectedTransactionWithBalance.put(key, new AbstractMap.SimpleEntry<>(new MutableLiveData<>(projectedTransaction), new MutableLiveData<>(indexBalance[0])));
                    if(mProjectedTransactionWithBalance.get(key).getKey().getValue().compareTo(projectedTransaction) != 0) mProjectedTransactionWithBalance.get(key).getKey().postValue(projectedTransaction);
                    if(mProjectedTransactionWithBalance.get(key).getValue().getValue() != indexBalance[0]) mProjectedTransactionWithBalance.get(key).getValue().postValue(indexBalance[0]);

                });
                if(indexBalance[1] < cLowestProjectedBalance.getValue()) cLowestProjectedBalance.postValue(indexBalance[1]);
            });
        });
        mModelRepo.getRecordsMap().observeForever( recordsMap -> {
            if(mRecordPeriods == null) mRecordPeriods = new MutableLiveData<>(new ArrayList<>());
            if(mRecordsKeysByPeriod == null) mRecordsKeysByPeriod = new HashMap<>();
            if(mRecordsByKey == null) mRecordsByKey = new HashMap<>();

            if(recordsMap.isEmpty()){
                mRecordPeriods.postValue(new ArrayList<>());
                mRecordsKeysByPeriod = new HashMap<>();
                mRecordsByKey = new HashMap<>();
            }
            else recordsMap.forEach( (period, periodRecords) -> {
                if(!mRecordPeriods.getValue().contains(period)) {
                    List<LocalDate> periods = mRecordPeriods.getValue();
                    periods.add(period);
                    mRecordPeriods.postValue(periods);
                }
                if(mRecordsKeysByPeriod.get(period) == null) mRecordsKeysByPeriod.put(period, new MutableLiveData<>(new LinkedList<>()));

                Collections.sort(periodRecords);
                Collections.reverse(periodRecords);
                LinkedList<String> recordKeys = new LinkedList<>();

                for(int i = 0; i < periodRecords.size(); ++i) recordKeys.add(periodRecords.get(i).getRecordId().toString());

//                if(!recordKeys.containsAll(mRecordsKeysByPeriod.get(period).getValue()) || !mRecordsKeysByPeriod.get(period).getValue().containsAll(recordKeys))
//                mRecordsKeysByPeriod.get(period).postValue(new LinkedList<>(recordKeys));

                if(recordKeys.size() == mRecordsKeysByPeriod.get(period).getValue().size()){
                    int index = 0;
                    boolean updateFlag = false;
                    while(!updateFlag && index < recordKeys.size()){
                        String newKey = recordKeys.get(index);
                        String oldKey = mRecordsKeysByPeriod.get(period).getValue().get(index);
                        if(!newKey.contentEquals(oldKey)) updateFlag = true;
                        ++index;
                    }
                    if(updateFlag) mRecordsKeysByPeriod.get(period).postValue(recordKeys);
                } else mRecordsKeysByPeriod.get(period).postValue(recordKeys);


                periodRecords.forEach( record -> {
                    if(mRecordsByKey.get(record.getRecordId().toString()) == null) mRecordsByKey.put(record.getRecordId().toString(), new MutableLiveData<>(record));
                    else if(mRecordsByKey.get(record.getRecordId().toString()).getValue() == null) mRecordsByKey.replace(record.getRecordId().toString(), new MutableLiveData<>(record));
                    else {
                        RecordedTransaction storedRecord = mRecordsByKey.get(record.getRecordId().toString()).getValue();
                        if(storedRecord.compareTo(record) != 0) mRecordsByKey.get(record.getRecordId().toString()).postValue(record);
                    }
                });
            });
        });
        mModelRepo.getSettingsObject(R.string.setting_value_balance_threshold).observeForever( threshold -> {
            if(cBalanceThreshold == null) cBalanceThreshold = new MutableLiveData<>();
            cBalanceThreshold.postValue(Double.parseDouble(threshold.toString()));
        });
        mModelRepo.getSettingsObject(R.string.setting_value_periods_to_project).observeForever( periodsToProject -> {
            if(cNumProjectionPeriods == null) cNumProjectionPeriods = new MutableLiveData<>();
            cNumProjectionPeriods.postValue(periodsToProject);
        });
        mModelRepo.getCurrentBalance().observeForever( balance -> {
            if(cCurrentBalance == null) cCurrentBalance = new MutableLiveData<>(0.0);
            cCurrentBalance.postValue(balance);
        });

    }

    public LiveData<List<Transaction>> getTransactions(){ return mTransactions; }
    public LiveData<List<LocalDate>> getProjectedPeriods(){ return mProjectedPeriods; }
    public LiveData<LinkedList<String>> getProjectionDataKeys(LocalDate periodDate){ return mProjectedTransactionKeysByPeriod.get(periodDate); }
    public LiveData<ProjectedTransaction> getProjection(String projectionKey){ return mProjectedTransactionWithBalance.get(projectionKey).getKey(); }
    public LiveData<Double> getProjectedBalance(String projectionKey){ return mProjectedTransactionWithBalance.get(projectionKey).getValue(); }
    public LiveData<List<LocalDate>> getRecordPeriods() { return mRecordPeriods; }
    public LiveData<LinkedList<String>> getRecordDataKeys(LocalDate periodDate){ return mRecordsKeysByPeriod.get(periodDate); }
    public LiveData<RecordedTransaction> getRecord(String recordKey) { return mRecordsByKey.get(recordKey); }
    public LiveData<Double> getCurrentBalance() { return mModelRepo.getCurrentBalance(); }
    public LiveData<?> getSettingsObject(int resourceId){
        switch (resourceId) {
            case R.string.setting_value_balance_threshold: return cBalanceThreshold;
            case R.string.setting_value_periods_to_project: return cNumProjectionPeriods;
            default: return null;
        }
    }

    public LiveData<Double> getExpendableFunds(){
        if(cExpendableFunds == null) cExpendableFunds = new MutableLiveData<>(cCurrentBalance.getValue());

        cLowestProjectedBalance.observeForever( lowBal -> {
            if( lowBal <= cBalanceThreshold.getValue() ) cExpendableFunds.postValue(0.0);
            else cExpendableFunds.postValue(lowBal - cBalanceThreshold.getValue());
        });

        return cExpendableFunds;
    }

    public boolean update(Object object) { return mModelRepo.update(object); }
    public boolean delete(Object object) { return mModelRepo.delete(object); }
    public boolean reset(Object object) { return mModelRepo.reset(object); }
    public boolean reconcile(Object object){ return mModelRepo.reconcile( object ); }


}

