package com.fudgetbudget.ui;

import android.os.Bundle;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.os.HandlerCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;

import com.fudgetbudget.FudgetBudgetViewModel;
import com.fudgetbudget.R;
import com.fudgetbudget.model.Transaction;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.concurrent.Executors;

public class PeriodFragment<T extends Transaction> extends Fragment {

    private FudgetBudgetViewModel<T> mViewModel;
    private MutableLiveData<LinkedList<String>> vProjectionKeys;
    private LocalDate vPeriod;

    public static PeriodFragment getInstance(LocalDate periodDate){
        PeriodFragment fragment = new PeriodFragment();
        Bundle bundle = new Bundle();
        bundle.putString("PERIOD_DATE", periodDate.toString());
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        vPeriod = LocalDate.parse(getArguments().getString("PERIOD_DATE"));

        if(savedInstanceState != null) vProjectionKeys = new MutableLiveData<>(new LinkedList<>(savedInstanceState.getStringArrayList("PERIOD_KEYS")));
        else vProjectionKeys = new MutableLiveData<>(new LinkedList<>());


        Executors.newSingleThreadExecutor().execute(() -> {
            FudgetBudgetViewModel<T> model = new ViewModelProvider(requireActivity()).get(FudgetBudgetViewModel.class);
            LinkedList<String> periodKeys = model.getProjectionDataKeys(vPeriod).getValue();

            HandlerCompat.createAsync(Looper.getMainLooper()).post(() -> {
                mViewModel = model;
                vProjectionKeys.postValue(periodKeys);

                mViewModel.getProjectionDataKeys(vPeriod).observe(getViewLifecycleOwner(), projectionKeys -> vProjectionKeys.postValue(projectionKeys));

            });
        });


    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View periodView = inflater.inflate(R.layout.period_item_projected, null);

        TextView periodText = periodView.findViewById(R.id.projection_period_header_date);
        periodText.setText( vPeriod.format( DateTimeFormatter.ofPattern("MMM yyyy")) );

        if( mViewModel != null){
            mViewModel.getProjectionDataKeys(vPeriod).observe(getViewLifecycleOwner(), projectionKeys -> vProjectionKeys.postValue(projectionKeys));
        }
        vProjectionKeys.observe(getViewLifecycleOwner(), periodKeys -> {
            FragmentManager fm = getChildFragmentManager();
            FragmentTransaction ft = fm.beginTransaction();


            periodKeys.forEach( key -> {
                if(fm.findFragmentByTag(key) == null){
                    ProjectionFragment newFragment = ProjectionFragment.getInstance(key);
                    ft.add(R.id.period_transactions, newFragment, key);
                }
            });

            fm.getFragments().forEach( fragment -> {
                if(!periodKeys.contains(fragment.getTag())) ft.remove(fragment);
            });

            ft.commit();
        });


        return periodView;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putStringArrayList("PERIOD_KEYS", new ArrayList(vProjectionKeys.getValue()));
    }
}
