package com.fudgetbudget.ui;

import androidx.appcompat.widget.SwitchCompat;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

import com.fudgetbudget.FudgetBudgetViewModel;
import com.fudgetbudget.R;
import com.fudgetbudget.model.Transaction;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ToolsFragment<T extends Transaction> extends Fragment {

    private FudgetBudgetViewModel<T> mViewModel;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach( context );

        mViewModel = new ViewModelProvider(requireActivity()).get( FudgetBudgetViewModel.class );
    }



    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View tools_page = inflater.inflate( R.layout.fragment_tools, container, false );
//
//        //initialize handlers toggling update balancesheet view and its tools
//        Button toggleUpdateBalancesheet = tools_page.findViewById( R.id.page_tools_update_balancesheet_button );
//        toggleUpdateBalancesheet.setOnClickListener( v -> {
//            ViewGroup updateBalancesheetTools = tools_page.findViewById( R.id.page_tools_update_balancesheet_layout );
//
//            if(updateBalancesheetTools.getVisibility() != View.VISIBLE) updateBalancesheetTools.setVisibility( View.VISIBLE );
//            else updateBalancesheetTools.setVisibility( View.GONE );
//        } );
//
//        SwitchCompat setNewBalance = tools_page.findViewById( R.id.reset_current_balance_toggle );
//        setNewBalance.setOnCheckedChangeListener( ((buttonView, isChecked) -> {
//            if(isChecked){
//                tools_page.findViewById( R.id.balance_budget_new_current_balance_label ).setVisibility( View.VISIBLE );
//                tools_page.findViewById( R.id.balance_budget_new_current_balance ).setVisibility( View.VISIBLE );
//            }
//            else {
//                tools_page.findViewById( R.id.balance_budget_new_current_balance_label ).setVisibility( View.INVISIBLE );
//                tools_page.findViewById( R.id.balance_budget_new_current_balance ).setVisibility( View.INVISIBLE );
//            }
//        }));
//
//        Button updateBalancesheetButton = tools_page.findViewById( R.id.page_tools_update_balancesheet );
//        updateBalancesheetButton.setOnClickListener( v -> {
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
//
//
//
//
//        } );
//
//
//
//        //initialize handlers toggling projection settings view and its tools
//        Button toggleProjectionsSettings = tools_page.findViewById( R.id.page_tools_projections_settings_button );
//        toggleProjectionsSettings.setOnClickListener( v -> {
//            ViewGroup projectionSettingsTools = tools_page.findViewById( R.id.page_tools_projections_settings_layout );
//
//            if(projectionSettingsTools.getVisibility() != View.VISIBLE) projectionSettingsTools.setVisibility( View.VISIBLE );
//            else projectionSettingsTools.setVisibility( View.GONE );
//        } );
//
//        SeekBar projectionFrequencySlider = tools_page.findViewById( R.id.projection_frequency_value );
//        projectionFrequencySlider.setOnSeekBarChangeListener( new SeekBar.OnSeekBarChangeListener(){
//
//            @Override
//            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
//                TextView displayNumber = tools_page.findViewById( R.id.number_periods_to_project );
//                displayNumber.setText(String.valueOf( progress ));
//            }
//
//            @Override
//            public void onStartTrackingTouch(SeekBar seekBar) {}
//
//            @Override
//            public void onStopTrackingTouch(SeekBar seekBar) {
//                String projectionPeriods = String.valueOf( seekBar.getProgress() );
//                String[] projectionPeriodsSetting = new String[]{"projection_periods_to_project", projectionPeriods};
//
//                mViewModel.update( projectionPeriodsSetting );
//            }
//        } );
//
//        Button thresholdSetButton = tools_page.findViewById( R.id.balance_threshold_set_button );
//        thresholdSetButton.setOnClickListener( v -> {
//            String state = thresholdSetButton.getText().toString();
//            EditText thresholdEditor = tools_page.findViewById( R.id.balance_threshold_value );
//
//            if(state.contentEquals( "Modify" )){
//                thresholdEditor.setEnabled( true );
//                thresholdSetButton.setText("Save");
//            }
//            else {
//                String[] settingKeyValue = new String[]{"balance_threshold", thresholdEditor.getText().toString()};
//                mViewModel.update( settingKeyValue );
//
//                thresholdEditor.setEnabled( false );
//                thresholdSetButton.setText("Modify");
//            }
//
//
//        } );
//
//
//        //initialize handlers toggling data operations view and its tools
//        Button toggleDataOperations = tools_page.findViewById( R.id.page_tools_batch_transaction_operations_button );
//        toggleDataOperations.setOnClickListener( v -> {
//            ViewGroup dataOperationsTools = tools_page.findViewById( R.id.page_tools_batch_transaction_operations_layout );
//
//            if(dataOperationsTools.getVisibility() != View.VISIBLE) dataOperationsTools.setVisibility( View.VISIBLE );
//            else dataOperationsTools.setVisibility( View.GONE );
//        } );
//
//        Button resetModifiedProjections = tools_page.findViewById( R.id.batch_operation_reset_repeating );
//        resetModifiedProjections.setOnClickListener( v -> {
////            ArrayList<T> allTransactions = new ArrayList<>();
////            allTransactions.addAll(mViewModel.getTransactions(R.string.transaction_type_income));
////            allTransactions.addAll(mViewModel.getTransactions(R.string.transaction_type_expense));
//
//            Map<Integer, List<Transaction>> transactions = mViewModel.getCachedTransactions().getValue();
//            transactions.values().forEach( transactionList -> {
//                transactionList.forEach( transaction -> {
//                    ArrayList<T> allTransactionProjections = mViewModel.getProjections( (Transaction) transaction );
//                    allTransactionProjections.forEach( projection -> mViewModel.reset( projection ) );
//                });
//            } );
//
//        } );
//
//        Button deleteRecords = tools_page.findViewById( R.id.batch_operation_delete_records );
//        deleteRecords.setOnClickListener( v -> {
//            ArrayList<T> allRecords = mViewModel.getRecords( R.string.records_type_all );
//            allRecords.forEach( recordedTransaction -> mViewModel.delete( recordedTransaction ) );
//
//
//        } );
//
//        Button deleteTransactions = tools_page.findViewById( R.id.batch_operation_delete_transactions );
//        deleteTransactions.setOnClickListener( v -> {
//
//            ArrayList<Transaction> allTransactions = new ArrayList<>();
//            Map<Integer, List<Transaction>> transactions = mViewModel.getCachedTransactions().getValue();
//            transactions.values().forEach( transactionList -> transactionList.forEach( transaction -> allTransactions.add(transaction)));
//            allTransactions.forEach( transaction -> mViewModel.delete( transaction ) );
//        } );
//
//
//        //initialize handlers for canIAffordItTool
//        Button affordItButton = tools_page.findViewById( R.id.page_tools_afford_operations_button );
//        affordItButton.setOnClickListener( v -> {
//            ViewGroup affortItTools = tools_page.findViewById( R.id.page_tools_afford_operations_layout );
//
//            if(affortItTools.getVisibility() != View.VISIBLE) affortItTools.setVisibility( View.VISIBLE );
//            else affortItTools.setVisibility( View.GONE );
//        } );
//        //this tool should make use of the existing transaction editor layout to get user input
//        //maybe use a empty transaction list with an add button
//        //items in list should have a remove button
//        //updates to an item are confirmed and a result is automatically recalculated using all list items
//
//
////        TransitionInflater transitionInflater = TransitionInflater.from(requireContext());
////        setEnterTransition(transitionInflater.inflateTransition( android.R.transition.slide_bottom ));


        return tools_page;
    }

    @Override
    public void onResume() {
        super.onResume();

        View pageTools = getActivity().findViewById( R.id.page_tools );
        int periodsToProject = (int) mViewModel.getSettingsValue( R.string.setting_value_periods_to_project );


        SeekBar projectionFrequencySlider = pageTools.findViewById( R.id.projection_frequency_value );
        projectionFrequencySlider.setProgress( periodsToProject );

        TextView periodsToProjectView = pageTools.findViewById( R.id.number_periods_to_project );
        periodsToProjectView.setText( String.valueOf(periodsToProject) );

        EditText thresholdValueEditor = pageTools.findViewById( R.id.balance_threshold_value );
        thresholdValueEditor.setText( formatUtility.formatCurrency(mViewModel.getSettingsValue( R.string.setting_value_balance_threshold )));



    }
}
