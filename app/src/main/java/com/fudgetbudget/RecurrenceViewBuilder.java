package com.fudgetbudget;

import android.app.DatePickerDialog;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import com.fudgetbudget.model.Transaction;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static android.view.View.GONE;

class RecurrenceViewBuilder {
    private final MainActivity context;

    RecurrenceViewBuilder(MainActivity context){
        this.context = context;
    }

    void setRecurrencePropertyViews(ViewGroup view, Transaction transaction, boolean isEditable, Switch recurrenceSwitch) {
        String stringRecurrence = (String) transaction.getProperty( R.string.recurrence_tag );
        if(stringRecurrence == null) stringRecurrence = "0-0-0-0-0-0-0";

        String[] recurrenceValues = stringRecurrence.split( "-" );
        int recurrenceBit = Integer.parseInt( recurrenceValues[0] );
        LocalDate date = (LocalDate) transaction.getProperty( R.string.date_tag );


        //set recurrenceSwitch values and handlers
        if(recurrenceBit == 1) recurrenceSwitch.setChecked( true );
        else recurrenceSwitch.setChecked( false );
        setRecurrencePropertyVisibility(view, isEditable, recurrenceBit);
        recurrenceSwitch.setOnCheckedChangeListener( (buttonView, isChecked) -> {
            buttonView.setChecked( isChecked );
            if(isChecked) setRecurrencePropertyVisibility( view, isEditable, 1 );
            else setRecurrencePropertyVisibility( view, isEditable, 0 );

        });

        TextView readableRecurrence = view.findViewById( R.id.readable_recurrence_value );
        readableRecurrence.setText( formatUtility.formatRecurrenceValue(recurrenceValues) );

        //set frequency values and handlers
        EditText frequencyEditor = view.findViewById( R.id.recurrence_frequency_value );
        if(recurrenceValues[1].contentEquals( "0" )) frequencyEditor.setText("");
        else frequencyEditor.setText( recurrenceValues[1]);


        //set onDay Type
        Spinner onDaySpinner = view.findViewById( R.id.on_day_type_selector );
        int periodValueBit = Integer.parseInt( recurrenceValues[2]);
        if(onDaySpinner != null){
            if(periodValueBit == 0 || periodValueBit == 1) onDaySpinner.setVisibility( GONE );
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
        ArrayAdapter<String> adapter = new ArrayAdapter<>( this.context, android.R.layout.simple_list_item_1, periodList );
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
                        LinearLayout onDayLayout = RecurrenceViewBuilder.this.context.findViewById( R.id.set_recurrence_onDateType_layout );
                        if(onDayLayout != null) onDayLayout.setVisibility( GONE );
                    }
                    else if(position == 1){
                        String[] onDayTypeWeekday = RecurrenceViewBuilder.this.context.getResources().getStringArray( R.array.days_of_week_options );
                        ArrayAdapter<String> adapter1 = new ArrayAdapter<>( RecurrenceViewBuilder.this.context, android.R.layout.simple_list_item_1, onDayTypeWeekday );
                        onDaySpinner.setAdapter( adapter1 );
                        onDaySpinner.setEnabled( false );
                        onDaySpinner.setSelection( date.getDayOfWeek().getValue() );
                    }
                    else if(position == 2){
                        String[] onDayTypeDayOfMonth = getOnDayOfMonthList(date);
                        ArrayAdapter<String> adapter1 = new ArrayAdapter<>( RecurrenceViewBuilder.this.context, android.R.layout.simple_list_item_1, onDayTypeDayOfMonth );
                        onDaySpinner.setAdapter( adapter1 );
                        onDaySpinner.setSelection( 0 );
                    }
                    else if(position == 3){
                        String[] onDayTypeDayOfYear = new String[]{};
                        ArrayAdapter<String> adapter1 = new ArrayAdapter<>( RecurrenceViewBuilder.this.context, android.R.layout.simple_list_item_1, onDayTypeDayOfYear );
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
        dateButton.setText(formatUtility.formatDate( R.string.date_string_long_mdy, date ));
        dateButton.setTag(R.string.date_tag, date);

        dateButton.setOnClickListener( dateButtonView -> {
            final LocalDate[] newDate = {date};
            DatePickerDialog pickerDialog = new DatePickerDialog( RecurrenceViewBuilder.this.context );
            DatePicker picker = pickerDialog.getDatePicker();

            picker.init( newDate[0].getYear(), newDate[0].getMonthValue()-1, newDate[0].getDayOfMonth(), (button_view, year, monthOfYear, dayOfMonth) ->
                newDate[0] = LocalDate.of( year, monthOfYear+1, dayOfMonth ) );
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
            if(periodValueBit == 0 ) onDaySpinner.setVisibility( GONE );
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
        String formattedDateOfMonth = formatUtility.formatDate(R.string.date_string_dateofmonth, date);
        String dateOfMonth = formattedDateOfMonth + " of each month";

        String formattedDayOfWeekInMonth = formatUtility.formatDate(R.string.date_string_dayofweekinmonth, date);
        String dayOfWeekInMonth = formattedDayOfWeekInMonth + " of each month";

        String[] dayOfArray = new String[]{dateOfMonth, dayOfWeekInMonth};

        return dayOfArray;
    }

    private void setRecurrencePropertyVisibility(View view, boolean isEditable, int recurrenceBit) {
        if(recurrenceBit == 1) { //repeats
            view.findViewById( R.id.property_layout_recurrence_editor ).setVisibility( View.VISIBLE );
            view.findViewById( R.id.property_layout_date ).setVisibility( GONE );


            if(isEditable) {
                //repeats and is editable: show the recurrence editor with values set
                view.findViewById( R.id.property_layout_recurrence_editor ).setVisibility( View.VISIBLE );
                view.findViewById( R.id.property_layout_recurrence_switch ).setVisibility( View.VISIBLE );
                view.findViewById( R.id.property_layout_recurrence_readable ).setVisibility( GONE );
                view.findViewById( R.id.property_layout_recurrence_projections ).setVisibility( GONE );

            } else {
                //repeats and is not editable: show readable text representation of the recurrence value and next scheduled date

                view.findViewById( R.id.property_layout_recurrence_switch ).setVisibility( GONE );
                view.findViewById( R.id.property_layout_recurrence_editor ).setVisibility( GONE );
                view.findViewById( R.id.property_layout_recurrence_readable ).setVisibility( View.VISIBLE );
                view.findViewById( R.id.property_layout_recurrence_projections ).setVisibility( View.VISIBLE );
            }
        }
        else { //does not repeat
            view.findViewById( R.id.property_layout_recurrence_editor ).setVisibility( GONE );
            view.findViewById( R.id.property_layout_recurrence_projections ).setVisibility( GONE );
            view.findViewById( R.id.property_layout_date ).setVisibility( View.VISIBLE );


            if(isEditable) {
                //does not repeat, but can be edited: need to load the recurrence editor and remove scheduled date
                view.findViewById( R.id.property_layout_recurrence_switch ).setVisibility( View.VISIBLE );
            }
            else{
                //does not repeat and cannot edit: just show the date and remove recurrence editor
                //remove recurrenceSwitch
                view.findViewById( R.id.property_layout_recurrence_switch ).setVisibility( GONE );
            }

        }
    }

}
