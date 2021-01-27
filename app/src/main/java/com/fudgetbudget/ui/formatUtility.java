package com.fudgetbudget.ui;

import com.fudgetbudget.R;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class formatUtility {

    public static String formatCurrency(Object currency){
//        if(currency == null) return "-";
        if(currency instanceof Double) return String.format( Locale.ENGLISH, "%.2f", currency );
        else if(currency instanceof String){
            if(((String)currency).isEmpty()) return "-";
            return String.format( Locale.ENGLISH, "%.0f", Double.parseDouble(currency.toString()));
        }
        else return currency.toString();

    }
    public static String formatDate(Object return_type, LocalDate date) {
        int resourceId;

        if(date == null) return "-";
        else if(return_type instanceof String) {
            if(((String)return_type).isEmpty()) return date.format( DateTimeFormatter.ofPattern( "M/d/yy" ) );
            else return date.format( (DateTimeFormatter.ofPattern( (String)return_type )) );
        }
        else if(return_type instanceof Integer) resourceId = (Integer) return_type;
        else throw new Error("unknown data type sent to formatDate");

        if(resourceId == R.string.date_string_long_mdy){
            return date.format( DateTimeFormatter.ofPattern( "eee MMMM dd, yyyy" ) );
        }
        else if(resourceId == R.string.date_long_dowmd) return date.format( DateTimeFormatter.ofPattern( "eee MMM d"  ));
        else if(resourceId == R.string.date_string_dayofweekanddate) return date.format(DateTimeFormatter.ofPattern( "d - eee" ));

        else if(resourceId == R.string.date_string_dayofweekinmonth){
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
        else if(resourceId == R.string.date_string_dateofmonth){
            int dateOfMonth = date.getDayOfMonth();
            if(dateOfMonth == 1) return "1st";
            else if(dateOfMonth == 2) return "2nd";
            else if(dateOfMonth == 3) return "3rd";
            else return String.valueOf( dateOfMonth ) + "th";


        }
        else return date.format( DateTimeFormatter.BASIC_ISO_DATE );
    }
    public static String formatRecurrenceValue(String[] recurrenceValues, LocalDate scheduledDate) {
        if(recurrenceValues[0].contentEquals( "0" )) return "single transaction";

        Integer frequency = Integer.parseInt( recurrenceValues[1] );
        String period = recurrenceValues[2];
        String onDayType = recurrenceValues[3];



        if(period.contentEquals( "0" )) onDayType = "";
        else if(period.contentEquals( "1" )) onDayType = "on " + scheduledDate.getDayOfWeek();
        else if(onDayType.contentEquals( "0")) {
            if(period.contentEquals( "2" )) {
                int scheduledDay = scheduledDate.getDayOfMonth();
                int lengthOfMonth = scheduledDate.lengthOfMonth();

                if(lengthOfMonth == scheduledDay) onDayType = "on last day of month";
                else {
                    if(scheduledDay == 1) onDayType = "on the 1st";
                    else if(scheduledDay == 2) onDayType = "on the 2nd";
                    else if(scheduledDay == 3) onDayType = "on the 3rd";
                    else onDayType = "on the " + scheduledDay + "th";

                }
            }
            else if(period.contentEquals( "3" )) {
                onDayType = "on day same date annually" ;
            }

        }
        else if(onDayType.contentEquals( "1")) {
            if(period.contentEquals( "2" )){
                int scheduledDay = scheduledDate.getMonthValue();
                DayOfWeek dayOfWeek = scheduledDate.getDayOfWeek();

                int weekCount = 0;
                int dayIndex = scheduledDay;
                while(dayIndex > 0){
                    ++weekCount;
                    dayIndex = dayIndex - 7;
                }

                if(weekCount == 1) onDayType = "on first " + dayOfWeek + " of month";
                else if(weekCount == 2) onDayType = "on second " + dayOfWeek + " of month";
                else if(weekCount == 3) onDayType = "on third " + dayOfWeek + " of month";
                else if(weekCount == 4) onDayType = "on fourth " + dayOfWeek + " of month";
                else if(weekCount == 5) onDayType = "on last " + dayOfWeek + " of month";

                onDayType = "on " + weekCount + dayOfWeek + " of month";
            }
            else if(period.contentEquals( "3" )){

            }


            onDayType = "on day of week count";
        }
        if(period.contentEquals( "0")) period = "Day";
        else if(period.contentEquals( "1")) period = "Week";
        else if(period.contentEquals( "2")) period = "Month";
        else if(period.contentEquals( "3")) period = "Year";

        if( frequency > 1 ) period.concat( "s" );


        String result;
        if(frequency == 1) result = "Every " + period + " " + onDayType;
        else result = "Every " + frequency + " " + period + "s " + onDayType;

        return result;
    }
    public static String formatRecurrenceShortValue(String[] recurrenceValues) {
        if(recurrenceValues[0].contentEquals( "0")) return "";

        Integer frequency = Integer.parseInt( recurrenceValues[1] );
        String period = recurrenceValues[2];
        String onDayType = recurrenceValues[3];

        if(period.contentEquals( "0")) period = "days";
        else if(period.contentEquals( "1")) period = "weeks";
        else if(period.contentEquals( "2")) period = "months";
        else if(period.contentEquals( "3")) period = "years";

        String result = frequency + " " + period ;

        if(result.contentEquals( "1days" )) result = "daily";
        else if(result.contentEquals( "1weeks" )) result = "weekly";
        else if(result.contentEquals( "1months" )) result = "monthly";
        else if(result.contentEquals( "1years" )) result = "yearly";

        return result;
    }
}
