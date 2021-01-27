package com.fudgetbudget.ui;

import android.app.DatePickerDialog;
import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import com.fudgetbudget.R;
import com.fudgetbudget.model.ProjectedTransaction;
import com.fudgetbudget.model.Transaction;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;

import static android.view.View.GONE;
import static android.view.View.TEXT_ALIGNMENT_CENTER;
import static android.view.View.VISIBLE;

public class ListItemViewBuilder {

    private final Context context;


    public ListItemViewBuilder(Context context) {
        this.context = context;
    }



//    void buildTransactionListView(ViewGroup listView, int layoutResource, Object dataObject){
//        ViewGroup listHeader = (ViewGroup) View.inflate(this.context, layoutResource, null);
//        Object listData;
//
//        setLineItemHeader(listHeader, layoutResource);
//        listView.addView( listHeader );
//
//
//        if(dataObject instanceof Integer){
//            Integer typeInt = (Integer) dataObject;
//            if( typeInt == R.string.transaction_type_income || typeInt == R.string.transaction_type_expense)
//                listData = budgetModel.getTransactionsByType( dataObject );
//            else if( typeInt == R.string.records_type_all )
//                listData = budgetModel.getRecords( dataObject );
//            else throw new Error("unknown data type resorce identifier");
//
//        } else listData = dataObject;
//
//        if( listData instanceof List){
//            List<?> listDataSet = (List) listData;
//
//            for( Object item : listDataSet){
//                ViewGroup listLineItem = (ViewGroup) View.inflate( this.context, layoutResource, null );
//                buildListItem( listLineItem, R.layout.layout_transaction, (Transaction) item );
//
//                listView.addView( listLineItem );
//            }
//        }
//
//        else if( listData instanceof Map ){
//            Map<?,?> listDataMap = (Map) listData;
//            for(Map.Entry item : listDataMap.entrySet() ){
//                ViewGroup listLineItem = (ViewGroup) View.inflate( this.context, layoutResource, null );
//                if( !(item.getKey() instanceof Transaction)
//                        || !(item.getValue() instanceof Double)) throw new Error("unknown data type");
//                Transaction transaction = (Transaction) item.getKey();
//                Double balance = (Double) item.getValue();
//
//                buildListItem( listLineItem, R.layout.layout_transaction, transaction );
//                setLineItemAlert( listLineItem, transaction, balance );
//
//                TextView balanceView = listLineItem.findViewById( R.id.balance_value );
//                if(balanceView != null) balanceView.setText( formatUtility.formatCurrency( balance ));
//
//                listView.addView( listLineItem );
//            }
//        }
//
//        else throw new Error("unknown list data type");
//
//    }

//    void buildPeriodListView(ViewGroup periodListView, int periodItemLayout, Object dataObject){
//        Object listData;
//
//
//        if(dataObject instanceof Integer){
//            Integer typeInt = (Integer) dataObject;
//            if( typeInt == R.string.balancesheet_projections ) listData = budgetModel.getProjectionsWithBalancesByPeriod();
//            //else if( typeInt == R.string.records_by_period ) listData = budgetModel.getRecordsByPeriod();
//            else throw new Error("unknown data type resorce identifier");
//
//        } else listData = dataObject;
//
//
//        if( listData instanceof List){
//            List<?> listDataSet = (List) listData;
//            for( Object item : listDataSet){
//                ViewGroup periodItemView;
//                Object[] periodData;
//                if( !(item instanceof Object[]) ) throw new Error("unknown data type");
//                else periodData = (Object[]) item;
//
//                LocalDate periodStartDate;
//                List periodBalanceData;
//                Map periodTransactionsWithBalance;
//
//                if( !(periodData[0] instanceof LocalDate) || !(periodData[1] instanceof List) || !(periodData[2] instanceof Map)) throw new Error( "unknown data type" );
//                else {
//                    periodStartDate = (LocalDate)periodData[0];
//                    periodBalanceData = (List)periodData[1];
//                    periodTransactionsWithBalance = (Map)periodData[2];
//                }
//
//                periodItemView = (ViewGroup) View.inflate( this.context, periodItemLayout, null );
//                setPeriodItemHeader( periodItemView.findViewById( R.id.period_header ), periodStartDate );
//                buildTransactionListView( periodItemView.findViewById( R.id.period_projection_list ), R.layout.list_item_transaction_with_balance, periodTransactionsWithBalance);
//                setPeriodItemBalances( periodItemView, periodBalanceData );
//
//
//                periodItemView.findViewById( R.id.period_projection_list ).setVisibility( VISIBLE );
//                periodItemView.findViewById( R.id.period_list_item_balances ).setVisibility( GONE );
//                periodItemView.setOnClickListener( v -> {
//                    View dataList = v.findViewById( R.id.period_projection_list );
//                    View balanceList = v.findViewById( R.id.period_list_item_balances );
//                    if(dataList.getVisibility() != VISIBLE){
//                        dataList.setVisibility( VISIBLE );
//                        balanceList.setVisibility( GONE );
//                    } else {
//                        dataList.setVisibility( GONE );
//                        balanceList.setVisibility( VISIBLE );
//                    }
//                } );
//                periodListView.addView( periodItemView );
//            }
//        } else throw new Error("unknown list data type");
//
//    }

//    void buildListItem(ViewGroup listLineItem, int cardLayoutResource, Transaction transaction) {
//
//        setLineItemPropertyViews( listLineItem, transaction );
////        initializeCardLayout( listLineItem, cardLayoutResource, transaction );
//
//        if(listLineItem.findViewById( R.id.card_view ) != null)
//            listLineItem.setOnClickListener( this::toggleCardVisibility );
//    }


