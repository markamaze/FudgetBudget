package com.fudgetbudget;

import android.app.DatePickerDialog;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import com.fudgetbudget.model.ProjectedTransaction;
import com.fudgetbudget.model.RecordedTransaction;
import com.fudgetbudget.model.Transaction;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

class ViewModel<T extends Transaction> {

    private final MainActivity context;
    private BudgetModel budgetModel;


    ViewModel(MainActivity context, BudgetModel budgetModel) {
        this.context = context;
        this.budgetModel = budgetModel;
    }


    View.OnClickListener createTransactionListener(int transaction_type_income) {
        return v -> {
            ViewParent parent = v.getParent().getParent();
            if(parent instanceof LinearLayout){
                LinearLayout incomeList = (LinearLayout) parent;

                T newTransaction = (T) Transaction.getInstance( transaction_type_income, this.context.getExternalFilesDir( null ).getAbsolutePath() );
                budgetModel.update( newTransaction );
                ViewGroup newListItem = (ViewGroup) View.inflate( this.context, R.layout.transaction_list_item, null );
                setLineItemPropertyViews( newListItem, newTransaction );

                initializeCardLayout( newListItem, newTransaction );
                initializeCardButtons( newListItem, newTransaction );

                newListItem.findViewById( R.id.card_view ).setVisibility( VISIBLE );
                newListItem.findViewById( R.id.card_buttons ).setVisibility( VISIBLE );
                newListItem.findViewById( R.id.modify_transaction_button ).callOnClick();

                newListItem.setOnClickListener( v1 -> toggleCardVisibility( newListItem ) );
                incomeList.addView( newListItem, 2 );
            }

        };
    }

    void buildTransactionListView(ViewGroup listView, int layoutResource, Object dataObject){
        ViewGroup listHeader = (ViewGroup) View.inflate(this.context, layoutResource, null);
        Object listData;

        setLineItemHeader(listHeader, layoutResource);
        listView.addView( listHeader );


        if(dataObject instanceof Integer){
            Integer typeInt = (Integer) dataObject;
            if( typeInt == R.string.transaction_type_income || typeInt == R.string.transaction_type_expense)
                listData = budgetModel.getTransactionsByType( dataObject );
            else if( typeInt == R.string.records_type_all )
                listData = budgetModel.getRecords( dataObject );
            else throw new Error("unknown data type resorce identifier");

        } else listData = dataObject;

        if( listData instanceof List){
            List<?> listDataSet = (List) listData;

            for( Object item : listDataSet){
                ViewGroup listLineItem = (ViewGroup) View.inflate( this.context, layoutResource, null );
                if( !(item instanceof Transaction) ) throw new Error("unknown data type");
                Transaction transaction = (Transaction) item;

                setLineItemPropertyViews( listLineItem, transaction );

                initializeCardLayout( listLineItem, transaction );
                initializeCardButtons( listLineItem, transaction );

                if(listLineItem.findViewById( R.id.card_view ) != null)
                    listLineItem.setOnClickListener( this::toggleCardVisibility );

                listView.addView( listLineItem );
            }
        }

        else if( listData instanceof Map ){
            Map<?,?> listDataMap = (Map) listData;
            for(Map.Entry item : listDataMap.entrySet() ){
                ViewGroup listLineItem = (ViewGroup) View.inflate( this.context, layoutResource, null );
                if( !(item.getKey() instanceof Transaction)
                        || !(item.getValue() instanceof Double)) throw new Error("unknown data type");
                Transaction transaction = (Transaction) item.getKey();
                Double balance = (Double) item.getValue();

                setLineItemPropertyViews( listLineItem, transaction );
                setLineItemAlert( listLineItem, transaction, balance );

                TextView balanceView = listLineItem.findViewById( R.id.balance_value );
                if(balanceView != null) balanceView.setText( formatUtility.formatCurrency( balance ));

                initializeCardLayout( listLineItem, transaction );
                initializeCardButtons( listLineItem, transaction );

                listLineItem.setOnClickListener( this::toggleCardVisibility );
                listView.addView( listLineItem );
            }
        }

        else throw new Error("unknown list data type");

    }

