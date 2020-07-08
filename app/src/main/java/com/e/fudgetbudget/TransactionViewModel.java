package com.e.fudgetbudget;

import android.app.DatePickerDialog;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.e.fudgetbudget.model.BudgetModel;
import com.e.fudgetbudget.model.ProjectedTransaction;
import com.e.fudgetbudget.model.PropertySetter;
import com.e.fudgetbudget.model.Transaction;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;


class TransactionViewModel {
    private final MainActivity context;
    private BudgetModel budgetModel;
    private ProjectionViewModel projectionViewModel;


    TransactionViewModel(MainActivity context) {
        this.context = context;
        this.budgetModel = new BudgetModel( context );
        this.projectionViewModel = new ProjectionViewModel( context );
    }

    void loadIncomeLineItems(ViewGroup list_view) {
        ArrayList<Transaction> income = budgetModel.getIncomeTransactions();
        if(income.size() <= 0) list_view.addView( TextView.inflate( this.context, R.layout.view_empty_list_message, null ) );

        else income.iterator().forEachRemaining( transaction -> {
            LinearLayout lineItem = getLineItemLayout( transaction );
            lineItem.setOnClickListener( view -> showFullDisplayDialog(transaction) );
            list_view.addView( lineItem );
        });
    }
    void loadExpenseLineItems(ViewGroup list_view) {
        ArrayList<Transaction> expenses = budgetModel.getExpenseTransactions();
        if(expenses.size() <= 0) list_view.addView( View.inflate( this.context, R.layout.view_empty_list_message, null ) );

        else expenses.iterator().forEachRemaining( transaction -> {
            LinearLayout lineItem = getLineItemLayout( transaction );
            lineItem.setOnClickListener( view -> showFullDisplayDialog( transaction ) );
            list_view.addView( lineItem );
        });
    }
    void showEditorDialog(Object object) {
        AlertDialog.Builder builder = new AlertDialog.Builder( this.context );
        Transaction transaction;
        LinearLayout editor_view;

        if(object instanceof Transaction)
            transaction = (Transaction) object ;
        else if(object instanceof Integer && (Integer)object == R.string.tag_transaction_expense)
            transaction = (Transaction)budgetModel.factory( Transaction.class, R.string.tag_transaction_expense );
        else if(object instanceof Integer && (Integer)object == R.string.tag_transaction_income)
            transaction = (Transaction) budgetModel.factory( Transaction.class, R.string.tag_transaction_income );
        else transaction = null;

        editor_view = getEditorLayout( transaction );
        TextView header = editor_view.findViewById( R.id.editor_header );
        if(transaction.getIncomeFlag()) header.setText("Income Transaction");
        else header.setText("Expense Transaction");

        builder.setView( editor_view );
        builder.setPositiveButton( "Save", (dialog, which) -> {
            budgetModel.update( getTransactionFromView( transaction, editor_view, "editor" ) );
            dialog.dismiss();
            this.context.recreate();
        });
        builder.setNegativeButton( "Close", (dialog, which) -> {
            dialog.dismiss();
        } );
        builder.show();
    }
    private void showFullDisplayDialog(Transaction transaction) {
        AlertDialog.Builder builder = new AlertDialog.Builder( this.context );
        LinearLayout fullDisplayView = getFullDisplayLayout( transaction );

        TextView header = fullDisplayView.findViewById( R.id.fulldisplay_header );
        if(transaction.getIncomeFlag()) header.setText("Income Transaction");
        else header.setText("Expense Transaction");

        builder.setView( fullDisplayView );
        builder.setPositiveButton( "Edit", (dialog, which) -> {
            dialog.dismiss();
            showEditorDialog(transaction);
        });
        builder.setNeutralButton( "Delete", (dialog, which) -> {
            budgetModel.delete( transaction );
            dialog.dismiss();
            this.context.recreate();
        } );
        builder.setNegativeButton( "Close", (dialog, which) -> dialog.dismiss() );
        builder.show();
    }

