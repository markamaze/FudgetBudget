package com.fudgetbudget;

import android.app.DatePickerDialog;
import android.graphics.Color;
import android.graphics.drawable.Icon;
import android.view.ContextThemeWrapper;
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
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;

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
    void buildTransactionListView(ViewGroup listView, ArrayList<T> listData, int[] listColumns){
        ViewGroup listHeader = (ViewGroup) View.inflate(this.context, R.layout.transaction_list_item, null);

        TextView dateHeader = listHeader.findViewById( R.id.date_value );
        TextView labelHeader = listHeader.findViewById( R.id.label_value );
        TextView amountHeader = listHeader.findViewById( R.id.amount_value );

        dateHeader.setText("Date");
        labelHeader.setText("Label");
        amountHeader.setText("Amt");

        dateHeader.setTextAppearance( R.style.period_list_item_header );
        labelHeader.setTextAppearance( R.style.period_list_item_header );
        amountHeader.setTextAppearance( R.style.period_list_item_header );

        listHeader.setBackgroundResource( R.drawable.list_header_item );
        listView.addView( listHeader );



        for( T transaction : listData ){
            ViewGroup listLineItem = (ViewGroup) View.inflate( this.context, R.layout.transaction_list_item, null );

            if(Arrays.stream(listColumns).anyMatch(i -> i == R.string.date_tag)){
                TextView dateView = listLineItem.findViewById( R.id.date_value );

                LocalDate date = (LocalDate)transaction.getProperty( R.string.date_tag );
                if(date == null) dateView.setText( "-" );
                else dateView.setText( formatDate( R.string.date_long_dowmd, date) );
            }

            if(Arrays.stream(listColumns).anyMatch(i -> i == R.string.label_tag )){
                TextView labelView = listLineItem.findViewById( R.id.label_value );

                String label = (String) transaction.getProperty( R.string.label_tag );
                labelView.setText( label );
            }

            if(Arrays.stream(listColumns).anyMatch(i -> i == R.string.recurrence_tag )){

            }

            if(Arrays.stream(listColumns).anyMatch(i -> i == R.string.amount_tag )){
                TextView amountView = listLineItem.findViewById( R.id.amount_value );

                amountView.setText( formatCurrency( transaction.getProperty( R.string.amount_tag )) );
                if(transaction.getIncomeFlag()) amountView.setTextAppearance( R.style.balance_amount_not_negative );
                else amountView.setTextAppearance( R.style.balance_amount_negative );
            }

            listLineItem.setOnClickListener( view -> viewTransaction( transaction, false ) );
            listView.addView( listLineItem );
        }
    }

    //builds list view for period balance projections
    void buildPeriodProjectionListView(ViewGroup listView, ArrayList<Object[]> listData){
//        LinearLayout listBody = listView.findViewWithTag( "list_body" );


        boolean firstPeriodLoaded = false; //using this as a hack to deal with inaccurate income/expense sums on the first period projected

        for(Object listItem : listData){
            ViewGroup listLineItem = (ViewGroup)LinearLayout.inflate( this.context, R.layout.period_list_item, null );

            Object[] entry = (Object[]) listItem;
            LocalDate periodBeginDate = (LocalDate) entry[0];
            ArrayList<Double> periodBalances = (ArrayList<Double>) entry[1];
            HashMap<T, Double> periodProjections = (HashMap<T, Double>) entry[2];
            if(periodBeginDate.isAfter( LocalDate.now() )) firstPeriodLoaded = true;


            //set period date
            TextView periodDate = listLineItem.findViewById( R.id.projection_period_header_date_month );
            periodDate.setText( periodBeginDate.format( DateTimeFormatter.ofPattern( "MMMM yyyy" ) ) );

            if(!firstPeriodLoaded) {
//                ((LinearLayout)listLineItem.findViewById( R.id.projection_period_header_balance_layout )).removeAllViewsInLayout();
//                ((LinearLayout)listLineItem.findViewById( R.id.period_header_expendable_line )).setVisibility( View.GONE );
                listLineItem.removeView( listLineItem.findViewById( R.id.period_list_item_balances ));
                listLineItem.findViewById( R.id.period_projection_list ).setVisibility( View.VISIBLE );
//                listLineItem.findViewById( R.id.period_date_expense_header ).setBackgroundResource( R.drawable.background_border_bottom );
            }
            else {
                Double initialBal = periodBalances.get(0);
                Double endBal = periodBalances.get(3);
                Double income = periodBalances.get(1);
                Double expense = periodBalances.get(2);


                TextView startOfPeriodBalance = listLineItem.findViewById( R.id.projection_period_header_initial_balance );
                if(initialBal < 0) startOfPeriodBalance.setTextAppearance( R.style.balance_amount_negative );
                else startOfPeriodBalance.setTextAppearance( R.style.balance_amount_not_negative );
                startOfPeriodBalance.setText( formatCurrency( initialBal ) );

                TextView endOfPeriodBalance = listLineItem.findViewById( R.id.projection_period_header_ending_balance );
                if(endBal < 0) endOfPeriodBalance.setTextAppearance( R.style.balance_amount_negative );
                else endOfPeriodBalance.setTextAppearance( R.style.balance_amount_not_negative );
                endOfPeriodBalance.setText( formatCurrency( endBal ) );

                TextView periodIncome = listLineItem.findViewById( R.id.projection_period_header_income);
                periodIncome.setTextAppearance( R.style.balance_amount_not_negative );
                periodIncome.setText( "+" + formatCurrency( income ));

                TextView periodExpenses = listLineItem.findViewById( R.id.projection_period_header_expense );
                periodExpenses.setTextAppearance( R.style.balance_amount_negative );
                periodExpenses.setText( "-" + formatCurrency( expense ));

                TextView netGain = listLineItem.findViewById( R.id.projection_period_header_net_gain );
                if(endBal < initialBal){
                    netGain.setText("-" + formatCurrency( endBal - initialBal ));
                    netGain.setTextAppearance( R.style.balance_amount_negative );
                } else {
                    netGain.setText("+" + formatCurrency( endBal - initialBal ));
                    netGain.setTextAppearance( R.style.balance_amount_not_negative );
                }
            }



            buildTransactionWithBalanceListView( listLineItem.findViewById( R.id.period_projection_list ), periodProjections );

            listLineItem.setOnClickListener( v -> {
                View projectionList = listLineItem.findViewById( R.id.period_projection_list );
                int visible = projectionList.getVisibility();

                if(visible == View.VISIBLE) projectionList.setVisibility( View.GONE );
                else projectionList.setVisibility( View.VISIBLE );
            } );




            listView.addView( listLineItem );
        }
    }

    //builds list view for projected transactions with resulting balance
    private void buildTransactionWithBalanceListView(ViewGroup listView, HashMap<T, Double> listData){

        ViewGroup listHeader = (ViewGroup) View.inflate(this.context, R.layout.transaction_list_item_with_balance, null);
        TextView dateHeader = listHeader.findViewById( R.id.date_value );
        TextView labelHeader = listHeader.findViewById( R.id.label_value );
        TextView amountHeader = listHeader.findViewById( R.id.amount_value );
        TextView balanceHeader = listHeader.findViewById( R.id.balance_value );

        dateHeader.setText("Date");
        labelHeader.setText("Label");
        amountHeader.setText("Amt");
        balanceHeader.setText( "Bal" );

        dateHeader.setTextAppearance( R.style.period_list_item_header );
        labelHeader.setTextAppearance( R.style.period_list_item_header );
        amountHeader.setTextAppearance( R.style.period_list_item_header );
        balanceHeader.setTextAppearance( R.style.period_list_item_header );

        listHeader.setBackgroundResource( R.drawable.list_header_item );
        listView.addView( listHeader );

        for(Map.Entry<T, Double> listItem : listData.entrySet()){
            ViewGroup listLineItem = (ViewGroup) View.inflate( this.context, R.layout.transaction_list_item_with_balance, null );

            T transaction = listItem.getKey();
            Double balance = listItem.getValue();


//            set reconcile action button
            Button reconcileTransactionButton = listLineItem.findViewById( R.id.reconcile_transaction_button );
            if(( (LocalDate) transaction.getProperty( R.string.date_tag )).isBefore( LocalDate.now().plusDays( 1 ) )){
                reconcileTransactionButton.setVisibility( View.VISIBLE );
                reconcileTransactionButton.setOnClickListener( v -> reconcileTransaction( transaction ) );
            } else reconcileTransactionButton.setVisibility( View.GONE );

            //set date column
            TextView dateValue = listLineItem.findViewById( R.id.date_value );
            LocalDate date = (LocalDate)transaction.getProperty( R.string.date_tag );
            dateValue.setText( formatDate( R.string.date_string_dayofweekanddate, date));


            //set label column
            TextView labelValue = listLineItem.findViewById( R.id.label_value );
            labelValue.setText( ((String)transaction.getProperty( R.string.label_tag )) );


            //set amount column
            TextView amount = listLineItem.findViewById( R.id.amount_value );
            if(transaction.getIncomeFlag()) {
                amount.setText( "+ " + formatCurrency( transaction.getProperty( R.string.amount_tag )));
                amount.setTextAppearance( R.style.balance_amount_not_negative );
            }
            else {
                amount.setText( "- " + formatCurrency(transaction.getProperty( R.string.amount_tag )));
                amount.setTextAppearance( R.style.balance_amount_negative );
            }


            //set balance column
            TextView balanceView = listLineItem.findViewById( R.id.balance_value );
            balanceView.setText( formatCurrency( balance ));
            if(balance >= 0)balanceView.setTextAppearance( R.style.balance_amount_not_negative );
            else balanceView.setTextAppearance( R.style.balance_amount_negative );


            listLineItem.setOnClickListener( view -> viewTransaction( transaction, false ) );

            listView.addView( listLineItem );
        }
    }

    private void setTransactionPropertyViews(ViewGroup view, Transaction transaction, boolean isEditable){
        EditText labelEditor = (EditText) view.findViewById( R.id.label_value );
        if(labelEditor != null){
            String label = String.valueOf( transaction.getProperty( R.string.label_tag ));
            if(isEditable){
                labelEditor.setText( label );
                labelEditor.setEnabled( true );
                labelEditor.setTextAppearance( R.style.property_value_cell_editor );
            } else{
                labelEditor.setText( label );
                labelEditor.setEnabled( false );
                labelEditor.setTextAppearance( R.style.property_value_cell_display );
            }
        }

        EditText amount = (EditText) view.findViewById( R.id.amount_value );
        if(amount != null){
            Double amountValue = (Double) transaction.getProperty( R.string.amount_tag );
            if(isEditable){
                if(amountValue == 0.0) amount.setText("");
                else amount.setText(String.valueOf( amountValue ));
                amount.setEnabled( true );
                amount.setTextAppearance( R.style.property_value_cell_editor );

            } else{
                amount.setText( formatCurrency( amountValue ) );
                amount.setEnabled( false );
                amount.setTextAppearance( R.style.property_value_cell_display );
            }
        }

        EditText note = (EditText) view.findViewById( R.id.note_value );
        if(note != null){
            if(isEditable){
                note.setText( (String) transaction.getProperty( R.string.note_tag ));
                note.setEnabled( true );
                note.setTextAppearance( R.style.property_value_cell_editor );

            } else {
                note.setText( (String) transaction.getProperty( R.string.note_tag ));
                note.setEnabled( false );
                note.setTextAppearance( R.style.property_value_cell_display );
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
                dateEditorButton.setTextAppearance( R.style.property_value_cell_editor );


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
                dateEditorButton.setTextAppearance( R.style.property_value_cell_editor );
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
        AlertDialog.Builder builder = new AlertDialog.Builder( new ContextThemeWrapper( this.context, R.style.dialog_page_title ) );
        ViewGroup dialogView;

        if(transaction instanceof ProjectedTransaction) builder.setTitle( "Repeating Transaction" );
        else if(transaction instanceof RecordedTransaction) builder.setTitle( "Recorded Transaction" );
        else builder.setTitle("Transaction");

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
            return date.format( DateTimeFormatter.ofPattern( "eee MMMM dd, yyyy" ) );
        } 
        else if(return_type == R.string.date_string_short_md){
            return date.format( DateTimeFormatter.ofPattern( "MMM d" ) );
        }
        else if(return_type == R.string.date_long_dowmd) return date.format( DateTimeFormatter.ofPattern( "eee MMM d"  ));
        else if(return_type == R.string.date_string_dayofweekanddate) return date.format(DateTimeFormatter.ofPattern( "d - eee" ));

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
