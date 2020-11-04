package com.fudgetbudget.ui;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.core.os.HandlerCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.fudgetbudget.FudgetBudgetRepo;
import com.fudgetbudget.R;
import com.fudgetbudget.model.RecordedTransaction;
import com.fudgetbudget.model.Transaction;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RecordsViewModel<T extends Transaction> extends ViewModel {

    private final String mDirectory;
    private final FudgetBudgetRepo<T> mModelRepo;
    private final ExecutorService mExecutor;
    private final Handler mMainThreadHandler;
    private final MutableLiveData<List<LocalDate>> mRecordPeriods;
    private final Map<LocalDate, MutableLiveData<List<String>>> mPeriodKeys;

    public RecordsViewModel(Context context) {
        mDirectory = context.getExternalFilesDir(null).getPath();
        mModelRepo = new FudgetBudgetRepo<T>(mDirectory);
        mExecutor = Executors.newSingleThreadExecutor();
        mMainThreadHandler = HandlerCompat.createAsync(Looper.getMainLooper());

        mRecordPeriods = new MutableLiveData<>();
        mPeriodKeys = new HashMap<>();

//        mExecutor.execute( () -> {
//            List<RecordedTransaction> allRecords = mModelRepo.getRecordedTransactions();
//            Map<LocalDate, List<RecordedTransaction>> recordsByPeriod = new HashMap<>();
//
//            allRecords.forEach( record -> {
//                LocalDate recordPeriodDate = ((LocalDate) record.getProperty(R.string.date_tag)).withDayOfMonth(1);
//                List<RecordedTransaction> periodRecords = recordsByPeriod.get(recordPeriodDate);
//
//                if(periodRecords == null) {
//                    periodRecords = new ArrayList<>();
//                    periodRecords.add(record);
//                    recordsByPeriod.put( recordPeriodDate, periodRecords );
//                }
//                else {
//                    periodRecords.add(record);
//                    recordsByPeriod.replace( recordPeriodDate, periodRecords );
//                }
//
//                mRecordPeriods.postValue(Arrays.asList(recordsByPeriod.keySet().stream().toArray(LocalDate[]::new)));
//
//            });
//
//            recordsByPeriod.forEach( (period, recordList) -> {
//                MutableLiveData<List<String>> periodKeys = mPeriodKeys.get(period);
//                if(periodKeys == null) periodKeys = new MutableLiveData<>(new ArrayList<>());
//
//            });
//
//        });

    }

    public LiveData<List<LocalDate>> getPeriods() {
        List<LocalDate> periods = new ArrayList<>();

        return mRecordPeriods;
    }

    public LiveData<Double> getCurrentBalance() {
        return mModelRepo.getCurrentBalance();
    }
}