    private LinearLayout getEditorLayout(Transaction transaction){
        LinearLayout editorView = (LinearLayout) this.context.getLayoutInflater().inflate( R.layout.layout_transaction_editor, null );

        if(transaction == null) return editorView;
        editorView.setTag( R.string.id_tag, transaction.getId() );
        if(transaction.getIncomeFlag()) editorView.setTag(R.string.object_type_tag, R.string.tag_transaction_income);
        else editorView.setTag(R.string.object_type_tag, R.string.tag_transaction_income);

        ( (EditText) editorView.findViewById( R.id.label_editor ) )
                .setText(transaction.getLabel());

        Button dateEditorButton = (Button) editorView.findViewById( R.id.date_editor_button );
        LocalDate date = transaction.getScheduledDate();
        dateEditorButton.setText(date.format( DateTimeFormatter.ofPattern( "MMMM dd, yyyy" ) ));
        dateEditorButton.setTag(R.string.date_tag, date);
        dateEditorButton.setOnClickListener( view -> {
            final LocalDate[] newDate = {date};
            DatePickerDialog pickerDialog = new DatePickerDialog( this.context );
            DatePicker picker = pickerDialog.getDatePicker();
            picker.init( newDate[0].getYear(), newDate[0].getMonthValue(), newDate[0].getDayOfMonth(), (button_view, year, monthOfYear, dayOfMonth) -> {
                newDate[0] = LocalDate.of( year, monthOfYear, dayOfMonth );
            } );
            pickerDialog.setButton( DatePickerDialog.BUTTON_POSITIVE, "OK", (dialog, which)->{
                dateEditorButton.setText( newDate[0].format( DateTimeFormatter.ofPattern( "MMMM dd, yyyy" ) ));
                dateEditorButton.setTag(R.string.date_tag, newDate[0] );
                dialog.dismiss();
            } );

            pickerDialog.setContentView( picker );
            pickerDialog.show();
        });

        Button recurranceEditorButton = editorView.findViewById( R.id.recurrance_editor_button );
        String recurrenceValue = String.valueOf( transaction.getRecurrance() );
        //TODO: print recurrenceValue in a more readable form
        recurranceEditorButton.setText( recurrenceValue );
        recurranceEditorButton.setOnClickListener( view -> {
            AlertDialog.Builder builder = new AlertDialog.Builder( this.context );
            LinearLayout recurranceEditor = (LinearLayout) LinearLayout.inflate( this.context, R.layout.layout_transaction_recurrance_editor, null );

            //todo: initialize values in the recurrenceEditor from transaction.getRecurrence string value
            //  as is, recurrenceEditor always starts from a state of no recurrence

            Switch recurranceToggleSwitch = recurranceEditor.findViewById( R.id.recurring_property_toggle_switch );
            recurranceToggleSwitch.setOnClickListener( view1 -> {
                LinearLayout recurrenceParametersLayout = (LinearLayout)recurranceEditor.findViewById( R.id.set_recurrance_parameters_layout );
                if(recurrenceParametersLayout.getVisibility() == View.VISIBLE) recurrenceParametersLayout.setVisibility( View.GONE );
                else {
                    recurrenceParametersLayout.setVisibility( View.VISIBLE );
                    Spinner period = recurrenceParametersLayout.findViewById( R.id.recurrence_period_spinner );
                    String selectedPeriod = String.valueOf( period.getSelectedItem());

                    period.setOnItemSelectedListener( new AdapterView.OnItemSelectedListener(){
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            String selectedPeriod = TransactionViewModel.this.context.getResources().getStringArray( R.array.date_periods )[position];
                            LinearLayout dayTypeLayout = recurrenceParametersLayout.findViewById( R.id.on_day_type_layout );
                            Spinner dayType = dayTypeLayout.findViewById( R.id.on_day_type_selector );
                            ArrayAdapter<CharSequence> adapter = null;

                            if(selectedPeriod.contentEquals( "Week" )){
                                dayTypeLayout.setVisibility( View.VISIBLE );
                                adapter= ArrayAdapter.createFromResource(TransactionViewModel.this.context, R.array.days_of_week_options, android.R.layout.simple_list_item_1 );
                                adapter.setDropDownViewResource( android.R.layout.simple_list_item_1 );
                                dayType.setAdapter(adapter);
                            }
                            else if(selectedPeriod.contentEquals( "Month" ) || selectedPeriod.contentEquals( "Year" )){
                                dayTypeLayout.setVisibility( View.VISIBLE );
                                adapter= ArrayAdapter.createFromResource(TransactionViewModel.this.context, R.array.day_of_month_options, android.R.layout.simple_list_item_1 );
                                adapter.setDropDownViewResource( android.R.layout.simple_list_item_1 );
                                dayType.setAdapter(adapter);
                            }
                            else dayTypeLayout.setVisibility( View.INVISIBLE );
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {

                        }
                    } );

                    ArrayAdapter<CharSequence> adapter = null;
                    LinearLayout dayTypeLayout = recurrenceParametersLayout.findViewById( R.id.on_day_type_layout );
                    Spinner dayType = dayTypeLayout.findViewById( R.id.on_day_type_selector );


                    if(selectedPeriod.contentEquals( "Week" )){
                        dayTypeLayout.setVisibility( View.VISIBLE );
                        adapter= ArrayAdapter.createFromResource(this.context, R.array.days_of_week_options, android.R.layout.simple_list_item_1 );
                        adapter.setDropDownViewResource( android.R.layout.simple_list_item_1 );
                        dayType.setAdapter(adapter);
                    }
                    else if(selectedPeriod.contentEquals( "Month" ) || selectedPeriod.contentEquals( "Year" )){
                        dayTypeLayout.setVisibility( View.VISIBLE );
                        adapter= ArrayAdapter.createFromResource(this.context, R.array.day_of_month_options, android.R.layout.simple_list_item_1 );
                        adapter.setDropDownViewResource( android.R.layout.simple_list_item_1 );
                        dayType.setAdapter(adapter);
                    }
                    else dayTypeLayout.setVisibility( View.INVISIBLE );

                }
            } );

            Switch endParametersToggleSwitch = recurranceEditor.findViewById( R.id.set_end_parameters_switch );
            endParametersToggleSwitch.setOnClickListener( view1 -> {
                LinearLayout endParametersLayout = recurranceEditor.findViewById( R.id.set_end_parameters_layout );

                if(endParametersLayout.getVisibility() == View.VISIBLE) endParametersLayout.setVisibility( View.GONE );
                else {
                    endParametersLayout.setVisibility( View.VISIBLE );
                    RadioGroup options = endParametersLayout.findViewById( R.id.set_end_parameters_options );
                    EditText afterNumberOfOccurrencesValue = endParametersLayout.findViewById( R.id.after_number_of_occurrences_value );
                    Button afterDateValue = endParametersLayout.findViewById( R.id.after_date_value );
                    EditText afterTotalReachesAmountValue = endParametersLayout.findViewById( R.id.when_total_reaches_amount_value );


                    options.setOnCheckedChangeListener( ((group, checkedId) -> {
                        if(checkedId == R.id.after_number_of_occurrences){
                            afterNumberOfOccurrencesValue.setEnabled( true );
                            afterDateValue.setEnabled( false );
                            afterTotalReachesAmountValue.setEnabled( false );
                        } else if(checkedId == R.id.after_date){
                            afterNumberOfOccurrencesValue.setEnabled( false );
                            afterDateValue.setEnabled( true );
                            afterTotalReachesAmountValue.setEnabled( false );
                        } else if(checkedId == R.id.when_total_reaches_amount){
                            afterNumberOfOccurrencesValue.setEnabled( false );
                            afterDateValue.setEnabled( false );
                            afterTotalReachesAmountValue.setEnabled( true );
                        }
                    }) );
                }
            } );


            builder.setPositiveButton( "Set", (dialog, which) -> {
                //pattern isn't doing anything, but here for my reference. Ideally I'd use this to set and validate the recurrence values
                String pattern = "^(0|1)-(\\d+)-(0|Month|Week|Year|Day)-(0|DayOfMonth|WeekDayOfMonth|Monday|Tuesday|Wednesday|Thursday|Friday|Saturday|Sunday)-(0|1)-(0|StopAfterOccurrenceCount|StopAfterDate|StopAfterRecordedAmount)-(\\d+)$";
                String[] groupValues = new String[7];

                for(int i = 0; i < groupValues.length; i++){ groupValues[i] = "0"; }

                Switch recurringPropertyToggle = recurranceEditor.findViewById( R.id.recurring_property_toggle_switch );
                if(recurringPropertyToggle.isChecked()){
                    EditText frequencyValue = recurranceEditor.findViewById( R.id.recurrence_frequency_value );
                    Spinner periodSpinner = recurranceEditor.findViewById( R.id.recurrence_period_spinner );
                    Spinner onDayTypeSpinner = recurranceEditor.findViewById( R.id.on_day_type_selector );

                    groupValues[0] = "1";
                    groupValues[1] = frequencyValue.getText().toString();
                    groupValues[2] = periodSpinner.getSelectedItem().toString();
                    groupValues[3] = onDayTypeSpinner.getSelectedItem().toString();

                    Switch endParametersToggle = recurranceEditor.findViewById( R.id.set_end_parameters_switch );

                    if(endParametersToggle.isChecked()){
                        groupValues[4] = "1";

                        RadioGroup endParametersOptions = recurranceEditor.findViewById( R.id.set_end_parameters_options );
                        int checkedOptionId = endParametersOptions.getCheckedRadioButtonId();
                        if(checkedOptionId == R.id.after_number_of_occurrences) {
                            groupValues[5] = "StopAfterOccurrenceCount";
                            groupValues[6] = ((EditText) recurranceEditor.findViewById( R.id.after_number_of_occurrences_value )).getText().toString();
                        }
                        else if(checkedOptionId == R.id.after_date) {
                            groupValues[5] = "StopAfterDate";
                            groupValues[6] = ((Button) recurranceEditor.findViewById( R.id.after_date_value )).getText().toString();
                        }
                        else if(checkedOptionId == R.id.when_total_reaches_amount) {
                            groupValues[5] = "StopAfterRecordedAmount";
                            groupValues[6] = ((EditText) recurranceEditor.findViewById( R.id.when_total_reaches_amount_value )).getText().toString();
                        }
                    }
                }

                StringBuilder capturedRecurrenceValue = new StringBuilder();
                int index = 0;
                while(index < 7){
                    capturedRecurrenceValue.append( groupValues[index] ).append( "-" );
                    index++;
                }
                capturedRecurrenceValue.deleteCharAt( capturedRecurrenceValue.length()-1 );
                //TODO: print capturedRecurrenceValue in a more readable form
                recurranceEditorButton.setText(capturedRecurrenceValue);
                recurranceEditorButton.setTag(R.string.transaction_recurrence_tag, capturedRecurrenceValue.toString() );
            } );
            builder.setNegativeButton( "Cancel", ((dialog, which) -> { dialog.dismiss(); }) );
            builder.setView( recurranceEditor );
            builder.show();
        });

        ( (EditText) editorView.findViewById( R.id.amount_editor ))
                .setText(String.valueOf( transaction.getAmount() ));

        ( (EditText) editorView.findViewById( R.id.note_editor ))
                .setText( transaction.getNote() );

        return editorView;
    }
    private LinearLayout getLineItemLayout(Transaction transaction){
        LinearLayout lineItemView = (LinearLayout)LinearLayout.inflate( this.context, R.layout.layout_transaction_lineitem, null);

        TextView date = lineItemView.findViewById( R.id.date_value );
        TextView label = lineItemView.findViewById( R.id.label_value );
        TextView amount = lineItemView.findViewById( R.id.amount_value );

        date.setText(transaction.getScheduledDate().format( DateTimeFormatter.ofPattern( "MM/dd" ) ));
        label.setText( transaction.getLabel() );
        amount.setText( String.valueOf( transaction.getAmount() ));

        lineItemView.setOnClickListener( view -> showFullDisplayDialog(transaction) );


        return lineItemView;
    }
    private LinearLayout getFullDisplayLayout(Transaction transaction){
        LinearLayout displayView = (LinearLayout)LinearLayout.inflate( this.context, R.layout.layout_transaction_fulldisplay, null);

        ( (TextView) displayView.findViewById( R.id.label_value ) ).setText(transaction.getLabel());
        ( (TextView) displayView.findViewById( R.id.amount_value ) ).setText(String.valueOf(transaction.getAmount()));
        ( (TextView) displayView.findViewById( R.id.date_value ) ).setText(transaction.getScheduledDate().format( DateTimeFormatter.ofPattern( "MMMM dd, yyyy" ) ));
        ( (TextView) displayView.findViewById( R.id.note_value ) ).setText(transaction.getNote());
        ( (TextView) displayView.findViewById( R.id.recurrance_value )).setText(transaction.getRecurrance().toString());

        LinearLayout projectedTransactionsList = displayView.findViewById( R.id.projected_transactions_list );
        //todo: set a property for how far to project and use this whenever calling to get projections
        HashMap<LocalDate, ProjectedTransaction> projections = transaction.getProjectedTransactionsWithProjectedDate(budgetModel.getCutoffDate());
        if(projections.size() <= 0) projectedTransactionsList.addView( View.inflate( this.context, R.layout.view_empty_list_message, null ) );
        else projections.forEach( (date, projectedTransaction) -> {
            //maybe this could be moved to projectionViewModel.showProjectionLineItemDialog ?
            LinearLayout lineItem = (LinearLayout) LinearLayout.inflate( this.context, R.layout.layout_projected_transactions_lineitem, null );
            ((TextView)lineItem.findViewById( R.id.date_value )).setText(projectedTransaction.getScheduledDate().format( DateTimeFormatter.ofPattern( "MM-dd" ) ));
            ((TextView)lineItem.findViewById( R.id.label_value )).setText(projectedTransaction.getLabel());
            ((TextView)lineItem.findViewById( R.id.amount_value )).setText(String.valueOf( projectedTransaction.getAmount() ));

            lineItem.setOnClickListener( view -> projectionViewModel.showProjectionFullDisplayDialog(projectedTransaction) );

            projectedTransactionsList.addView( lineItem );
        } );


        return displayView;
    }

