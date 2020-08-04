package com.e.fudgetbudget;

import android.app.DatePickerDialog;
import android.graphics.drawable.Icon;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.e.fudgetbudget.model.ProjectedTransaction;
import com.e.fudgetbudget.model.RecordedTransaction;
import com.e.fudgetbudget.model.Transaction;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ViewModel<T extends Transaction> {

    private final MainActivity context;
    private BudgetModel budgetModel;


    ViewModel(MainActivity context, BudgetModel budgetModel) {
        this.context = context;
        this.budgetModel = budgetModel;
    }

    void createTransaction(int transaction_type) {
        T newTransaction = (T) Transaction.getInstance( transaction_type, this.context.getExternalFilesDir( null ).getAbsolutePath() );
        editTransaction( newTransaction );
    }

    void getListView(ViewGroup listView, ArrayList<T> listData, int[] listColumns){
        LinearLayout listHeader = listView.findViewById( R.id.list_header );
        LinearLayout listBody = listView.findViewById( R.id.list_body );


        for( T transaction : listData ){
            LinearLayout listLineItem = (LinearLayout) LinearLayout.inflate( this.context, R.layout.view_list_lineitem, null );
            LinearLayout listLineItemBody = listLineItem.findViewById( R.id.list_lineitem_body );

            //set date


            //set label


            //set amount


            //set recurrence if T instanceof Transaction and is recurring




            for (int tagid : listColumns) {
                LinearLayout cellView = (LinearLayout) View.inflate( this.context, R.layout.view_list_lineitem_cell, null );
                TextView textView = cellView.findViewById( R.id.cell_value );

                Object value = transaction.getProperty( tagid );
                float cellWeight;

                if (tagid == R.string.label_tag) {
                    textView.setText( String.valueOf( value ) );
                    cellWeight = 6;
                } else if (tagid == R.string.date_tag) {
                    textView.setText( ((LocalDate) value).format( DateTimeFormatter.ofPattern( "d - eee" ) ) );
                    textView.setTextAlignment( View.TEXT_ALIGNMENT_CENTER );
                    cellWeight = 2;
                } else if (tagid == R.string.amount_tag) {
                    textView.setText( String.valueOf( value ) );
                    cellWeight = 1;
                } else if (tagid == R.string.recurrence_tag) {
                    String readableValue = String.valueOf( value );
                    textView.setText( readableValue );
                    cellWeight = 2;
                } else {
                    textView.setText( String.valueOf( value ) );
                    cellWeight = 1;
                }


                cellView.setLayoutParams( new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, cellWeight ) );

                listLineItemBody.addView( cellView );
            }

            listLineItemBody.setOnClickListener( view -> viewTransaction( transaction ) );
            listBody.addView( listLineItem );
        }
    }

    void getListView(ViewGroup listView, ArrayList<Object[]> listData){
        listView.removeView( listView.findViewById( R.id.list_header ));
        LinearLayout listBody = listView.findViewById( R.id.list_body );

        for(Object listItem : listData){
            LinearLayout listLineItem = (LinearLayout) LinearLayout.inflate( this.context, R.layout.view_list_lineitem, null );
            LinearLayout listLineItemBody = listLineItem.findViewById( R.id.list_lineitem_body );

            listLineItem.setPadding( 8, 8, 8, 8 );

            Object[] entry = (Object[]) listItem;
            LocalDate periodBeginDate = (LocalDate) entry[0];
            ArrayList<Double> periodBalances = (ArrayList<Double>) entry[1];
            HashMap<T, Double> periodProjections = (HashMap<T, Double>) entry[2];

            LinearLayout periodDateWrapper = (LinearLayout) View.inflate( this.context, R.layout.view_list_lineitem_cell, null );
            TextView periodDate = periodDateWrapper.findViewById( R.id.cell_value );
            periodDate.setText( periodBeginDate.format( DateTimeFormatter.ofPattern( "MMMM yyyy" ) ) );
            periodDate.setTextAppearance( R.style.period_date );
            periodDateWrapper.setLayoutParams( new LinearLayout.LayoutParams( LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 2 ) );


            LinearLayout incomeBalanceWrapper = (LinearLayout) View.inflate( this.context, R.layout.view_list_lineitem_cell, null );
            TextView incomeBalance = incomeBalanceWrapper.findViewById( R.id.cell_value );
            incomeBalance.setText( "+ $" + String.valueOf( periodBalances.get( 1 ) ) );
            incomeBalance.setTextAppearance( R.style.period_income );

            LinearLayout expenseBalanceWrapper = (LinearLayout) View.inflate( this.context, R.layout.view_list_lineitem_cell, null );
            TextView expenseBalance = expenseBalanceWrapper.findViewById( R.id.cell_value );
            expenseBalance.setText( "- $" + String.valueOf( periodBalances.get( 2 ) ) );
            expenseBalance.setTextAppearance( R.style.period_expense );

            LinearLayout endOfPeriodBalanceWrapper = (LinearLayout) View.inflate( this.context, R.layout.view_list_lineitem_cell, null );
            TextView endOfPeriodBalance = endOfPeriodBalanceWrapper.findViewById( R.id.cell_value );
            endOfPeriodBalance.setText( String.valueOf( periodBalances.get( 0 ) ) );
            endOfPeriodBalance.setTextAppearance( R.style.period_balance );

            listLineItemBody.addView( periodDateWrapper );
            listLineItemBody.addView( incomeBalanceWrapper );
            listLineItemBody.addView( expenseBalanceWrapper );
            listLineItemBody.addView( endOfPeriodBalanceWrapper );

            listLineItem.setOnClickListener( v -> {
                ViewGroup expandedList = v.findViewById( R.id.list );

                if (expandedList instanceof View) {
                    listLineItem.removeView( expandedList );
                } else {
                    int[] projectionColumns = new int[]{R.string.date_tag, R.string.label_tag, R.string.amount_tag};
                    expandedList = (LinearLayout) View.inflate( this.context, R.layout.view_list, null );
                    getListView( expandedList, periodProjections );
                    listLineItem.addView( expandedList );
                }
            } );
            listBody.addView( listLineItem );
        }
    }

    private void getListView(ViewGroup listView, HashMap<T, Double> listData){
        listView.removeView( listView.findViewById( R.id.list_header ));
        LinearLayout listBody = listView.findViewById( R.id.list_body );

        for(Map.Entry<T, Double> listItem : listData.entrySet()){
            T transaction = listItem.getKey();
            Double balance = listItem.getValue();
            LinearLayout listLineItem = (LinearLayout) LinearLayout.inflate( this.context, R.layout.view_list_lineitem, null );
            LinearLayout listLineItemBody = listLineItem.findViewById( R.id.list_lineitem_body );

            //set date column
            LinearLayout dateWrapper = (LinearLayout) View.inflate( this.context, R.layout.view_list_lineitem_cell, null );
            TextView date = dateWrapper.findViewById( R.id.cell_value );
            date.setText( ((LocalDate)transaction.getProperty( R.string.date_tag )).format( DateTimeFormatter.ofPattern( "d - eee" )));
            date.setTextAlignment( View.TEXT_ALIGNMENT_TEXT_START );
//            dateWrapper.setLayoutParams( new LinearLayout.LayoutParams(
//                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1 ) );

            //set label column
            LinearLayout labelWrapper = (LinearLayout) View.inflate( this.context, R.layout.view_list_lineitem_cell, null );
            TextView label = labelWrapper.findViewById( R.id.cell_value );
            label.setText( ((String)transaction.getProperty( R.string.label_tag )) );
            date.setTextAlignment( View.TEXT_ALIGNMENT_CENTER );
            labelWrapper.setLayoutParams( new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 5 ) );

            //set income amount column
            LinearLayout incomeWrapper = (LinearLayout) View.inflate( this.context, R.layout.view_list_lineitem_cell, null );
            TextView income = incomeWrapper.findViewById( R.id.cell_value );
            if(transaction.getIncomeFlag()) income.setText( "+ " + transaction.getProperty( R.string.amount_tag ));
            else income.setText("");
            income.setTextAppearance( R.style.period_income );
            income.setTextAlignment( View.TEXT_ALIGNMENT_TEXT_END );
//            incomeWrapper.setLayoutParams( new LinearLayout.LayoutParams(
//                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1 ) );

            //set expense amount column
            LinearLayout expenseWrapper = (LinearLayout) View.inflate( this.context, R.layout.view_list_lineitem_cell, null );
            TextView expense = expenseWrapper.findViewById( R.id.cell_value );
            if(!transaction.getIncomeFlag()) expense.setText( "- " + transaction.getProperty( R.string.amount_tag ));
            else expense.setText("");
            expense.setTextAppearance( R.style.period_expense );
            expense.setTextAlignment( View.TEXT_ALIGNMENT_TEXT_END );
//            incomeWrapper.setLayoutParams( new LinearLayout.LayoutParams(
//                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1 ) );


            //set balance column
            LinearLayout balanceWrapper = (LinearLayout) View.inflate( this.context, R.layout.view_list_lineitem_cell, null );
            TextView balanceView = balanceWrapper.findViewById( R.id.cell_value );
            balanceView.setText( String.valueOf( balance ));
            balanceWrapper.setTextAlignment( View.TEXT_ALIGNMENT_VIEW_END );
//            balanceWrapper.setLayoutParams( new LinearLayout.LayoutParams(
//                    LinearLayout.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1 ) );

            listLineItemBody.setOnClickListener( view -> {
                viewTransaction( transaction );
            } );

            //set reconcile action button
            ImageButton recordButton = (ImageButton) View.inflate( this.context, R.layout.button_record_projection, null );
            if(( (LocalDate) transaction.getProperty( R.string.date_tag )).isAfter( LocalDate.now() )) recordButton.setVisibility( View.INVISIBLE );
            recordButton.setImageIcon( Icon.createWithResource(this.context, androidx.appcompat.R.drawable.abc_ic_ab_back_material ));
            recordButton.setOnClickListener( v -> reconcileTransaction( transaction ) );

            listLineItemBody.addView( recordButton );
            listLineItemBody.addView( dateWrapper );
            listLineItemBody.addView( labelWrapper );
            listLineItemBody.addView( incomeWrapper );
            listLineItemBody.addView( expenseWrapper );
            listLineItemBody.addView( balanceWrapper );
            listBody.addView( listLineItem );
        }
    }

    private void setTransactionPropertyEditorViews(ViewGroup editorView, Transaction transaction){
        if(transaction instanceof ProjectedTransaction){
            ProjectedTransaction projectedTransaction = (ProjectedTransaction) transaction;

            TextView label = editorView.findViewById( R.id.label_value );
            Button dateEditorButton = editorView.findViewById( R.id.date_editor_button );
            TextView scheduledDate = editorView.findViewById( R.id.scheduled_date_value );
            EditText editAmount = editorView.findViewById( R.id.amount_editor );
            TextView scheduledValue = editorView.findViewById( R.id.scheduled_amount_value );
            EditText editNote = editorView.findViewById( R.id.note_editor );

            LocalDate date = (LocalDate) projectedTransaction.getProperty( R.string.date_tag );

            if(dateEditorButton != null) dateEditorButton.setOnClickListener( view -> {
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
            } );
        }

        else if(transaction instanceof RecordedTransaction){
            RecordedTransaction recordedTransaction = (RecordedTransaction) transaction;

            Button dateEditorButton = (Button) editorView.findViewById( R.id.date_editor_button );
            EditText amountEditor = (EditText) editorView.findViewById( R.id.amount_editor );
            EditText noteEditor = (EditText) editorView.findViewById( R.id.note_editor );

            LocalDate date = (LocalDate) recordedTransaction.getProperty( R.string.date_tag );

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
            } );
        }

        else {
            Button dateEditorButton = (Button) editorView.findViewById( R.id.date_editor_button );
            Button recurranceEditorButton = editorView.findViewById( R.id.recurrance_editor_button );
            EditText labelEditor = (EditText) editorView.findViewById( R.id.label_editor );
            EditText amountEditor = (EditText) editorView.findViewById( R.id.amount_editor );
            EditText noteEditor = (EditText) editorView.findViewById( R.id.note_editor );

            LocalDate date = (LocalDate) transaction.getProperty( R.string.date_tag );

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
            } );
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
                                String selectedPeriod = ViewModel.this.context.getResources().getStringArray( R.array.date_periods )[position];
                                LinearLayout dayTypeLayout = recurrenceParametersLayout.findViewById( R.id.on_day_type_layout );
                                Spinner dayType = dayTypeLayout.findViewById( R.id.on_day_type_selector );
                                ArrayAdapter<CharSequence> adapter = null;

                                if(selectedPeriod.contentEquals( "Week" )){
                                    dayTypeLayout.setVisibility( View.VISIBLE );
                                    adapter= ArrayAdapter.createFromResource(ViewModel.this.context, R.array.days_of_week_options, android.R.layout.simple_list_item_1 );
                                    adapter.setDropDownViewResource( android.R.layout.simple_list_item_1 );
                                    dayType.setAdapter(adapter);
                                }
                                else if(selectedPeriod.contentEquals( "Month" ) || selectedPeriod.contentEquals( "Year" )){
                                    dayTypeLayout.setVisibility( View.VISIBLE );
                                    adapter= ArrayAdapter.createFromResource(ViewModel.this.context, R.array.day_of_month_options, android.R.layout.simple_list_item_1 );
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
        }
    }

    private void setTransactionPropertyDisplayViews(ViewGroup view, Transaction transaction) {
        if( transaction instanceof ProjectedTransaction ){
            ProjectedTransaction projectedTransaction = (ProjectedTransaction) transaction;

            TextView label = view.findViewById( R.id.label_value );
            TextView projectedDate = view.findViewById( R.id.projected_date_value );
            TextView scheduledDate = view.findViewById( R.id.scheduled_date_value );
            TextView amount = view.findViewById( R.id.amount_value );
            TextView scheduledValue = view.findViewById( R.id.scheduled_amount_value );
            TextView note = view.findViewById( R.id.note_value );

            label.setText((String)projectedTransaction.getProperty(R.string.label_tag ));
            projectedDate.setText(((LocalDate)projectedTransaction.getProperty( R.string.date_tag )).format(DateTimeFormatter.ofPattern( "MMMM dd, yyyy" )));
            scheduledDate.setText(((LocalDate)projectedTransaction.getProperty(R.string.date_tag )).format(DateTimeFormatter.ofPattern( "MMMM dd, yyyy" )));
            amount.setText(String.valueOf(projectedTransaction.getProperty(R.string.amount_tag )));
            scheduledValue.setText(String.valueOf( projectedTransaction.getProperty(R.string.amount_tag )) );
            note.setText((String)projectedTransaction.getProperty(R.string.note_tag ));
        }

        else if( transaction instanceof RecordedTransaction ){
            RecordedTransaction recordedTransaction = (RecordedTransaction) transaction;
            String label = (String) recordedTransaction.getProperty( R.string.label_tag );
            LocalDate date = (LocalDate) recordedTransaction.getProperty( R.string.date_tag );
            Double amount = (Double) recordedTransaction.getProperty( R.string.amount_tag );
            String note = (String) recordedTransaction.getProperty( R.string.note_tag );

            ((TextView) view.findViewById( R.id.label_value )).setText( label );
            ((TextView) view.findViewById( R.id.date_value )).setText( date.format( DateTimeFormatter.ofPattern( "MMMM d, yyyy" ) ));
            ((TextView) view.findViewById( R.id.amount_value )).setText( String.valueOf( amount ));
            ((TextView) view.findViewById( R.id.note_value )).setText( note );
        }

        else {
            ( (TextView) view.findViewById( R.id.label_value ) ).setText((String)transaction.getProperty(R.string.label_tag ));
            ( (TextView) view.findViewById( R.id.amount_value ) ).setText(String.valueOf(transaction.getProperty(R.string.amount_tag )));
            ( (TextView) view.findViewById( R.id.date_value ) ).setText(((LocalDate)transaction.getProperty( R.string.date_tag )).format( DateTimeFormatter.ofPattern( "MMMM dd, yyyy" ) ));
            ( (TextView) view.findViewById( R.id.note_value ) ).setText((String)transaction.getProperty(R.string.note_tag ));
            ( (TextView) view.findViewById( R.id.recurrance_value )).setText((String)transaction.getProperty(R.string.recurrence_tag ));
        }
    }

    private void updateTransactionFromEditorLayout(ViewGroup editorView, Transaction transaction){
        if(transaction instanceof ProjectedTransaction){
            ProjectedTransaction projection = (ProjectedTransaction) transaction;

            Button dateButton = (Button) editorView.findViewById( R.id.date_editor_button );
            long longDate = Long.parseLong( dateButton.getTag(R.string.date_value_tag).toString() );
            LocalDate date = LocalDate.ofEpochDay( longDate );
            Double amount = Double.valueOf(( (EditText) editorView.findViewById( R.id.amount_editor )).getText().toString());
            String note = ( (EditText) editorView.findViewById( R.id.note_editor )).getText().toString();

            projection.setProperty( R.string.date_tag, date );
            projection.setProperty( R.string.amount_tag, amount );
            projection.setProperty( R.string.note_tag, note );

        }

        else if(transaction instanceof RecordedTransaction){
            RecordedTransaction record = (RecordedTransaction) transaction;
            String date = ((Button) editorView.findViewById( R.id.date_editor_button )).getText().toString();
            String amount = ((EditText) editorView.findViewById( R.id.amount_editor )).getText().toString();
            String note = ((EditText) editorView.findViewById( R.id.note_editor )).getText().toString();

            record.setProperty( R.string.date_tag, date );
            record.setProperty( R.string.amount_tag, amount );
            record.setProperty( R.string.note_tag, note );

        }

        else {
            String label = ( (EditText) editorView.findViewById( R.id.label_editor )).getText().toString();
            LocalDate date = (LocalDate) editorView.findViewById( R.id.date_editor_button ).getTag(R.string.date_tag );
            Double amount = Double.valueOf(( (EditText) editorView.findViewById( R.id.amount_editor )).getText().toString());
            String note = ( (EditText) editorView.findViewById( R.id.note_editor )).getText().toString();
            String recurrence = editorView.findViewById( R.id.recurrance_editor_button ).getTag(R.string.recurrence_tag ).toString();

            transaction.setProperty( R.string.label_tag, label );
            transaction.setProperty( R.string.date_tag, date );
            transaction.setProperty( R.string.amount_tag, amount );
            transaction.setProperty( R.string.note_tag, note );
            transaction.setProperty( R.string.recurrence_tag, recurrence );

        }
    }

    private void reconcileTransaction(T transaction) {
        AlertDialog.Builder builder = new AlertDialog.Builder( this.context );
        LinearLayout reconcileView = (LinearLayout) LinearLayout.inflate( this.context, R.layout.layout_reconcile_projection, null );

        setTransactionPropertyEditorViews( reconcileView, transaction );

        builder.setNegativeButton( "Cancel", (dialog, which) -> dialog.dismiss() );
        builder.setPositiveButton( "Create Record", (dialog, which) -> {
            budgetModel.reconcile(transaction);
            dialog.dismiss();
            this.context.recreate();
        } );

        builder.setView( reconcileView );
        builder.show();
    }

    private void viewTransaction(T transaction){
        AlertDialog.Builder builder = new AlertDialog.Builder( this.context );
        ViewGroup dialogView;

        if(transaction instanceof ProjectedTransaction)
            dialogView = (ViewGroup) ViewGroup.inflate( this.context, R.layout.layout_projected_transactions_fulldisplay, null );
        else if(transaction instanceof RecordedTransaction)
            dialogView = (ViewGroup) ViewGroup.inflate( this.context, R.layout.layout_record_fulldisplay, null );
        else dialogView = (ViewGroup) ViewGroup.inflate( this.context, R.layout.layout_transaction_fulldisplay, null );

        setTransactionPropertyDisplayViews(dialogView, transaction);

        builder.setPositiveButton( "Edit", (dialog, which) -> {
            editTransaction( transaction );
            dialog.dismiss();
        });

        builder.setNegativeButton( "Close", (dialog, which) -> dialog.dismiss() );
        builder.setView( dialogView );
        builder.show();
    }

    private void editTransaction(T transaction){
        AlertDialog.Builder builder = new AlertDialog.Builder( this.context );
        ViewGroup dialogView;

        if(transaction instanceof ProjectedTransaction)
            dialogView = (ViewGroup) ViewGroup.inflate( this.context, R.layout.layout_projected_transactions_editor, null );
        else if(transaction instanceof RecordedTransaction)
            dialogView = (ViewGroup) ViewGroup.inflate( this.context, R.layout.layout_record_editor, null );
        else dialogView = (ViewGroup) ViewGroup.inflate( this.context, R.layout.layout_transaction_editor, null );

        setTransactionPropertyEditorViews(dialogView, transaction);

        builder.setPositiveButton( "Save", (dialog, which) -> {
            updateTransactionFromEditorLayout(dialogView, transaction);
            budgetModel.update( transaction );
            dialog.dismiss();
            this.context.recreate();
        });

        builder.setNeutralButton( "Delete", (dialog, which) -> {
            budgetModel.delete( transaction );
            dialog.dismiss();
            this.context.recreate();
        } );

        builder.setNegativeButton( "Close", (dialog, which) -> dialog.dismiss() );
        builder.setView( dialogView );
        builder.show();
    }

}