    void buildPeriodListView(ViewGroup periodListView, int periodItemLayout, Object dataObject){
        Object listData;


        if(dataObject instanceof Integer){
            Integer typeInt = (Integer) dataObject;
            if( typeInt == R.string.balancesheet_projections ) listData = budgetModel.getProjectionsWithBalancesByPeriod();
            //else if( typeInt == R.string.records_by_period ) listData = budgetModel.getRecordsByPeriod();
            else throw new Error("unknown data type resorce identifier");

        } else listData = dataObject;


        if( listData instanceof List){
            List<?> listDataSet = (List) listData;
            for( Object item : listDataSet){
                ViewGroup periodItemView;
                Object[] periodData;
                if( !(item instanceof Object[]) ) throw new Error("unknown data type");
                else periodData = (Object[]) item;

                LocalDate periodStartDate;
                List periodBalanceData;
                Map periodTransactionsWithBalance;

                if( !(periodData[0] instanceof LocalDate) || !(periodData[1] instanceof List) || !(periodData[2] instanceof Map)) throw new Error( "unknown data type" );
                else {
                    periodStartDate = (LocalDate)periodData[0];
                    periodBalanceData = (List)periodData[1];
                    periodTransactionsWithBalance = (Map)periodData[2];
                }

                periodItemView = (ViewGroup) View.inflate( this.context, periodItemLayout, null );
                setPeriodItemHeader( periodItemView.findViewById( R.id.period_header ), periodStartDate );
                buildTransactionListView( periodItemView.findViewById( R.id.period_projection_list ), R.layout.transaction_list_item_with_balance, periodTransactionsWithBalance);
                setPeriodItemBalances( periodItemView, periodBalanceData );


                periodItemView.findViewById( R.id.period_projection_list ).setVisibility( VISIBLE );
                periodItemView.findViewById( R.id.period_list_item_balances ).setVisibility( GONE );
                periodItemView.setOnClickListener( v -> {
                    View dataList = v.findViewById( R.id.period_projection_list );
                    View balanceList = v.findViewById( R.id.period_list_item_balances );
                    if(dataList.getVisibility() != VISIBLE){
                        dataList.setVisibility( VISIBLE );
                        balanceList.setVisibility( GONE );
                    } else {
                        dataList.setVisibility( GONE );
                        balanceList.setVisibility( VISIBLE );
                    }
                } );
                periodListView.addView( periodItemView );
            }
        } else throw new Error("unknown list data type");

    }



    private void setPeriodItemHeader(ViewGroup periodHeader, LocalDate periodBeginDate){
        TextView periodDateView = periodHeader.findViewById( R.id.projection_period_header_date );
        periodDateView.setText( periodBeginDate.format( DateTimeFormatter.ofPattern( "MMMM yyyy" ) ) );

    }

    private void setPeriodItemBalances(ViewGroup periodItem, List<Double> periodBalances){
        Double initialBal = periodBalances.get(0);
        Double endBal = periodBalances.get(3);
        Double income = periodBalances.get(1);
        Double expense = periodBalances.get(2);


        TextView startOfPeriodBalance = periodItem.findViewById( R.id.projection_period_header_initial_balance );
//                if(initialBal < 0) startOfPeriodBalance.setTextAppearance( R.style.balance_amount_negative );
//                else startOfPeriodBalance.setTextAppearance( R.style.balance_amount_not_negative );
        startOfPeriodBalance.setText( formatUtility.formatCurrency( initialBal ) );

        TextView endOfPeriodBalance = periodItem.findViewById( R.id.projection_period_header_ending_balance );
        if(endBal < 0) endOfPeriodBalance.setTextColor( this.context.getColor( R.color.colorCurrencyNegative ));
        else endOfPeriodBalance.setTextColor( this.context.getColor( R.color.colorCurrencyNotNegative ));
        endOfPeriodBalance.setText( formatUtility.formatCurrency( endBal ) );

        TextView periodIncome = periodItem.findViewById( R.id.projection_period_header_income);
        periodIncome.setTextColor( this.context.getColor( R.color.colorCurrencyNotNegative ));
        periodIncome.setText( "+" + formatUtility.formatCurrency( income ));

        TextView periodExpenses = periodItem.findViewById( R.id.projection_period_header_expense );
        periodExpenses.setTextColor( this.context.getColor( R.color.colorCurrencyNegative ));
        periodExpenses.setText( "-" + formatUtility.formatCurrency( expense ));

        TextView netGain = periodItem.findViewById( R.id.projection_period_header_net_gain );
        TextView netGainLabel = periodItem.findViewById( R.id.projection_period_header_net_gain_label );
        if(endBal < initialBal){
            netGainLabel.setText( "Loss" );
            netGain.setText("-" + formatUtility.formatCurrency( endBal - initialBal ));
            netGain.setTextColor( this.context.getColor( R.color.colorCurrencyNegative ));
        } else {
            netGainLabel.setText( "Gain");
            netGain.setText("+" + formatUtility.formatCurrency( endBal - initialBal ));
            netGain.setTextColor( this.context.getColor( R.color.colorCurrencyNotNegative ));
        }
    }

