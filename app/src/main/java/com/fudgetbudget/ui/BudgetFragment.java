package com.fudgetbudget.ui;

import android.os.Bundle;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.os.HandlerCompat;
import androidx.fragment.app.Fragment;

import com.fudgetbudget.FudgetBudgetViewModel;
import com.fudgetbudget.R;
import com.fudgetbudget.model.Transaction;

import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class BudgetFragment extends Fragment {

    private FudgetBudgetViewModel mViewModel;
    private MutableLiveData<List<LocalDate>> vProjectionPeriods;
    private MutableLiveData<Double> vCurrentBalance;
    private MutableLiveData<Double> vExpendableFunds;
    private MutableLiveData<Double> vBalanceThreshold;
    private MutableLiveData<Integer> vPeriodsToProject;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(savedInstanceState != null) {
            vProjectionPeriods = new MutableLiveData<>(Arrays.asList((Arrays.asList(savedInstanceState.getStringArray("PROJECTION_PERIODS"))).stream().map( period -> LocalDate.parse(period, DateTimeFormatter.BASIC_ISO_DATE)).toArray(LocalDate[]::new)));
            vCurrentBalance = new MutableLiveData<>(Double.parseDouble(savedInstanceState.getString("CURRENT_BALANCE")));
            vExpendableFunds = new MutableLiveData<>(0.0);
            vBalanceThreshold = new MutableLiveData<>(0.0);
            vPeriodsToProject = new MutableLiveData<>(0);
        }
        else {
            vProjectionPeriods = new MutableLiveData<>(new ArrayList<>());
            vCurrentBalance = new MutableLiveData<>(0.0);
            vExpendableFunds = new MutableLiveData<>(0.0);
            vBalanceThreshold = new MutableLiveData<>(0.0);
            vPeriodsToProject = new MutableLiveData<>(0);

        }


    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View budgetFragView = inflater.inflate( R.layout.fragment_budget, container, false );

        Button resetModifiedProjections = budgetFragView.findViewById( R.id.tool_reset_projections );
        resetModifiedProjections.setOnClickListener( v -> {
            List<Transaction> transactions = mViewModel.getTransactions().getValue();
            transactions.forEach( transaction -> {
                List<LocalDate> projectionPeriods = mViewModel.getProjectedPeriods().getValue();
                projectionPeriods.stream().map( period -> mViewModel.getProjectionDataKeys(period).getValue() )
                        .forEach( periodKeySet -> periodKeySet.forEach( key -> {
                            mViewModel.reset(mViewModel.getProjection(key).getValue());
                        }));
            } );

        } );

        SeekBar projectionFrequencySlider = budgetFragView.findViewById( R.id.tool_update_period_number );
        projectionFrequencySlider.setOnSeekBarChangeListener( new SeekBar.OnSeekBarChangeListener(){

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                TextView displayNumber = budgetFragView.findViewById( R.id.tool_current_period_number );
                displayNumber.setText(String.valueOf( progress ));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                String projectionPeriods = String.valueOf( seekBar.getProgress() );
                String[] projectionPeriodsSetting = new String[]{"projection_periods_to_project", projectionPeriods};

                mViewModel.update( projectionPeriodsSetting );
            }
        } );

        Button thresholdSetButton = budgetFragView.findViewById( R.id.tool_update_threshold_button );
        thresholdSetButton.setOnClickListener( v -> {
            String state = thresholdSetButton.getText().toString();
            EditText thresholdEditor = budgetFragView.findViewById( R.id.balance_threshold_value );

            if(state.contentEquals( "Modify" )){
                thresholdEditor.setEnabled( true );
                thresholdSetButton.setText("Save");
            }
            else {
                String[] settingKeyValue = new String[]{"balance_threshold", thresholdEditor.getText().toString()};
                mViewModel.update( settingKeyValue );

                thresholdEditor.setEnabled( false );
                thresholdSetButton.setText("Modify");
            }


        } );

        SwitchCompat setNewBalance = budgetFragView.findViewById( R.id.reset_current_balance_toggle );
        setNewBalance.setOnCheckedChangeListener( ((buttonView, isChecked) -> {
            if(isChecked){
                budgetFragView.findViewById( R.id.balance_budget_new_current_balance_label ).setVisibility( View.VISIBLE );
                budgetFragView.findViewById( R.id.balance_budget_new_current_balance ).setVisibility( View.VISIBLE );
            }
            else {
                budgetFragView.findViewById( R.id.balance_budget_new_current_balance_label ).setVisibility( View.INVISIBLE );
                budgetFragView.findViewById( R.id.balance_budget_new_current_balance ).setVisibility( View.INVISIBLE );
            }
        }));

        Button updateBalancesheetButton = budgetFragView.findViewById( R.id.page_tools_update_balancesheet );
        updateBalancesheetButton.setOnClickListener( v -> {
//            boolean deleteTransactions, getNewBalance, includeCurrentDue;
//
//            SwitchCompat deletePastDueTransactionsSwitch = tools_page.findViewById( R.id.page_tools_update_balancesheet_delete_pastdue_toggle );
//            deleteTransactions = deletePastDueTransactionsSwitch.isChecked();
//
//            SwitchCompat setNewBalanceSwitch = tools_page.findViewById( R.id.reset_current_balance_toggle );
//            getNewBalance = setNewBalanceSwitch.isChecked();
//
//            SwitchCompat currentDueTransactionsSwitch = tools_page.findViewById( R.id.page_tools_update_balancesheet_include_current_due_toggle );
//            includeCurrentDue = currentDueTransactionsSwitch.isChecked();
//
//            ArrayList<T> overdueTransactions = mViewModel.getOverdueTransactions(includeCurrentDue);
//
//            if(deleteTransactions) overdueTransactions.forEach( transaction -> mViewModel.delete( transaction ) );
//            else overdueTransactions.forEach( transaction -> mViewModel.reconcile( transaction ) );
//
//            if(getNewBalance){
//                EditText newBalanceView = tools_page.findViewById( R.id.balance_budget_new_current_balance );
//                String balanceString = newBalanceView.getText().toString();
//                Double balance;
//                if(balanceString.substring( 0, 1 ).contentEquals( "$" )) balance = Double.parseDouble( balanceString.substring( 1 ) );
//                else balance = Double.parseDouble( balanceString );
//
//                Double calculatedBalance = mViewModel.getCurrentBalance();
//                Double fudgedRecordBalance = balance - calculatedBalance;
//
//                Transaction fudgedTransaction;
//                if(fudgedRecordBalance > 0) {
//                    fudgedTransaction = Transaction.getInstance(R.string.transaction_type_income, getArguments().getString( "FILE_PATH" ));
//                    fudgedTransaction.setProperty( R.string.amount_tag, fudgedRecordBalance );
//                }
//                else if(fudgedRecordBalance < 0) {
//                    fudgedRecordBalance = -fudgedRecordBalance;
//                    fudgedTransaction = Transaction.getInstance(R.string.transaction_type_expense, getArguments().getString( "FILE_PATH" ));
//                    fudgedTransaction.setProperty( R.string.amount_tag, fudgedRecordBalance );
//                }
//                else return;
//
//                String fudgedRecordNote = "User updated balance set to: " + formatUtility.formatCurrency( balance);
//                fudgedRecordNote += "\nCalculated balance was: " + formatUtility.formatCurrency( calculatedBalance );
//
//                if(includeCurrentDue) fudgedRecordNote += "\nTransactions due today were included in fudged record";
//                else fudgedRecordNote += "\nTransactions due today were not included in fudged record";
//                if(deleteTransactions) fudgedRecordNote += "\nPastdue transactions were deleted prior to calculations";
//                else fudgedRecordNote += "\nPastdue transactions were recorded prior to calculations";
//
//
//                fudgedTransaction.setProperty( R.string.label_tag, "Fudged balance record" );
//                fudgedTransaction.setProperty( R.string.note_tag, fudgedRecordNote);
//                fudgedTransaction.setProperty( R.string.date_tag, LocalDate.now() );
//
//                mViewModel.update( fudgedTransaction );
//                mViewModel.reconcile( fudgedTransaction );
//            }
        } );

        return budgetFragView;
    }


    @Override
    public void onResume() {
        super.onResume();
        View budgetFragView = getView();

        Executors.newSingleThreadExecutor().execute( () -> {
            FudgetBudgetViewModel model = new ViewModelProvider(requireActivity()).get(FudgetBudgetViewModel.class);
            HandlerCompat.createAsync(Looper.getMainLooper()).post( () -> {
                mViewModel = model;
                mViewModel.getProjectedPeriods().observe(getViewLifecycleOwner(), periods -> vProjectionPeriods.postValue(periods) );
                mViewModel.getCurrentBalance().observe(getViewLifecycleOwner(),  balance -> vCurrentBalance.postValue(balance));
                mViewModel.getExpendableFunds().observe(getViewLifecycleOwner(), expendable -> vExpendableFunds.postValue(expendable));
                mViewModel.getSettingsObject(R.string.setting_value_balance_threshold).observe(getViewLifecycleOwner(), threshold -> vBalanceThreshold.postValue((Double)threshold));
                mViewModel.getSettingsObject(R.string.setting_value_periods_to_project).observe(getViewLifecycleOwner(), periodsToProject -> vPeriodsToProject.postValue(Integer.valueOf(periodsToProject.toString())));

            } );
        });

        vProjectionPeriods.observeForever( periods -> {
            ViewGroup projectionList = budgetFragView.findViewById(R.id.period_projection_list);

            FragmentManager fm = getChildFragmentManager();

            projectionList.removeAllViews();
            periods.sort(LocalDate::compareTo);
            periods.forEach( period -> fm.beginTransaction().add(R.id.period_projection_list, PeriodFragment.getInstance(period, "PROJECTIONS")).commitNow() );
        } );
        vCurrentBalance.observeForever( balance -> {
            ((TextView)budgetFragView.findViewById(R.id.current_recorded_balance)).setText("$" + formatUtility.formatCurrency(balance));
        });
        vExpendableFunds.observeForever( expendable -> {
            ((TextView)budgetFragView.findViewById(R.id.current_expendable_funds)).setText("$" + formatUtility.formatCurrency(expendable));
        });
        vBalanceThreshold.observeForever( threshold -> {
            ((TextView)budgetFragView.findViewById(R.id.balance_threshold_value)).setText(formatUtility.formatCurrency(threshold));
        });

        vPeriodsToProject.observeForever( periodsToProject -> {
            ((SeekBar)budgetFragView.findViewById(R.id.tool_update_period_number)).setProgress(periodsToProject);
            ((TextView)budgetFragView.findViewById( R.id.tool_current_period_number )).setText(String.valueOf(periodsToProject));
        });
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putStringArray("PROJECTION_PERIODS", vProjectionPeriods.getValue().stream().map(period ->
                period.format(DateTimeFormatter.BASIC_ISO_DATE)).toArray(String[]::new));

        outState.putString("CURRENT_BALANCE", vCurrentBalance.getValue().toString());

    }

    @Override
    public void onPause() {
        super.onPause();
        getView().setVisibility(View.INVISIBLE);
    }

}