    void setPeriodItemHeader(ViewGroup periodHeader, LocalDate periodBeginDate){
        TextView periodDateView = periodHeader.findViewById( R.id.period_header_date);
        periodDateView.setText( periodBeginDate.format( DateTimeFormatter.ofPattern( "MMMM yyyy" ) ) );

    }
    void setPeriodItemBalances(ViewGroup periodItem, List<Double> periodBalances){
        Double initialBal = periodBalances.get(0);
        Double endBal = periodBalances.get(3);
        Double income = periodBalances.get(1);
        Double expense = periodBalances.get(2);


        TextView startOfPeriodBalance = periodItem.findViewById( R.id.projection_period_header_initial_balance );
//                if(initialBal < 0) startOfPeriodBalance.setTextAppearance( R.style.balance_amount_negative );
//                else startOfPeriodBalance.setTextAppearance( R.style.balance_amount_not_negative );
        startOfPeriodBalance.setText( formatUtility.formatCurrency( initialBal ) );

        TextView endOfPeriodBalance = periodItem.findViewById( R.id.projection_period_header_ending_balance );
//        if(endBal < 0) endOfPeriodBalance.setTextColor( this.context.getColor( R.color.colorCurrencyNegative ));
//        else endOfPeriodBalance.setTextColor( this.context.getColor( R.color.colorCurrencyNotNegative ));
        endOfPeriodBalance.setText( formatUtility.formatCurrency( endBal ) );

        TextView periodIncome = periodItem.findViewById( R.id.projection_period_header_income);
//        periodIncome.setTextColor( this.context.getColor( R.color.colorCurrencyNotNegative ));
        periodIncome.setText( "+" + formatUtility.formatCurrency( income ));

        TextView periodExpenses = periodItem.findViewById( R.id.projection_period_header_expense );
//        periodExpenses.setTextColor( this.context.getColor( R.color.colorCurrencyNegative ));
        periodExpenses.setText( "-" + formatUtility.formatCurrency( expense ));

        TextView netGain = periodItem.findViewById( R.id.projection_period_header_net_gain );
        TextView netGainLabel = periodItem.findViewById( R.id.projection_period_header_net_gain_label );
        if(endBal < initialBal){
            netGainLabel.setText( "Loss" );
            netGain.setText("-" + formatUtility.formatCurrency( endBal - initialBal ));
//            netGain.setTextColor( this.context.getColor( R.color.colorCurrencyNegative ));
        } else {
            netGainLabel.setText( "Gain");
            netGain.setText("+" + formatUtility.formatCurrency( endBal - initialBal ));
//            netGain.setTextColor( this.context.getColor( R.color.colorCurrencyNotNegative ));
        }
    }
    void setPeriodItemBalances(ViewGroup periodItem, HashMap<String, Double> periodBalances){
        //use this method for setting the balances in the periodItem view
        //should be called by the balancesheet and records


        TextView currentBalanceView = periodItem.findViewById( R.id.current_recorded_balance );
        Double currentBalance = periodBalances.get( "current_recorded_balance" );
        if(currentBalanceView != null && currentBalance != null){
            currentBalanceView.setText( String.valueOf( currentBalance ) );
        }

        TextView periodIncomeView = periodItem.findViewById( R.id.period_income_balance );
        Double periodIncome = periodBalances.get( "period_income_balance" );
        if(periodIncomeView != null && periodIncome != null){
            periodIncomeView.setText( String.valueOf( periodIncome ) );

        }

        TextView periodExpenseView = periodItem.findViewById( R.id.period_expense_balance );
        Double periodExpense = periodBalances.get( "period_expense_balance" );
        if(periodExpenseView != null && periodExpense != null){
            periodExpenseView.setText( String.valueOf( periodExpense ) );
        }

        TextView periodNetChangeView = periodItem.findViewById( R.id.period_netchange_balance );
        Double periodNetChange = periodBalances.get( "period_netchange_balance" );
        if(periodNetChangeView != null && periodNetChange != null){
            periodNetChangeView.setText( String.valueOf( periodNetChange ) );
        }

//        TextView startingBalanceView = periodItem.findViewById( R.id.period_starting_balance );
//        Double startingBalance = periodBalances.get( "period_starting_balance" );
//        if(startingBalanceView != null && startingBalance != null){
//            startingBalanceView.setText( String.valueOf( startingBalance ) );
//        }
//
//        TextView endingBalanceView = periodItem.findViewById( R.id.period_ending_balance );
//        Double endingBalance = periodBalances.get( "period_ending_balance" );
//        if(endingBalanceView != null && endingBalance != null){
//            endingBalanceView.setText( String.valueOf( endingBalance ) );
//        }

    }