    private void setLineItemHeader(ViewGroup listHeader, int layoutResource) {

        TextView dateHeader = listHeader.findViewById( R.id.date_value );
        TextView labelHeader = listHeader.findViewById( R.id.label_value );
        TextView amountHeader = listHeader.findViewById( R.id.amount_value );

        dateHeader.setText("Date");
        labelHeader.setText("Label");
        amountHeader.setText("Amt");

//        dateHeader.setTextAppearance( R.style.period_list_item_header );
//        labelHeader.setTextAppearance( R.style.period_list_item_header );
//        amountHeader.setTextAppearance( R.style.period_list_item_header );

//        listHeader.setBackgroundResource( R.drawable.list_header_item );
        listHeader.setBackgroundResource( R.drawable.list_header_item );

        TextView balanceHeader = listHeader.findViewById( R.id.balance_value );
        if(balanceHeader != null) balanceHeader.setText( "Bal" );

    }

    private void setLineItemPropertyViews(ViewGroup listLineItem, Transaction transaction){

        //set date column
        TextView dateValue = listLineItem.findViewById( R.id.date_value );
        if(dateValue != null){
            LocalDate date = (LocalDate)transaction.getProperty( R.string.date_tag );
            dateValue.setText( formatUtility.formatDate( R.string.date_string_dayofweekanddate, date));
        }

        //set label column
        TextView labelValue = listLineItem.findViewById( R.id.label_value );
        if(labelValue != null)
            labelValue.setText( ((String)transaction.getProperty( R.string.label_tag )) );

        //set amount column
        TextView amount = listLineItem.findViewById( R.id.amount_value );
        if(amount != null){
            if(transaction.getIncomeFlag()) {
                amount.setText( "+ " + formatUtility.formatCurrency( transaction.getProperty( R.string.amount_tag )));
//                amount.setTextAppearance( R.style.balance_amount_not_negative );
            }
            else {
                amount.setText( "- " + formatUtility.formatCurrency(transaction.getProperty( R.string.amount_tag )));
//                amount.setTextAppearance( R.style.balance_amount_negative );
            }
        }

    }

    private void setLineItemAlert(ViewGroup listLineItem, Transaction transaction, Double balance){
        ImageView alert = listLineItem.findViewById( R.id.alert_indicator );
        if( balance <= 0.0){
            //set alert for balance below threshold
            alert.setImageDrawable( this.context.getDrawable( android.R.drawable.ic_dialog_alert ) );
            alert.setBackgroundColor( this.context.getColor( R.color.colorCurrencyNegative ) );
        }else if(balance < budgetModel.getThresholdValue()){
            //set alert for balance below zero
            alert.setImageDrawable( this.context.getDrawable( android.R.drawable.ic_dialog_alert ) );
            alert.setBackgroundColor( this.context.getColor( R.color.colorCurrencyNotNegative ) );
        }

        LocalDate date = (LocalDate) transaction.getProperty( R.string.date_tag );
        if(date.isBefore( LocalDate.now().plusDays( 1 ) )){
            //set visual que that item is reconcilable
            alert.setImageDrawable( this.context.getDrawable( android.R.drawable.ic_input_add ));
        }
    }