    private Transaction getTransactionFromView(Transaction transaction, View containing_view, String view_type){
        String label = null;
        Double amount = null;
        String note = null;
        LocalDate date = null;
        String recurrence = null;
        HashMap<Integer, PropertySetter> properties = transaction.taggedPropertySetters();

        if(view_type.contentEquals( "editor" )){
            label = ( (EditText) containing_view.findViewById( R.id.label_editor )).getText().toString();
            date = (LocalDate)( (Button) containing_view.findViewById( R.id.date_editor_button )).getTag(R.string.date_tag );
            amount = Double.valueOf(( (EditText) containing_view.findViewById( R.id.amount_editor )).getText().toString());
            note = ( (EditText) containing_view.findViewById( R.id.note_editor )).getText().toString();
            recurrence = ( (Button) containing_view.findViewById( R.id.recurrance_editor_button )).getTag(R.string.transaction_recurrence_tag).toString();
        }

        properties.get(R.string.transaction_label_tag).setProperty( label );
        properties.get(R.string.date_tag ).setProperty( date );
        properties.get(R.string.transaction_amount_tag).setProperty( amount );
        properties.get(R.string.transaction_note_tag).setProperty( note );
        properties.get(R.string.transaction_recurrence_tag).setProperty( recurrence );


        return transaction;
    }

}