    public void setLineItemHeader(ViewGroup listHeader) {

        TextView dateHeader = listHeader.findViewById( R.id.line_item_date_value );
        if(dateHeader != null) {
            dateHeader.setText("Date");
            dateHeader.setTextAlignment( TEXT_ALIGNMENT_CENTER );
        }

        TextView labelHeader = listHeader.findViewById( R.id.line_item_label_value );
        if(labelHeader != null) {
            labelHeader.setText("Label");
            labelHeader.setTextAlignment( TEXT_ALIGNMENT_CENTER );
        }

        TextView amountHeader = listHeader.findViewById( R.id.line_item_amount_value );
        if(amountHeader != null) {
            amountHeader.setText("Amt");
            amountHeader.setTextAlignment( TEXT_ALIGNMENT_CENTER );
        }

        listHeader.setBackgroundResource( R.drawable.list_header_item );

        TextView balanceHeader = listHeader.findViewById( R.id.line_item_balance_value );
        if(balanceHeader != null) {
            balanceHeader.setText( "Bal" );
            balanceHeader.setTextAlignment( TEXT_ALIGNMENT_CENTER );
        }

        TextView recurrenceIcon = listHeader.findViewById( R.id.line_item_recurrence_short_value );
        if(recurrenceIcon != null){
            recurrenceIcon.setText( "Repeat" );
            recurrenceIcon.setTextAlignment( TEXT_ALIGNMENT_CENTER );
        }

        listHeader.setPadding( 12,6,12,6 );
    }

    public void setLineItemPropertyViews(ViewGroup listLineItem, Transaction transaction){

        //set line-item_date value
        TextView lineItemDate = listLineItem.findViewById( R.id.line_item_date_value );
        if(lineItemDate != null){
            LocalDate date = (LocalDate)transaction.getProperty( R.string.date_tag );
            Object formatTag = lineItemDate.getTag();
            String dateString;
            if(formatTag != null) dateString = formatUtility.formatDate( formatTag, date );
            else dateString = formatUtility.formatDate("", date);
            lineItemDate.setText( dateString );
        }

        TextView recurrenceIcon = listLineItem.findViewById( R.id.line_item_recurrence_short_value );
        if(recurrenceIcon != null){
            Object recurrenceValue = transaction.getProperty( R.string.recurrence_tag );
            if(recurrenceValue != null && recurrenceValue.toString().contentEquals( "0-0-0-0-0-0-0" ))
                recurrenceIcon.setText( "" );
            else {
                String[] recurrenceArray = ((String)transaction.getProperty( R.string.recurrence_tag )).split( "-" );
                recurrenceIcon.setText(formatUtility.formatRecurrenceShortValue( recurrenceArray ));
            }
        }

        //set line_item_label column
        TextView lineItemLabel = listLineItem.findViewById( R.id.line_item_label_value );
        if(lineItemLabel != null) lineItemLabel.setText( ((String)transaction.getProperty( R.string.label_tag )) );

        //set line_item_amount column
        TextView lineItemAmount = listLineItem.findViewById( R.id.line_item_amount_value );
        if(lineItemAmount != null){
            if(transaction.getIncomeFlag()) {
                lineItemAmount.setText( "$" + formatUtility.formatCurrency( transaction.getProperty( R.string.amount_tag )));
                lineItemAmount.setTextAppearance( R.style.income_text_style );
            }
            else {
                lineItemAmount.setText( "$" + formatUtility.formatCurrency(transaction.getProperty( R.string.amount_tag )));
                lineItemAmount.setTextAppearance( R.style.expense_text_style );
            }
        }

    }