    private void setCardLayoutPropertyViews(ViewGroup view, Transaction transaction, boolean isEditable){

        View labelEditor = view.findViewById( R.id.label_value );
        if(labelEditor != null){
            String label = String.valueOf( transaction.getProperty( R.string.label_tag ));
            if(isEditable && labelEditor instanceof EditText){
                ((EditText) labelEditor).setText( label );
                labelEditor.setEnabled( true );
//                labelEditor.setTextAppearance( R.style.property_value_cell_editor );
            } else{
                if(labelEditor instanceof EditText) ((EditText) labelEditor).setText( label );
                else ((TextView) labelEditor).setText( label );
                labelEditor.setEnabled( false );
//                labelEditor.setTextAppearance( R.style.property_value_cell_display );
            }
        }

        View amount = view.findViewById( R.id.amount_value );
        if(amount != null){
            Double amountValue = (Double) transaction.getProperty( R.string.amount_tag );
            if(isEditable && amount instanceof EditText){
                if(amountValue == 0.0) ((EditText) amount).setText("");
                else ((EditText) amount).setText(String.valueOf( amountValue ));
                amount.setEnabled( true );
//                amount.setTextAppearance( R.style.property_value_cell_editor );

            } else{
                ((TextView) amount).setText( formatUtility.formatCurrency( amountValue ) );
                amount.setEnabled( false );
//                amount.setTextAppearance( R.style.property_value_cell_display );
            }
        }

        EditText note = (EditText) view.findViewById( R.id.note_value );
        if(note != null){
            if(isEditable){
                note.setText( (String) transaction.getProperty( R.string.note_tag ));
                note.setEnabled( true );
//                note.setTextAppearance( R.style.property_value_cell_editor );

            } else {
                note.setText( (String) transaction.getProperty( R.string.note_tag ));
                note.setEnabled( false );
//                note.setTextAppearance( R.style.property_value_cell_display );
            }

        }

        TextView scheduledDateView = (TextView) view.findViewById( R.id.scheduled_date_value );
        if(scheduledDateView != null) {
            LocalDate scheduledDate = ((ProjectedTransaction)transaction).getScheduledProjectionDate();
            scheduledDateView.setText( formatUtility.formatDate( R.string.date_string_short_md, scheduledDate ));
        }

        TextView scheduledAmountView = (TextView) view.findViewById( R.id.scheduled_amount_value );
        if(scheduledAmountView != null) {
            Double scheduledAmount = ((ProjectedTransaction)transaction).getScheduledAmount();
            scheduledAmountView.setText( formatUtility.formatCurrency( scheduledAmount ));
        }

        View dateView = view.findViewById( R.id.date_value );
        if(dateView != null){
            LocalDate date = (LocalDate) transaction.getProperty( R.string.date_tag );
            String dateString = formatUtility.formatDate(R.string.date_string_long_mdy, date);

            if(date == null) {
                dateString = "unscheduled";
                date = LocalDate.now();
            }

            if(isEditable && dateView instanceof Button){
                Button dateEditorButton = (Button) dateView;
                dateEditorButton.setText( dateString );
                dateEditorButton.setTag(R.string.date_tag, date);
                dateEditorButton.setEnabled( true );
//                dateEditorButton.setTextAppearance( R.style.property_value_cell_editor );


                LocalDate finalDate = date;
                dateEditorButton.setOnClickListener( dateButtonView -> {
                    final LocalDate[] newDate = {finalDate};
                    DatePickerDialog pickerDialog = new DatePickerDialog( this.context );
                    DatePicker picker = pickerDialog.getDatePicker();

                    picker.init( newDate[0].getYear(), newDate[0].getMonthValue()-1, newDate[0].getDayOfMonth(), (button_view, year, monthOfYear, dayOfMonth) -> {
                        newDate[0] = LocalDate.of( year, monthOfYear+1, dayOfMonth );
                    } );
                    pickerDialog.setButton( DatePickerDialog.BUTTON_POSITIVE, "OK", (dialog, which)->{
                        Button dateButton = view.findViewById( R.id.date_value );
                        dateButton.setText( formatUtility.formatDate( R.string.date_string_long_mdy, newDate[0]));
                        dateButton.setTag(R.string.date_tag, newDate[0] );
                        dialog.dismiss();
                    } );

                    pickerDialog.setContentView( picker );
                    pickerDialog.show();
                } );

            }else {
                ((TextView) dateView).setText( formatUtility.formatDate(R.string.date_string_long_mdy, date) );
                dateView.setTag(R.string.date_tag, date);
                dateView.setEnabled( false );
//                dateEditorButton.setTextAppearance( R.style.property_value_cell_editor );
            }
        }

        Switch recurrenceSwitch = (Switch) view.findViewById( R.id.recurrence_switch );
        if(recurrenceSwitch != null) {
            RecurrenceViewBuilder recurrenceViewBuilder = new RecurrenceViewBuilder( this.context );
            recurrenceViewBuilder.setRecurrencePropertyViews( view, transaction, isEditable, recurrenceSwitch );
        }
    }

