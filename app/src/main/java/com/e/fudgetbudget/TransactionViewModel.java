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
import com.e.fudgetbudget.model.Transaction;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;


class TransactionViewModel {
    private final MainActivity context;
    private BudgetModel budgetModel;


    TransactionViewModel(MainActivity context, BudgetModel budgetModel) {
        this.context = context;
        this.budgetModel = budgetModel;
    }

    void loadIncomeLineItems(ViewGroup list_view) {
        ArrayList<Transaction> income = budgetModel.getTransactionsByType("income");
        if(income.size() <= 0) list_view.addView( TextView.inflate( this.context, R.layout.view_empty_list_message, null ) );

        else income.iterator().forEachRemaining( transaction -> {
            LinearLayout lineItem = getLineItemLayout( transaction );
            lineItem.setOnClickListener( view -> showFullDisplayDialog(transaction) );
            list_view.addView( lineItem );
        });
    }
    void loadExpenseLineItems(ViewGroup list_view) {
        ArrayList<Transaction> expenses = budgetModel.getTransactionsByType("expense");
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
        else if(object instanceof Integer && (Integer)object == R.string.expense_tag)
            transaction = Transaction.getInstance(R.string.expense_tag, this.context.getExternalFilesDir( null ).getAbsolutePath());
        else if(object instanceof Integer && (Integer)object == R.string.income_tag)
            transaction = Transaction.getInstance(R.string.income_tag, this.context.getExternalFilesDir( null ).getAbsolutePath());
        else transaction = null;

        editor_view = getEditorLayout( transaction );
        TextView header = editor_view.findViewById( R.id.editor_header );
        if((boolean)transaction.getProperty(R.string.income_tag )) header.setText("Income Transaction");
        else header.setText("Expense Transaction");

        builder.setView( editor_view );
        builder.setPositiveButton( "Save", (dialog, which) -> {
            budgetModel.update( getTransactionFromEditorView( transaction, editor_view ) );
            dialog.dismiss();
            this.context.recreate();
        });
        builder.setNegativeButton( "Close", (dialog, which) -> {
            dialog.dismiss();
        } );
        builder.show();
    }
    void showFullDisplayDialog(Transaction transaction) {
        AlertDialog.Builder builder = new AlertDialog.Builder( this.context );
        LinearLayout fullDisplayView = getFullDisplayLayout( transaction );

        TextView header = fullDisplayView.findViewById( R.id.fulldisplay_header );
        if((boolean)transaction.getProperty( R.string.income_tag )) header.setText("Income Transaction");
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

    LinearLayout getEditorLayout(Transaction transaction){
        LinearLayout editorView = (LinearLayout) this.context.getLayoutInflater().inflate( R.layout.layout_transaction_editor, null );

        if(transaction == null) return editorView;
        editorView.setTag( R.string.id_tag, (UUID)transaction.getProperty( R.string.id_tag ) );
        if((boolean)transaction.getProperty( R.string.income_tag )) editorView.setTag(R.string.object_type_tag, R.string.income_tag );
        else editorView.setTag(R.string.object_type_tag, R.string.income_tag );

        ( (EditText) editorView.findViewById( R.id.label_editor ) )
                .setText((String)transaction.getProperty( R.string.label_tag ));

        Button dateEditorButton = (Button) editorView.findViewById( R.id.date_editor_button );
        LocalDate date = (LocalDate) transaction.getProperty( R.string.date_tag );
        dateEditorButton.setText(date.format( DateTimeFormatter.ofPattern( "MMMM dd, yyyy" ) ));
        dateEditorButton.setTag(R.string.date_tag, date);
        dateEditorButton.setOnClickListener( view -> {
            final LocalDate[] newDate = {date};
            DatePickerDialog pickerDialog = new DatePickerDialog( this.context );
            DatePicker picker = pickerDialog.getDatePicker();

            picker.init( newDate[0].getYear(), newDate[0].getMonthValue()-1, newDate[0].getDayOfMonth(), (button_view, year, monthOfYear, dayOfMonth) -> {
                newDate[0] = LocalDate.of( year, monthOfYear+1, dayOfMonth );
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
        String recurrenceValue = (String)transaction.getProperty( R.string.recurrence_tag );
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

                Arrays.fill( groupValues, "0" );

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
                recurranceEditorButton.setTag(R.string.recurrence_tag, capturedRecurrenceValue.toString() );
            } );
            builder.setNegativeButton( "Cancel", ((dialog, which) -> { dialog.dismiss(); }) );
            builder.setView( recurranceEditor );
            builder.show();
        });

        ( (EditText) editorView.findViewById( R.id.amount_editor ))
                .setText(String.valueOf( transaction.getProperty( R.string.amount_tag ) ));

        ( (EditText) editorView.findViewById( R.id.note_editor ))
                .setText( (String)transaction.getProperty( R.string.note_tag ) );

        return editorView;
    }
    private LinearLayout getLineItemLayout(Transaction transaction){
        LinearLayout lineItemView = (LinearLayout)LinearLayout.inflate( this.context, R.layout.layout_transaction_lineitem, null);

        TextView date = lineItemView.findViewById( R.id.date_value );
        TextView label = lineItemView.findViewById( R.id.label_value );
        TextView amount = lineItemView.findViewById( R.id.amount_value );

        date.setText( ((LocalDate)transaction.getProperty( R.string.date_tag )).format( DateTimeFormatter.ofPattern( "MM/dd" ) ));
        label.setText( (String) transaction.getProperty( R.string.label_tag ) );
        amount.setText( String.valueOf( transaction.getProperty( R.string.amount_tag ) ));

        //I think this is redundant: also setting onClickListener to open fullDisplay when getLineItems is called
        lineItemView.setOnClickListener( view -> showFullDisplayDialog(transaction) );


        return lineItemView;
    }
    private <T extends Transaction> LinearLayout getFullDisplayLayout(Transaction transaction){
        LinearLayout displayView = (LinearLayout)LinearLayout.inflate( this.context, R.layout.layout_transaction_fulldisplay, null);

        ( (TextView) displayView.findViewById( R.id.label_value ) ).setText((String)transaction.getProperty(R.string.label_tag ));
        ( (TextView) displayView.findViewById( R.id.amount_value ) ).setText(String.valueOf(transaction.getProperty(R.string.amount_tag )));
        ( (TextView) displayView.findViewById( R.id.date_value ) ).setText(((LocalDate)transaction.getProperty( R.string.date_tag )).format( DateTimeFormatter.ofPattern( "MMMM dd, yyyy" ) ));
        ( (TextView) displayView.findViewById( R.id.note_value ) ).setText((String)transaction.getProperty(R.string.note_tag ));
        ( (TextView) displayView.findViewById( R.id.recurrance_value )).setText((String)transaction.getProperty(R.string.recurrence_tag ));

        if(!((String)transaction.getProperty( R.string.recurrence_tag )).contentEquals( "0-0-0-0-0-0-0")){
            LinearLayout projectedTransactionsList = displayView.findViewById( R.id.projected_transactions_list );
            ArrayList<T> projections = budgetModel.getProjections(transaction);

            if(projections.size() <= 0) projectedTransactionsList.addView( View.inflate( this.context, R.layout.view_empty_list_message, null ) );
            else projections.forEach( projectedTransaction -> {
                LinearLayout lineItem = (LinearLayout) LinearLayout.inflate( this.context, R.layout.layout_projected_transactions_lineitem, null );
                ((TextView)lineItem.findViewById( R.id.date_value )).setText(((LocalDate)projectedTransaction.getProperty( R.string.date_tag )).format( DateTimeFormatter.ofPattern( "MM-dd" ) ));
                ((TextView)lineItem.findViewById( R.id.label_value )).setText((String)projectedTransaction.getProperty(R.string.label_tag ));
                ((TextView)lineItem.findViewById( R.id.amount_value )).setText(String.valueOf( projectedTransaction.getProperty(R.string.amount_tag ) ));

                lineItem.setOnClickListener( view -> {
                    if(projectedTransaction instanceof ProjectedTransaction) this.context.projectionViewModel.showProjectionFullDisplayDialog((ProjectedTransaction)projectedTransaction);
                    else throw new Error("unhandled object type");
                } );

                projectedTransactionsList.addView( lineItem );
            } );
        } else {
            displayView.removeView( displayView.findViewById( R.id.layout_transaction_fulldisplay_recurrance ) );
            displayView.removeView( displayView.findViewById( R.id.layout_transaction_fulldisplay_futureProjections ) );
        }

        return displayView;
    }

    private Transaction getTransactionFromEditorView(Transaction transaction, View containing_view){
        String label = ( (EditText) containing_view.findViewById( R.id.label_editor )).getText().toString();
        LocalDate date = (LocalDate) containing_view.findViewById( R.id.date_editor_button ).getTag(R.string.date_tag );
        Double amount = Double.valueOf(( (EditText) containing_view.findViewById( R.id.amount_editor )).getText().toString());
        String note = ( (EditText) containing_view.findViewById( R.id.note_editor )).getText().toString();
        String recurrence = containing_view.findViewById( R.id.recurrance_editor_button ).getTag(R.string.recurrence_tag ).toString();

        transaction.setProperty( R.string.label_tag, label );
        transaction.setProperty( R.string.date_tag, date );
        transaction.setProperty( R.string.amount_tag, amount );
        transaction.setProperty( R.string.note_tag, note );
        transaction.setProperty( R.string.recurrence_tag, recurrence );

        return transaction;
    }

}


