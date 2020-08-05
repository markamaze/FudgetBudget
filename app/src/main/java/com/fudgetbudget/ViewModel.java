package com.fudgetbudget;

import android.app.DatePickerDialog;
import android.graphics.Color;
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

import com.fudgetbudget.model.ProjectedTransaction;
import com.fudgetbudget.model.RecordedTransaction;
import com.fudgetbudget.model.Transaction;

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
        viewTransaction( newTransaction, true );
    }

    //build list view for any of the Transaction classes, using tags to set the property columns
    void buildListView(ViewGroup listView, ArrayList<T> listData, int[] listColumns){
        LinearLayout listHeader = listView.findViewById( R.id.list_header );
        LinearLayout listBody = listView.findViewById( R.id.list_body );


        for( T transaction : listData ){
            LinearLayout listLineItem = (LinearLayout) LinearLayout.inflate( this.context, R.layout.view_list_lineitem, null );
            LinearLayout listLineItemBody = listLineItem.findViewById( R.id.list_lineitem_body );

            if(Arrays.stream(listColumns).anyMatch(i -> i == R.string.date_tag)){
                LinearLayout dateWrapper = (LinearLayout) View.inflate( this.context, R.layout.view_list_lineitem_cell, null );
                TextView dateView = dateWrapper.findViewById( R.id.cell_value );

                LocalDate date = (LocalDate)transaction.getProperty( R.string.date_tag );
                if(date == null) dateView.setText( "unscheduled" );
                else dateView.setText(date.format( DateTimeFormatter.ofPattern( "eee MMM d" ) ));

                dateView.setTextAppearance( R.style.line_item_cell_date );

                if(!Arrays.stream( listColumns ).anyMatch( i -> i == R.string.label_tag ))
                    dateWrapper.setLayoutParams( new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 3 ) );

                listLineItemBody.addView( dateWrapper );
            }

            if(Arrays.stream(listColumns).anyMatch(i -> i == R.string.label_tag )){
                LinearLayout labelWrapper = (LinearLayout) View.inflate( this.context, R.layout.view_list_lineitem_cell, null );
                TextView labelView = labelWrapper.findViewById( R.id.cell_value );

                String label = (String) transaction.getProperty( R.string.label_tag );
                labelView.setText( label );

                labelWrapper.setLayoutParams( new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 3 ) );
                labelView.setTextAppearance( R.style.line_item_cell_label );

                listLineItemBody.addView( labelWrapper );
            }

            if(Arrays.stream(listColumns).anyMatch(i -> i == R.string.recurrence_tag )){
                LinearLayout recurranceWrapper = (LinearLayout) View.inflate( this.context, R.layout.view_list_lineitem_cell, null );
                TextView recurranceView = recurranceWrapper.findViewById( R.id.cell_value );

                String recurrance = (String) transaction.getProperty( R.string.recurrence_tag );
                recurranceView.setText( "repeat value" );

                listLineItemBody.addView( recurranceWrapper );
            }

            if(Arrays.stream(listColumns).anyMatch(i -> i == R.string.amount_tag )){
                LinearLayout amountWrapper = (LinearLayout) View.inflate( this.context, R.layout.view_list_lineitem_cell, null );
                TextView amountView = amountWrapper.findViewById( R.id.cell_value );

                String amount = String.valueOf( (Double) transaction.getProperty( R.string.amount_tag ) );
                amountView.setText( amount );
                if(transaction.getIncomeFlag()) amountView.setTextAppearance( R.style.line_item_cell_amount_income );
                else amountView.setTextAppearance( R.style.line_item_cell_amount_expense );
                listLineItemBody.addView( amountWrapper );
            }

            if(Arrays.stream(listColumns).anyMatch(i -> i == R.string.note_tag )){
                LinearLayout noteWrapper = (LinearLayout) View.inflate( this.context, R.layout.view_list_lineitem_cell, null );
                TextView noteView = noteWrapper.findViewById( R.id.cell_value );

                String note = (String) transaction.getProperty( R.string.note_tag );
                noteView.setText( note );

                listLineItemBody.addView( noteWrapper );
            }

            listLineItemBody.setOnClickListener( view -> viewTransaction( transaction, false ) );
            listBody.addView( listLineItem );
        }
    }

    //builds list view for period balance projections
    void buildListView(ViewGroup listView, ArrayList<Object[]> listData){
        listView.removeView( listView.findViewById( R.id.list_header ));
        LinearLayout listBody = listView.findViewById( R.id.list_body );

        for(Object listItem : listData){
            LinearLayout listLineItem = (LinearLayout) LinearLayout.inflate( this.context, R.layout.view_list_lineitem, null );
            LinearLayout listLineItemBody = listLineItem.findViewById( R.id.list_lineitem_body );

            Object[] entry = (Object[]) listItem;
            LocalDate periodBeginDate = (LocalDate) entry[0];
            ArrayList<Double> periodBalances = (ArrayList<Double>) entry[1];
            HashMap<T, Double> periodProjections = (HashMap<T, Double>) entry[2];
            boolean expandPeriodTransactions;
            if(periodBeginDate.isAfter( LocalDate.now() )) expandPeriodTransactions = false;
            else expandPeriodTransactions = true;



            //set period date
            LinearLayout periodDateWrapper = (LinearLayout) View.inflate( this.context, R.layout.view_list_lineitem_cell, null );
            TextView periodDate = periodDateWrapper.findViewById( R.id.cell_value );
            periodDate.setText( periodBeginDate.format( DateTimeFormatter.ofPattern( "MMMM yyyy" ) ) );
            periodDate.setTextAppearance( R.style.period_date );
            periodDateWrapper.setLayoutParams( new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 2 ) );
            listLineItemBody.addView( periodDateWrapper );

            //set period income total
            LinearLayout incomeBalanceWrapper = (LinearLayout) View.inflate( this.context, R.layout.view_list_lineitem_cell, null );
            TextView incomeBalance = incomeBalanceWrapper.findViewById( R.id.cell_value );
            incomeBalance.setText( "+ $" + String.valueOf( periodBalances.get( 1 ) ) );
            incomeBalance.setTextAppearance( R.style.line_item_cell_amount_income );
            listLineItemBody.addView( incomeBalanceWrapper );

            //set period expense total
            LinearLayout expenseBalanceWrapper = (LinearLayout) View.inflate( this.context, R.layout.view_list_lineitem_cell, null );
            TextView expenseBalance = expenseBalanceWrapper.findViewById( R.id.cell_value );
            expenseBalance.setText( "- $" + String.valueOf( periodBalances.get( 2 ) ) );
            expenseBalance.setTextAppearance( R.style.line_item_cell_amount_expense );
            listLineItemBody.addView( expenseBalanceWrapper );

            //set period beginning balance
            LinearLayout startOfPeriodBalanceWrapper = (LinearLayout) View.inflate( this.context, R.layout.view_list_lineitem_cell, null );
            TextView startfPeriodBalance = startOfPeriodBalanceWrapper.findViewById( R.id.cell_value );
            startfPeriodBalance.setText( String.valueOf( periodBalances.get( 0 ) ) );
            startfPeriodBalance.setTextAppearance( R.style.line_item_cell_balance );
            listLineItemBody.addView( startOfPeriodBalanceWrapper );


            //add list of all projections for the period, with running balances
            if(expandPeriodTransactions){
                LinearLayout expandedList = (LinearLayout) View.inflate( this.context, R.layout.view_list, null );
                buildListView( expandedList, periodProjections );
                listLineItem.addView( expandedList );
            }
            else {
                listLineItem.setOnClickListener( v -> {

                    LinearLayout expandedList = v.findViewById( R.id.list );

                    if(expandedList == null){
                        expandedList = (LinearLayout) View.inflate( this.context, R.layout.view_list, null );
                        buildListView( expandedList, periodProjections );
                        listLineItem.addView( expandedList );
                    }
                    else listLineItem.removeView( expandedList );
                } );
            }




            listBody.addView( listLineItem );
        }
    }

    //builds list view for projected transactions with resulting balance
    private void buildListView(ViewGroup listView, HashMap<T, Double> listData){
        listView.removeView( listView.findViewById( R.id.list_header ));
        LinearLayout listBody = listView.findViewById( R.id.list_body );

        for(Map.Entry<T, Double> listItem : listData.entrySet()){
            LinearLayout listLineItem = (LinearLayout) LinearLayout.inflate( this.context, R.layout.view_list_lineitem, null );
            LinearLayout listLineItemBody = listLineItem.findViewById( R.id.list_lineitem_body );

            T transaction = listItem.getKey();
            Double balance = listItem.getValue();


            //set reconcile action button
            if(( (LocalDate) transaction.getProperty( R.string.date_tag )).isBefore( LocalDate.now().plusDays( 1 ) )){
                ImageButton recordButton = (ImageButton) View.inflate( this.context, R.layout.view_button_record_projection, null );
                recordButton.setImageIcon( Icon.createWithResource(this.context, androidx.appcompat.R.drawable.abc_ic_ab_back_material ));
                recordButton.setOnClickListener( v -> reconcileTransaction( transaction ) );
                listLineItemBody.addView( recordButton );
            }

            //set date column
            LinearLayout dateWrapper = (LinearLayout) View.inflate( this.context, R.layout.view_list_lineitem_cell, null );
            TextView date = dateWrapper.findViewById( R.id.cell_value );
            date.setText( ((LocalDate)transaction.getProperty( R.string.date_tag )).format( DateTimeFormatter.ofPattern( "d - eee" )));
            date.setTextAppearance( R.style.line_item_cell_date );
            listLineItemBody.addView( dateWrapper );

            //set label column
            LinearLayout labelWrapper = (LinearLayout) View.inflate( this.context, R.layout.view_list_lineitem_cell, null );
            TextView label = labelWrapper.findViewById( R.id.cell_value );
            label.setText( ((String)transaction.getProperty( R.string.label_tag )) );
            date.setTextAppearance( R.style.line_item_cell_label );
            labelWrapper.setLayoutParams( new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 5 ) );
            listLineItemBody.addView( labelWrapper );

            //set income amount column
            LinearLayout incomeWrapper = (LinearLayout) View.inflate( this.context, R.layout.view_list_lineitem_cell, null );
            TextView income = incomeWrapper.findViewById( R.id.cell_value );
            if(transaction.getIncomeFlag()) income.setText( "+ " + transaction.getProperty( R.string.amount_tag ));
            else income.setText("");
            income.setTextAppearance( R.style.line_item_cell_amount_income );
            listLineItemBody.addView( incomeWrapper );

            //set expense amount column
            LinearLayout expenseWrapper = (LinearLayout) View.inflate( this.context, R.layout.view_list_lineitem_cell, null );
            TextView expense = expenseWrapper.findViewById( R.id.cell_value );
            if(!transaction.getIncomeFlag()) expense.setText( "- " + transaction.getProperty( R.string.amount_tag ));
            else expense.setText("");
            expense.setTextAppearance( R.style.line_item_cell_amount_expense );
            listLineItemBody.addView( expenseWrapper );

            //set balance column
            LinearLayout balanceWrapper = (LinearLayout) View.inflate( this.context, R.layout.view_list_lineitem_cell, null );
            TextView balanceView = balanceWrapper.findViewById( R.id.cell_value );
            balanceView.setText( String.valueOf( balance ));
            balanceView.setTextAppearance( R.style.line_item_cell_balance );
            listLineItemBody.addView( balanceWrapper );


            listLineItemBody.setOnClickListener( view -> viewTransaction( transaction, false ) );

            listBody.addView( listLineItem );
        }
    }

    private void setTransactionPropertyViews(ViewGroup view, Transaction transaction, boolean isEditable){
        EditText labelEditor = (EditText) view.findViewById( R.id.label_value );
        if(labelEditor != null){
            labelEditor.setText(String.valueOf( transaction.getProperty( R.string.label_tag ) ));
            labelEditor.setEnabled( isEditable );
            labelEditor.setTextColor( Color.BLACK );
        }


        Button dateEditorButton = (Button) view.findViewById( R.id.date_value );
        if(dateEditorButton != null){
            LocalDate date = (LocalDate) transaction.getProperty( R.string.date_tag );
            if(date == null) {
                dateEditorButton.setText("unscheduled");
                date = LocalDate.now();
            }
            else dateEditorButton.setText( date.format( DateTimeFormatter.ofPattern( "MMMM dd, yyyy" ) ) );
            dateEditorButton.setTag(R.string.date_tag, date);
            LocalDate finalDate = date;
            dateEditorButton.setOnClickListener( dateButtonView -> {
                final LocalDate[] newDate = {finalDate};
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
            dateEditorButton.setEnabled( isEditable );
            dateEditorButton.setTextColor( Color.BLACK );
        }


        Button recurranceEditorButton = view.findViewById( R.id.recurrance_value );
        if(recurranceEditorButton != null) {
            recurranceEditorButton.setText( (String) transaction.getProperty( R.string.recurrence_tag ) );
            recurranceEditorButton.setOnClickListener( recurranceButtonView -> {
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
            recurranceEditorButton.setEnabled( isEditable );
            recurranceEditorButton.setTextColor( Color.BLACK );

        }


        EditText amount = (EditText) view.findViewById( R.id.amount_value );
        if(amount != null){
            amount.setText( String.valueOf( transaction.getProperty( R.string.amount_tag ) ));
            amount.setEnabled( isEditable );
            amount.setTextColor( Color.BLACK );
        }


        EditText note = (EditText) view.findViewById( R.id.note_value );
        if(note != null){
            note.setText( (String) transaction.getProperty( R.string.note_tag ));
            note.setEnabled( isEditable );
            note.setTextColor( Color.BLACK );
        }


        TextView scheduledDate = (TextView) view.findViewById( R.id.scheduled_date_value );
        if(scheduledDate != null) {
            scheduledDate.setText( ((ProjectedTransaction)transaction).getScheduledProjectionDate().format( DateTimeFormatter.ofPattern( "MMM dd" ) ));
        }

        TextView scheduledAmount = (TextView) view.findViewById( R.id.scheduled_amount_value );
        if(scheduledAmount != null) scheduledAmount.setText( String.valueOf(((ProjectedTransaction)transaction).getScheduledAmount() ));


        ViewGroup projections = (ViewGroup) view.findViewById( R.id.future_occurrences );
        if(projections != null) {
            ArrayList<T> storedProjections = budgetModel.getProjections( transaction );
            int[] columns = new int[]{R.string.date_tag, R.string.amount_tag};
            buildListView(projections.findViewById( R.id.future_occurrences_list ), storedProjections, columns);

            projections.findViewById( R.id.clear_projections_button ).setOnClickListener( v -> {
                transaction.setClearStoredProjectionsFlag(true);
                budgetModel.update( transaction );
                this.context.recreate();
            } );
        }

    }

    private void updateTransactionFromView(ViewGroup editorView, Transaction transaction){

        Button dateValue = (Button) editorView.findViewById( R.id.date_value );
        Object tagDate = dateValue.getTag(R.string.date_tag);
        if(dateValue != null) transaction.setProperty( R.string.date_tag, tagDate );

        EditText amountValue = (EditText) editorView.findViewById( R.id.amount_value );
        if(amountValue != null) transaction.setProperty( R.string.amount_tag, amountValue.getText().toString() );

        EditText noteValue = (EditText) editorView.findViewById( R.id.note_value );
        if(noteValue != null) transaction.setProperty( R.string.note_tag, noteValue.getText().toString() );

        EditText labelValue = (EditText) editorView.findViewById( R.id.label_value );
        if(labelValue != null) transaction.setProperty( R.string.label_tag, labelValue.getText().toString() );

        Button recurranceValue = (Button) editorView.findViewById( R.id.recurrance_value );
        if(recurranceValue != null) transaction.setProperty( R.string.recurrence_tag, recurranceValue.getTag(R.string.recurrence_tag).toString() );

    }

    private void reconcileTransaction(T transaction) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder( this.context );
        LinearLayout reconcileView = (LinearLayout) LinearLayout.inflate( this.context, R.layout.layout_reconcile_projection, null );

        setTransactionPropertyViews( reconcileView, transaction, true );

        dialogBuilder.setNegativeButton( "Cancel", (dialog, which) -> dialog.dismiss() );
        dialogBuilder.setPositiveButton( "Create Record", (dialog, which) -> {
            updateTransactionFromView( reconcileView, transaction );
            budgetModel.reconcile(transaction);
            dialog.dismiss();
            this.context.recreate();
        } );

        dialogBuilder.setView( reconcileView );
        dialogBuilder.show();
    }

    private void viewTransaction(T transaction, boolean editable){
        AlertDialog.Builder builder = new AlertDialog.Builder( this.context );
        ViewGroup dialogView;

        if(transaction instanceof ProjectedTransaction) dialogView = (ViewGroup) ViewGroup.inflate( this.context, R.layout.layout_projection, null );
        else if(transaction instanceof RecordedTransaction) dialogView = (ViewGroup) ViewGroup.inflate( this.context, R.layout.layout_record, null );
        else dialogView = (ViewGroup) ViewGroup.inflate( this.context, R.layout.layout_transaction, null );

        setTransactionPropertyViews(dialogView, transaction, editable);

        if(editable){
            builder.setPositiveButton( "Save", (dialog, which) -> {
                updateTransactionFromView(dialogView, transaction);
                budgetModel.update( transaction );
                dialog.dismiss();
                this.context.recreate();
            });

            builder.setNeutralButton( "Delete", (dialog, which) -> {
                budgetModel.delete( transaction );
                dialog.dismiss();
                this.context.recreate();
            } );
        } else {
            builder.setPositiveButton( "Edit", (dialog, which) -> {
                viewTransaction( transaction, true );
                dialog.dismiss();
            });
        }


        builder.setNegativeButton( "Close", (dialog, which) -> dialog.dismiss() );
        builder.setView( dialogView );
        builder.show();
    }

}