    private void updateTransactionFromEditorView(ViewGroup editorView, Transaction transaction){

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

                Button recurrenceDateButton = editorView.findViewById( R.id.recurrence_date_value );
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

    private void toggleCardVisibility(View view) {
        ViewGroup listV = view.findViewById( R.id.period_projection_list );
        ViewGroup cardV = view.findViewById( R.id.card_view );
        ViewGroup buttonsV = view.findViewById( R.id.card_buttons );

        if(cardV.getVisibility() != VISIBLE){
            cardV.setVisibility( VISIBLE );
            buttonsV.setVisibility( VISIBLE );
            if(listV != null) listV.setVisibility( GONE );
        } else {
            cardV.setVisibility( GONE );
            buttonsV.setVisibility( GONE );
            if(listV != null) listV.setVisibility( VISIBLE );
        }
    }

    private void initializeCardButtons(ViewGroup view, Transaction transaction){
        ViewGroup buttonsView = view.findViewById( R.id.card_buttons );
        ViewGroup cardDrawer = view.findViewById( R.id.card_view );

        Button modifyButton = buttonsView.findViewById( R.id.modify_transaction_button );
        Button resetProjectionButton = buttonsView.findViewById( R.id.reset_projections_button );
        Button updateButton = buttonsView.findViewById( R.id.editor_update_button );
        Button cancelButton = buttonsView.findViewById( R.id.editor_cancel_button );
        Button deleteButton = buttonsView.findViewById( R.id.editor_delete_button );
        Button recordTransactionButton = buttonsView.findViewById( R.id.record_transaction_button );

        if(modifyButton != null) modifyButton.setOnClickListener( v -> {
            setCardLayoutPropertyViews( cardDrawer, transaction, true );

            cancelButton.setVisibility( View.VISIBLE );
            modifyButton.setVisibility( GONE );
            updateButton.setVisibility( View.VISIBLE );

            if(transaction instanceof ProjectedTransaction) {
                resetProjectionButton.setVisibility( View.VISIBLE );
                deleteButton.setVisibility( GONE );
            }
            else {
                deleteButton.setVisibility( View.VISIBLE );
                resetProjectionButton.setVisibility( GONE );
            }

        } );

        if(resetProjectionButton != null) resetProjectionButton.setOnClickListener( v-> {} );

        if(updateButton != null) updateButton.setOnClickListener( v -> {
            updateTransactionFromEditorView( view.findViewById( R.id.card_view ), transaction );
            budgetModel.update( transaction );

            this.context.recreate();
        } );

        if(cancelButton != null) cancelButton.setOnClickListener( v-> {
            setCardLayoutPropertyViews( cardDrawer, transaction, false );

            modifyButton.setVisibility( View.VISIBLE );
            updateButton.setVisibility( GONE );
            resetProjectionButton.setVisibility( GONE );
            deleteButton.setVisibility( GONE );
            cancelButton.setVisibility( GONE );

        } );

        if(deleteButton != null) deleteButton.setOnClickListener( v -> {
            budgetModel.delete( transaction );
            this.context.recreate();
        } );

        if(recordTransactionButton != null) recordTransactionButton.setOnClickListener( v -> {
            budgetModel.reconcile(transaction);
            this.context.recreate();
        } );

    }

    private void initializeCardLayout(ViewGroup view, Transaction transaction){
        ViewGroup cardViewLayout;

        if(transaction instanceof ProjectedTransaction) cardViewLayout = (ViewGroup) ViewGroup.inflate( this.context, R.layout.layout_projection, null );
        else if(transaction instanceof RecordedTransaction) cardViewLayout = (ViewGroup) ViewGroup.inflate( this.context, R.layout.layout_record, null );
        else cardViewLayout = (ViewGroup) ViewGroup.inflate( this.context, R.layout.layout_transaction, null );

        setCardLayoutPropertyViews( cardViewLayout, transaction, false );

        ((ViewGroup)view.findViewById( R.id.card_view )).addView( cardViewLayout );
    }
}
