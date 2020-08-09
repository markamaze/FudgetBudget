package com.fudgetbudget;

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
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
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

                amountView.setText( formatCurrency( transaction.getProperty( R.string.amount_tag )) );
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


        boolean firstPeriodLoaded = false; //using this as a hack to deal with inaccurate income/expense sums on the first period projected

        for(Object listItem : listData){
            LinearLayout listLineItem = (LinearLayout) LinearLayout.inflate( this.context, R.layout.projection_period_header, null );

            Object[] entry = (Object[]) listItem;
            LocalDate periodBeginDate = (LocalDate) entry[0];
            ArrayList<Double> periodBalances = (ArrayList<Double>) entry[1];
            HashMap<T, Double> periodProjections = (HashMap<T, Double>) entry[2];
            if(periodBeginDate.isAfter( LocalDate.now() )) firstPeriodLoaded = true;


            //set period date
            TextView periodMonth = listLineItem.findViewById( R.id.projection_period_header_date_month );
            TextView periodYear = listLineItem.findViewById( R.id.projection_period_header_date_year );
            periodMonth.setText( periodBeginDate.format( DateTimeFormatter.ofPattern( "MMMM" ) ) );
            periodYear.setText( periodBeginDate.format( DateTimeFormatter.ofPattern( "yyyy" ) ) );

            if(!firstPeriodLoaded) ((LinearLayout)listLineItem.findViewById( R.id.projection_period_header_balance_layout )).removeAllViewsInLayout();
            else {
                Double initialBal = periodBalances.get(0);
                Double endBal = periodBalances.get(3);
                Double income = periodBalances.get(1);
                Double expense = periodBalances.get(2);


                TextView startOfPeriodBalance = listLineItem.findViewById( R.id.projection_period_header_initial_balance );
                startOfPeriodBalance.setText( formatCurrency( initialBal ) );

                TextView endOfPeriodBalance = listLineItem.findViewById( R.id.projection_period_header_ending_balance );
                endOfPeriodBalance.setText( formatCurrency( endBal ));

                TextView periodIncome = listLineItem.findViewById( R.id.projection_period_header_income);
                periodIncome.setText( "+ " + formatCurrency( income ));

                TextView periodExpenses = listLineItem.findViewById( R.id.projection_period_header_expense );
                periodExpenses.setText( "- " + formatCurrency( expense ));

                TextView netGain = listLineItem.findViewById( R.id.projection_period_header_net_gain );
                if(endBal > initialBal){
                    netGain.setText("+ " + formatCurrency( endBal - initialBal ));
                    netGain.setTextAppearance( R.style.line_item_cell_amount_income );
                } else if(endBal < initialBal){
                    netGain.setText("- " + formatCurrency( endBal - initialBal ));
                    netGain.setTextAppearance( R.style.line_item_cell_amount_expense );
                } else netGain.setText( " - " );
            }




            //add list of all projections for the period, with running balances
            if(!firstPeriodLoaded){
                LinearLayout expandedList = (LinearLayout) View.inflate( this.context, R.layout.view_list, null );
                buildListView( expandedList, periodProjections );
                listLineItem.addView( expandedList );
            }

            listLineItem.setOnClickListener( v -> {

                LinearLayout expandedList = v.findViewById( R.id.list );

                if(expandedList == null){
                    expandedList = (LinearLayout) View.inflate( this.context, R.layout.view_list, null );
                    buildListView( expandedList, periodProjections );
                    listLineItem.addView( expandedList );
                }
                else listLineItem.removeView( expandedList );
            } );




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

            //set amount column
            LinearLayout amountWrapper = (LinearLayout) View.inflate( this.context, R.layout.view_list_lineitem_cell, null );
            TextView amount = amountWrapper.findViewById( R.id.cell_value );
            if(transaction.getIncomeFlag()) {
                amount.setText( "+ " + formatCurrency( transaction.getProperty( R.string.amount_tag )));
                amount.setTextAppearance( R.style.line_item_cell_amount_income );
            }
            else {
                amount.setText( "- " + formatCurrency(transaction.getProperty( R.string.amount_tag )));
                amount.setTextAppearance( R.style.line_item_cell_amount_expense );
            }
            amount.setTextSize( 12 );
            listLineItemBody.addView( amountWrapper );


            //set balance column
            LinearLayout balanceWrapper = (LinearLayout) View.inflate( this.context, R.layout.view_list_lineitem_cell, null );
            TextView balanceView = balanceWrapper.findViewById( R.id.cell_value );
            balanceView.setText( formatCurrency( balance ));
            balanceView.setTextAppearance( R.style.line_item_cell_balance );
            listLineItemBody.addView( balanceWrapper );


            listLineItemBody.setOnClickListener( view -> viewTransaction( transaction, false ) );

            listBody.addView( listLineItem );
        }
    }

    private void setTransactionPropertyViews(ViewGroup view, Transaction transaction, boolean isEditable){
        EditText labelEditor = (EditText) view.findViewById( R.id.label_value );
        if(labelEditor != null){
            String label = String.valueOf( transaction.getProperty( R.string.label_tag ));
            if(isEditable){
                labelEditor.setText( label );
                labelEditor.setEnabled( true );
                labelEditor.setBackgroundColor( this.context.getColor( android.R.color.background_light ));
            } else{
                labelEditor.setText( label );
                labelEditor.setEnabled( false );
                labelEditor.setBackgroundColor( this.context.getColor( R.color.colorPrimaryLight ));
            }
        }

        EditText amount = (EditText) view.findViewById( R.id.amount_value );
        if(amount != null){
            Double amountValue = (Double) transaction.getProperty( R.string.amount_tag );
            if(isEditable){
                if(amountValue == 0.0) amount.setText("");
                else amount.setText(String.valueOf( amountValue ));
                amount.setEnabled( true );
                amount.setBackgroundColor( this.context.getColor( android.R.color.background_light ) );
            } else{
                amount.setText( formatCurrency( amountValue ) );
                amount.setEnabled( false );
                amount.setBackgroundColor( this.context.getColor( R.color.colorPrimaryLight ) );
            }
        }

        EditText note = (EditText) view.findViewById( R.id.note_value );
        if(note != null){
            if(isEditable){
                note.setText( (String) transaction.getProperty( R.string.note_tag ));
                note.setEnabled( true );
                note.setBackgroundColor( this.context.getColor( android.R.color.background_light ) );
            } else {
                note.setText( (String) transaction.getProperty( R.string.note_tag ));
                note.setEnabled( false );
                note.setBackgroundColor( this.context.getColor( R.color.colorPrimaryLight ) );
            }

        }

        TextView scheduledDateView = (TextView) view.findViewById( R.id.scheduled_date_value );
        if(scheduledDateView != null) {
            LocalDate scheduledDate = ((ProjectedTransaction)transaction).getScheduledProjectionDate();
            scheduledDateView.setText( formatDate( R.string.date_string_short_md, scheduledDate ));
        }

        TextView scheduledAmountView = (TextView) view.findViewById( R.id.scheduled_amount_value );
        if(scheduledAmountView != null) {
            Double scheduledAmount = ((ProjectedTransaction)transaction).getScheduledAmount();
            scheduledAmountView.setText( formatCurrency( scheduledAmount ));
        }



        Button dateEditorButton = (Button) view.findViewById( R.id.date_value );
        if(dateEditorButton != null){
            LocalDate date = (LocalDate) transaction.getProperty( R.string.date_tag );
            String dateString = formatDate(R.string.date_string_long_mdy, date);

            if(date == null) {
                dateString = "unscheduled";
                date = LocalDate.now();
            }

            if(isEditable){
                dateEditorButton.setText( dateString );
                dateEditorButton.setTag(R.string.date_tag, date);
                dateEditorButton.setEnabled( true );
                dateEditorButton.setBackgroundColor( this.context.getColor( android.R.color.background_light ) );

                LocalDate finalDate = date;
                dateEditorButton.setOnClickListener( dateButtonView -> {
                    final LocalDate[] newDate = {finalDate};
                    DatePickerDialog pickerDialog = new DatePickerDialog( this.context );
                    DatePicker picker = pickerDialog.getDatePicker();

                    picker.init( newDate[0].getYear(), newDate[0].getMonthValue()-1, newDate[0].getDayOfMonth(), (button_view, year, monthOfYear, dayOfMonth) -> {
                        newDate[0] = LocalDate.of( year, monthOfYear+1, dayOfMonth );
                    } );
                    pickerDialog.setButton( DatePickerDialog.BUTTON_POSITIVE, "OK", (dialog, which)->{
                        dateEditorButton.setText( formatDate( R.string.date_string_long_mdy, newDate[0]));
                        dateEditorButton.setTag(R.string.date_tag, newDate[0] );
                        dialog.dismiss();
                    } );

                    pickerDialog.setContentView( picker );
                    pickerDialog.show();
                } );

            }else {
                dateEditorButton.setText( formatDate(R.string.date_string_long_mdy, date) );
                dateEditorButton.setTag(R.string.date_tag, date);
                dateEditorButton.setEnabled( false );
                dateEditorButton.setBackgroundColor( this.context.getColor( R.color.colorPrimaryLight ) );
            }
        }


        Switch recurrenceSwitch = (Switch) view.findViewById( R.id.recurrence_switch );
        if(recurrenceSwitch != null) setRecurrencePropertyViews( view, transaction, isEditable, recurrenceSwitch );

    }

    private void setRecurrencePropertyViews(ViewGroup view, Transaction transaction, boolean isEditable, Switch recurrenceSwitch) {
        String[] recurrenceValues = transaction.getProperty( R.string.recurrence_tag ).toString().split( "-" );
        int recurrenceBit = Integer.parseInt( recurrenceValues[0] );
        LocalDate date = (LocalDate) transaction.getProperty( R.string.date_tag );


        //set recurrenceSwitch values and handlers
        if(recurrenceBit == 1) recurrenceSwitch.setChecked( true );
        else recurrenceSwitch.setChecked( false );
        setVisibility(view, isEditable, recurrenceBit);
        recurrenceSwitch.setOnCheckedChangeListener( (buttonView, isChecked) -> {
            buttonView.setChecked( isChecked );
            if(isChecked) setVisibility( view, isEditable, 1 );
            else setVisibility( view, isEditable, 0 );

        });

        TextView readableRecurrence = view.findViewById( R.id.readable_recurrence_value );
        readableRecurrence.setText( formatRecurrenceValue(recurrenceValues) );

        //set frequency values and handlers
        EditText frequencyEditor = view.findViewById( R.id.recurrence_frequency_value );
        if(recurrenceValues[1].contentEquals( "0" )) frequencyEditor.setText("");
        else frequencyEditor.setText( recurrenceValues[1]);


        //set onDay Type
        Spinner onDaySpinner = view.findViewById( R.id.on_day_type_selector );
        int periodValueBit = Integer.parseInt( recurrenceValues[2]);
        if(onDaySpinner != null){
            if(periodValueBit == 0 || periodValueBit == 1) onDaySpinner.setVisibility( View.GONE );
            else if(periodValueBit == 2){
                String[] onDayTypeWeekday = this.context.getResources().getStringArray( R.array.days_of_week_options );
                ArrayAdapter<String> adapter1 = new ArrayAdapter<>( this.context, android.R.layout.simple_list_item_1, onDayTypeWeekday );
                onDaySpinner.setAdapter( adapter1 );
                onDaySpinner.setEnabled( false );
                onDaySpinner.setSelection( date.getDayOfWeek().getValue() );
            }
            else if(periodValueBit == 3){
                String[] onDayTypeDayOfMonth = getOnDayOfMonthList(date);
                ArrayAdapter<String> adapter1 = new ArrayAdapter<>( this.context, android.R.layout.simple_list_item_1, onDayTypeDayOfMonth );
                onDaySpinner.setAdapter( adapter1 );
                onDaySpinner.setSelection( 0 );

            }
            else if(periodValueBit == 4){
                String[] onDayTypeDayOfYear = new String[]{};
                ArrayAdapter<String> adapter1 = new ArrayAdapter<>( this.context, android.R.layout.simple_list_item_1, onDayTypeDayOfYear );
                onDaySpinner.setAdapter( adapter1 );
                onDaySpinner.setSelection( 0 );
            }
        }
        //set period values and handlers
        //      set the selected item in spinner, set visibility and content of onType Listener (setting onType Listener will require using the date to reset the list)
        Spinner periodSpinner = view.findViewById( R.id.recurrence_period_value );
        String[] periodList = this.context.getResources().getStringArray( R.array.date_periods );
        ArrayAdapter<String> adapter = new ArrayAdapter<>( ViewModel.this.context, android.R.layout.simple_list_item_1, periodList );
        adapter.setDropDownViewResource( android.R.layout.simple_list_item_1 );
        periodSpinner.setAdapter( adapter );
        periodSpinner.setSelection( periodValueBit );

        //adjust onDay Spinner based on period
        setOnDaySpinner( date, onDaySpinner, periodValueBit );


        periodSpinner.setOnItemSelectedListener( new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                //when I change the selected Item I need to:
                //  determine the display of the onDay type: both visibility and content
                Spinner onDaySpinnerParent = (Spinner) parent;
                onDaySpinner.setEnabled( true );
                onDaySpinner.setVisibility( View.VISIBLE );
                if(parent != null){
                    if(position == 0 ) {
                        LinearLayout onDayLayout = ViewModel.this.context.findViewById( R.id.on_day_type_layout );
                        if(onDayLayout != null) onDayLayout.setVisibility( View.GONE );
                    }
                    else if(position == 1){
                        String[] onDayTypeWeekday = ViewModel.this.context.getResources().getStringArray( R.array.days_of_week_options );
                        ArrayAdapter<String> adapter1 = new ArrayAdapter<>( ViewModel.this.context, android.R.layout.simple_list_item_1, onDayTypeWeekday );
                        onDaySpinner.setAdapter( adapter1 );
                        onDaySpinner.setEnabled( false );
                        onDaySpinner.setSelection( date.getDayOfWeek().getValue() );
                    }
                    else if(position == 2){
                        String[] onDayTypeDayOfMonth = getOnDayOfMonthList(date);
                        ArrayAdapter<String> adapter1 = new ArrayAdapter<>( ViewModel.this.context, android.R.layout.simple_list_item_1, onDayTypeDayOfMonth );
                        onDaySpinner.setAdapter( adapter1 );
                        onDaySpinner.setSelection( 0 );
                    }
                    else if(position == 3){
                        String[] onDayTypeDayOfYear = new String[]{};
                        ArrayAdapter<String> adapter1 = new ArrayAdapter<>( ViewModel.this.context, android.R.layout.simple_list_item_1, onDayTypeDayOfYear );
                        onDaySpinner.setAdapter( adapter1 );
                        onDaySpinner.setSelection( 0 );
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        } );

        //set recurrence date and handlers
        //      set the value in the display, and reset the list and selection of the onDay Type
        Button dateButton = view.findViewById( R.id.recurrence_date_value );
        dateButton.setText(formatDate( R.string.date_string_long_mdy, date ));
        dateButton.setTag(R.string.date_tag, date);

        dateButton.setOnClickListener( dateButtonView -> {
            final LocalDate[] newDate = {date};
            DatePickerDialog pickerDialog = new DatePickerDialog( ViewModel.this.context );
            DatePicker picker = pickerDialog.getDatePicker();

            picker.init( newDate[0].getYear(), newDate[0].getMonthValue()-1, newDate[0].getDayOfMonth(), (button_view, year, monthOfYear, dayOfMonth) -> {
                newDate[0] = LocalDate.of( year, monthOfYear+1, dayOfMonth );
            } );
            pickerDialog.setButton( DatePickerDialog.BUTTON_POSITIVE, "OK", (dialog, which)->{
                dateButton.setText( newDate[0].format( DateTimeFormatter.ofPattern( "MMMM dd, yyyy" ) ));
                dateButton.setTag(R.string.date_tag, newDate[0] );

                int periodPos = ((Spinner)view.findViewById( R.id.recurrence_period_value )).getSelectedItemPosition();
                setOnDaySpinner( newDate[0], onDaySpinner, periodPos );

                dialog.dismiss();
            } );

            pickerDialog.setContentView( picker );
            pickerDialog.show();
        } );






//        //set end parameters... May hold off until a future update. not crucial and this is taking forever
//        Switch setEndParametersSwitch = view.findViewById( R.id.set_end_parameters_switch );
//        setEndParametersSwitch.setChecked( Integer.parseInt(recurrenceValues[4]) == 1 );
////            setEndParametersSwitch.setOnCheckedChangeListener( getRecurringStopSwitchChangeListener(view, transaction, isEditable) );
//
//        EditText stopAfterNumOccurrences = view.findViewById( R.id.after_number_of_occurrences_value );
//        Button stopAfterDate = view.findViewById( R.id.after_date_value );
//        EditText stopAfterTotal = view.findViewById( R.id.when_total_reaches_amount_value );


        //set projections list dropdown
//        ViewGroup projections = (ViewGroup) view.findViewById( R.id.future_occurrences_list );
//        if(projections != null) {
//            ArrayList<T> storedProjections = budgetModel.getProjections( transaction );
//            int[] columns = new int[]{R.string.date_tag, R.string.amount_tag};
//            buildListView(projections.findViewById( R.id.future_occurrences_list ), storedProjections, columns);
//
//            projections.findViewById( R.id.clear_projections_button ).setOnClickListener( v -> {
//                transaction.setClearStoredProjectionsFlag(true);
//                budgetModel.update( transaction );
//                this.context.recreate();
//            } );
//        }

    }

    private void setOnDaySpinner(LocalDate date, Spinner onDaySpinner, int periodValueBit) {
        if(onDaySpinner != null){
            if(periodValueBit == 0 || periodValueBit == 1) onDaySpinner.setVisibility( View.GONE );
            else if(periodValueBit == 1){
                String[] onDayTypeWeekday = this.context.getResources().getStringArray( R.array.days_of_week_options );
                ArrayAdapter<String> adapter1 = new ArrayAdapter<>( this.context, android.R.layout.simple_list_item_1, onDayTypeWeekday );
                onDaySpinner.setAdapter( adapter1 );
                onDaySpinner.setEnabled( false );
                onDaySpinner.setSelection( date.getDayOfWeek().getValue() );
            }
            else if(periodValueBit == 2){
                String[] onDayTypeDayOfMonth = getOnDayOfMonthList(date);
                ArrayAdapter<String> adapter1 = new ArrayAdapter<>( this.context, android.R.layout.simple_list_item_1, onDayTypeDayOfMonth );
                onDaySpinner.setAdapter( adapter1 );
                onDaySpinner.setSelection( 0 );

            }
            else if(periodValueBit == 3){
                String[] onDayTypeDayOfYear = new String[]{};
                ArrayAdapter<String> adapter1 = new ArrayAdapter<>( this.context, android.R.layout.simple_list_item_1, onDayTypeDayOfYear );
                onDaySpinner.setAdapter( adapter1 );
                onDaySpinner.setSelection( 0 );
            }
        }

    }

    private String[] getOnDayOfMonthList(LocalDate date) {
        String formattedDateOfMonth = formatDate(R.string.date_string_dateofmonth, date);
        String dateOfMonth = formattedDateOfMonth + " of each month";

        String formattedDayOfWeekInMonth = formatDate(R.string.date_string_dayofweekinmonth, date);
        String dayOfWeekInMonth = formattedDayOfWeekInMonth + " of each month";

        String[] dayOfArray = new String[]{dateOfMonth, dayOfWeekInMonth};

        return dayOfArray;
    }

    private void setVisibility(View view, boolean isEditable, int recurrenceBit) {
        if(recurrenceBit == 1) { //repeats
            view.findViewById( R.id.property_layout_recurrence_editor ).setVisibility( View.VISIBLE );
            view.findViewById( R.id.property_layout_date ).setVisibility( View.GONE );


            if(isEditable) {
                //repeats and is editable: show the recurrence editor with values set
                view.findViewById( R.id.property_layout_recurrence_editor ).setVisibility( View.VISIBLE );
                view.findViewById( R.id.property_layout_recurrence_switch ).setVisibility( View.VISIBLE );
                view.findViewById( R.id.property_layout_recurrence_readable ).setVisibility( View.GONE );
                view.findViewById( R.id.property_layout_recurrence_projections ).setVisibility( View.GONE );

            } else {
                //repeats and is not editable: show readable text representation of the recurrence value and next scheduled date

                view.findViewById( R.id.property_layout_recurrence_switch ).setVisibility( View.GONE );
                view.findViewById( R.id.property_layout_recurrence_editor ).setVisibility( View.GONE );
                view.findViewById( R.id.property_layout_recurrence_readable ).setVisibility( View.VISIBLE );
                view.findViewById( R.id.property_layout_recurrence_projections ).setVisibility( View.VISIBLE );
            }
        }
        else { //does not repeat
            view.findViewById( R.id.property_layout_recurrence_editor ).setVisibility( View.GONE );
            view.findViewById( R.id.property_layout_recurrence_projections ).setVisibility( View.GONE );
            view.findViewById( R.id.property_layout_date ).setVisibility( View.VISIBLE );


            if(isEditable) {
                //does not repeat, but can be edited: need to load the recurrence editor and remove scheduled date
                view.findViewById( R.id.property_layout_recurrence_switch ).setVisibility( View.VISIBLE );
            }
            else{
                //does not repeat and cannot edit: just show the date and remove recurrence editor
                //remove recurrenceSwitch
                view.findViewById( R.id.property_layout_recurrence_switch ).setVisibility( View.GONE );
            }

        }
    }

    private void updateTransactionFromView(ViewGroup editorView, Transaction transaction){

        Button dateValue = (Button) editorView.findViewById( R.id.date_value );
        Object tagDate = dateValue.getTag(R.string.date_tag);
        if(tagDate != null) transaction.setProperty( R.string.date_tag, tagDate );

        EditText amountValue = (EditText) editorView.findViewById( R.id.amount_value );
        if(amountValue != null) transaction.setProperty( R.string.amount_tag, amountValue.getText().toString() );

        EditText noteValue = (EditText) editorView.findViewById( R.id.note_value );
        if(noteValue != null) transaction.setProperty( R.string.note_tag, noteValue.getText().toString() );

        EditText labelValue = (EditText) editorView.findViewById( R.id.label_value );
        if(labelValue != null) transaction.setProperty( R.string.label_tag, labelValue.getText().toString() );

        Switch recurranceValue = (Switch) editorView.findViewById( R.id.recurrence_switch );
        if(recurranceValue != null) {
            String recurrenceBit = "0", frequency = "0", period = "0", dayType = "0", endRecurrenceBit = "0", endRecurrenceOption = "0", endRecurrenceParameter = "0";

            if(recurranceValue.isChecked()){
                recurrenceBit = "1";

                EditText frequencyValueView = editorView.findViewById( R.id.recurrence_frequency_value );
                frequency = String.valueOf(frequencyValueView.getText());

                Spinner periodValueView = editorView.findViewById( R.id.recurrence_period_value );
                period = String.valueOf( periodValueView.getSelectedItemPosition() );

                Spinner dayTypeView = editorView.findViewById( R.id.on_day_type_selector );
                dayType = String.valueOf( dayTypeView.getSelectedItemPosition() );

                Switch endParameterSwitch = editorView.findViewById( R.id.set_end_parameters_switch );
                if(endParameterSwitch.isChecked()){
                    endRecurrenceBit = "1";

                    RadioButton numberOfOccurrenceOption = editorView.findViewById( R.id.after_number_of_occurrences );
                    RadioButton afterDateOption = editorView.findViewById( R.id.after_date );
                    RadioButton afterTotalAmountOption = editorView.findViewById( R.id.when_total_reaches_amount );

                    if(numberOfOccurrenceOption.isChecked()){
                        endRecurrenceOption = "1";

                        EditText numberOfOccurrenceView = editorView.findViewById( R.id.after_number_of_occurrences_value );
                        String value = String.valueOf( numberOfOccurrenceView.getText() );
                        endRecurrenceParameter = value;
                    }
                    else if(afterDateOption.isChecked()){
                        endRecurrenceOption = "2";

                        Button afterDateView = editorView.findViewById( R.id.after_date_value );
                        LocalDate value = (LocalDate) afterDateView.getTag(R.string.date_tag);
                        endRecurrenceParameter = value.format( DateTimeFormatter.BASIC_ISO_DATE );
                    }
                    else if(afterTotalAmountOption.isChecked()){
                        endRecurrenceOption = "3";

                        EditText afterTotalAmountView = editorView.findViewById( R.id.when_total_reaches_amount_value );
                        String value = String.valueOf( afterTotalAmountView.getText() );
                        endRecurrenceParameter = value;
                    }
                }
            }

            String[] recurrenceStrings = new String[]{recurrenceBit, frequency, period, dayType, endRecurrenceBit, endRecurrenceOption, endRecurrenceParameter};
            transaction.setProperty( R.string.recurrence_tag, String.join( "-", recurrenceStrings ) );
        }

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

    String formatCurrency(Object currency){ return "$" + String.format( "%.0f", currency ); }
    String formatDate(int return_type, LocalDate date) {

        if(return_type == R.string.date_string_long_mdy){
            return date.format( DateTimeFormatter.ofPattern( "MMMM dd, yyyy" ) );
        } 
        else if(return_type == R.string.date_string_short_md){
            return date.format( DateTimeFormatter.ofPattern( "MM dd" ) );
        }
        else if(return_type == R.string.date_string_dayofweekinmonth){
            int dateOfMonth = date.getDayOfMonth();
            String dayOfWeek = date.getDayOfWeek().toString();
            int weekCounter = 1;
            int dateIndex = dateOfMonth;

            while(dateIndex > 7){
                dateIndex = dateIndex -7;
                weekCounter++;
            }

            if(weekCounter == 1) return "First " + dayOfWeek;
            else if(weekCounter == 2) return "Second " + dayOfWeek;
            else if(weekCounter == 3) return "Third " + dayOfWeek;
            else if(weekCounter == 4) return "Fourth " + dayOfWeek;
            else return "Last " + dayOfWeek;
        }
        else if(return_type == R.string.date_string_dateofmonth){
            int dateOfMonth = date.getDayOfMonth();
            if(dateOfMonth == 1) return "1st";
            else if(dateOfMonth == 2) return "2nd";
            else if(dateOfMonth == 3) return "3rd";
            else return String.valueOf( dateOfMonth ) + "th";


        }
        else return date.format( DateTimeFormatter.BASIC_ISO_DATE );
    }
    String formatRecurrenceValue(String[] recurrenceValues) {

        return "readable recurrence";
    }

}
