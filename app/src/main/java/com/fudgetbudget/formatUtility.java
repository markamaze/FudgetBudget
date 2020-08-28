package com.fudgetbudget;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

class formatUtility {

    static String formatCurrency(Object currency){ return "$" + String.format( Locale.ENGLISH, "%.0f", currency ); }
    static String formatDate(int return_type, LocalDate date) {

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
    static String formatRecurrenceValue(String[] recurrenceValues) {

        return "readable recurrence";
    }
}