    public void setCardLayoutPropertyViews(ViewGroup view, Transaction transaction, boolean isEditable){


//        if(isEditable) view.setBackgroundResource( R.drawable.editable_field );
//        else view.setBackgroundColor( this.context.getColor( android.R.color.white ));

        EditText labelEditor = view.findViewById( R.id.label_value );
        if(labelEditor != null){
            String label = String.valueOf( transaction.getProperty( R.string.label_tag ));
            labelEditor.setText( label );
            labelEditor.setEnabled( isEditable );

            if(isEditable) {
                labelEditor.setBackgroundResource( R.drawable.editable_field );
            }
            else {
                labelEditor.setBackgroundResource( R.drawable.display_field );
            }
        }

        EditText amount = view.findViewById( R.id.amount_value );
        if(amount != null){
            Double amountValue = (Double) transaction.getProperty( R.string.amount_tag );
            amount.setText( formatUtility.formatCurrency( amountValue ) );

            if(isEditable){
                amount.setEnabled( true );
                amount.setBackgroundResource( R.drawable.editable_field );
            } else {
                amount.setEnabled( false );
                amount.setBackgroundResource( R.drawable.display_field );
            }
        }

        EditText note = view.findViewById( R.id.note_value );
        if(note != null){
            note.setText( (String) transaction.getProperty( R.string.note_tag ));

            if(isEditable){
                note.setEnabled( true );
                note.setBackgroundResource( R.drawable.editable_field );
            } else {
                note.setEnabled( false );
                note.setBackgroundResource( R.drawable.display_field );
            }
        }

//        TextView scheduledDateView = (TextView) view.findViewById( R.id.scheduled_date_value );
//        if(scheduledDateView != null && transaction instanceof ProjectedTransaction) {
//            LocalDate scheduledDate = ((ProjectedTransaction)transaction).getScheduledProjectionDate();
//            scheduledDateView.setText( formatUtility.formatDate( R.string.date_string_short_md, scheduledDate ));
//        }
//
//        TextView scheduledAmountView = (TextView) view.findViewById( R.id.scheduled_amount_value );
//        if(scheduledAmountView != null && transaction instanceof ProjectedTransaction) {
//            Double scheduledAmount = ((ProjectedTransaction)transaction).getScheduledAmount();
//            scheduledAmountView.setText( formatUtility.formatCurrency( scheduledAmount ));
//        }

        TextView dateView = view.findViewById( R.id.date_value );
        if(dateView != null){
            LocalDate date = (LocalDate) transaction.getProperty( R.string.date_tag );
            Object formatTag = dateView.getTag();
            String dateString;
            if(formatTag != null && formatTag instanceof String) dateString = formatUtility.formatDate( (String)formatTag, date );
            else dateString = formatUtility.formatDate(R.string.date_string_long_mdy, date);

            if(date == null) {
                dateString = "unscheduled";
                date = LocalDate.now();
            }
            dateView.setText( dateString );
            dateView.setTag(R.string.date_tag, date);
            if(isEditable){
                dateView.setBackgroundResource( R.drawable.editable_field );
                LocalDate finalDate = date;
                dateView.setOnClickListener( dateButtonView -> {
                    final LocalDate[] newDate = {finalDate};
                    DatePickerDialog pickerDialog = new DatePickerDialog( this.context );
                    DatePicker picker = pickerDialog.getDatePicker();

                    picker.init( newDate[0].getYear(), newDate[0].getMonthValue()-1, newDate[0].getDayOfMonth(), (button_view, year, monthOfYear, dayOfMonth) -> {
                        newDate[0] = LocalDate.of( year, monthOfYear+1, dayOfMonth );
                    } );
                    pickerDialog.setButton( DatePickerDialog.BUTTON_POSITIVE, "OK", (dialog, which)->{
                        TextView dateButton = view.findViewById( R.id.date_value );
                        dateButton.setText( formatUtility.formatDate( R.string.date_string_long_mdy, newDate[0]));
                        dateButton.setTag(R.string.date_tag, newDate[0] );
                        dialog.dismiss();
                    } );

                    pickerDialog.setContentView( picker );
                    pickerDialog.show();
                } );
            } else {
                dateView.setBackgroundResource( R.drawable.display_field );
                dateView.setOnClickListener( null );
            }
        }

        TextView readableRecurrence = view.findViewById( R.id.readable_recurrence_value );
        if(readableRecurrence != null) {
            String[] recurrenceString = transaction.getProperty( R.string.recurrence_tag ).toString().split( "-" );
            LocalDate scheduledDate;
            if(transaction instanceof ProjectedTransaction) scheduledDate = ((ProjectedTransaction) transaction).getScheduledProjectionDate();
            else scheduledDate = (LocalDate) transaction.getProperty( R.string.date_tag );
            String readableValue = formatUtility.formatRecurrenceValue( recurrenceString, scheduledDate );
            readableRecurrence.setText( readableValue );

            if(isEditable) readableRecurrence.setBackgroundResource( R.drawable.editable_field );
            else readableRecurrence.setBackgroundResource( R.drawable.display_field );
        }


        Switch recurrenceSwitch = (Switch) view.findViewById( R.id.recurrence_switch );
        if(recurrenceSwitch != null) {
            RecurrenceViewBuilder recurrenceViewBuilder = new RecurrenceViewBuilder( this.context );
            recurrenceViewBuilder.setRecurrencePropertyViews( view, transaction, isEditable, recurrenceSwitch );

            if(!recurrenceSwitch.isChecked()){

            }

//            recurrenceSwitch.setOnClickListener( v -> {
//                View recurrenceEditor = view.findViewById( R.id.property_layout_recurrence_editor );
//                ViewGroup recurrenceParameters = recurrenceEditor.findViewById( R.id.set_recurrence_parameters );
//                if(recurrenceSwitch.isChecked())recurrenceParameters.setVisibility( VISIBLE );
//                else recurrenceParameters.setVisibility( GONE );
//            } );
        }

//        ViewGroup futureOccurrences = view.findViewById( R.id.transaction_projections_list );
//        if(futureOccurrences != null){
//            // for some reason when this code is run, the card_buttons loads into the projections_list at index 1
//
////            ArrayList<T> projections = budgetModel.getProjections( transaction );
////            int index = 0;
////            Collections.sort(projections);
////            for(T projection : projections){
////                View projectionItem = ViewGroup.inflate( this.context, R.layout.list_item_transaction, null );
////                setLineItemPropertyViews( (ViewGroup) projectionItem, projection );
////                futureOccurrences.addView( projectionItem, index );
////                index++;
////            }
//
//        }
    }

    public void updateTransactionFromEditorView(ViewGroup editorView, Transaction transaction){

        View dateValue = editorView.findViewById( R.id.date_value );
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

                TextView recurrenceDateButton = editorView.findViewById( R.id.recurrence_date_value );
                if(recurrenceDateButton != null) {
                    Object recurrenceTagDate = recurrenceDateButton.getTag( R.string.date_tag );
                    transaction.setProperty( R.string.date_tag, recurrenceTagDate );
                }

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

    public void toggleCardVisibility(View listItem) {
        ViewGroup cardV = listItem.findViewById( R.id.card_view );
        ViewGroup buttonsV = listItem.findViewById( R.id.card_buttons );

        if(cardV.getVisibility() != VISIBLE){
            listItem.setBackgroundResource(R.drawable.list_item_background_selected);
            buttonsV.setVisibility( VISIBLE );
            cardV.setVisibility( VISIBLE );
        } else {
            listItem.setBackgroundResource(R.drawable.list_item_background);
            buttonsV.setVisibility( GONE );
            cardV.setVisibility( GONE );
        }
    }

}
