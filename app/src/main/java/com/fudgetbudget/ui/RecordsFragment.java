package com.fudgetbudget.ui;

import androidx.core.os.HandlerCompat;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.fudgetbudget.FudgetBudgetViewModel;
import com.fudgetbudget.R;
import com.fudgetbudget.model.RecordedTransaction;
import com.fudgetbudget.model.Transaction;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

public class RecordsFragment extends Fragment {

    private FudgetBudgetViewModel mViewModel;
    private MutableLiveData<List<LocalDate>> vRecordPeriods;
    private MutableLiveData<Double> vCurrentBalance;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(savedInstanceState != null){
            vRecordPeriods = new MutableLiveData<>(Arrays.asList(Arrays.asList(savedInstanceState.getStringArray("RECORD_PERIODS"))
                    .stream().map( period -> LocalDate.parse(period, DateTimeFormatter.BASIC_ISO_DATE)).toArray(LocalDate[]::new)));

            vCurrentBalance = new MutableLiveData<>( Double.parseDouble(savedInstanceState.getString("CURRENT_BALANCE") ));

        }
        else {
            vRecordPeriods = new MutableLiveData<>(new ArrayList<>());
            vCurrentBalance = new MutableLiveData<>(0.0);
        }

        Executors.newSingleThreadExecutor().execute(() -> {
            FudgetBudgetViewModel model = new ViewModelProvider(requireActivity()).get(FudgetBudgetViewModel.class);
            HandlerCompat.createAsync(Looper.getMainLooper()).post( () -> {
                mViewModel = model;
                mViewModel.getRecordPeriods().observeForever( periods -> vRecordPeriods.postValue(periods));
                mViewModel.getCurrentBalance().observeForever( balance -> vCurrentBalance.postValue(balance));
            });
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View recordsPage = inflater.inflate( R.layout.fragment_records, container, false );


        Button deleteRecords = recordsPage.findViewById( R.id.tool_delete_records );
        deleteRecords.setOnClickListener( v -> {
            mViewModel.getRecordPeriods().getValue().stream()
                    .map( period -> mViewModel.getRecordDataKeys(period).getValue())
                    .forEach( keySet -> keySet.forEach( key -> mViewModel.delete(mViewModel.getRecord(key).getValue())));
        } );

        return recordsPage;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        vRecordPeriods.observe( getViewLifecycleOwner(), periods -> {
            ViewGroup recordList = requireView().findViewById(R.id.record_list);

            FragmentManager fm = getChildFragmentManager();

            recordList.removeAllViews();
            periods.sort(LocalDate::compareTo);
            Collections.reverse(periods);
            periods.forEach( period -> fm.beginTransaction().add(R.id.record_list, PeriodFragment.getInstance(period, "RECORDS")).commitNow() );

        });
        vCurrentBalance.observe( getViewLifecycleOwner(), newBalance -> {
            TextView currentBalanceView = view.findViewById( R.id.current_recorded_balance );
            currentBalanceView.setText( "$" + formatUtility.formatCurrency( newBalance ));
        });

    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        String[] savedRecordPeriods = vRecordPeriods.getValue().stream().map( period ->
                period.format(DateTimeFormatter.BASIC_ISO_DATE)).toArray(String[]::new);
        outState.putStringArray("RECORD_PERIODS", savedRecordPeriods);
        outState.putString("CURRENT_BALANCE", vCurrentBalance.getValue().toString());
    }
    @Override
    public void onPause() {
        super.onPause();
        getView().setVisibility(View.INVISIBLE);
    }

}
