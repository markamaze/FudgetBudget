package com.e.fudgetbudget.model;

import com.e.fudgetbudget.R;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.time.LocalDate;

public class RecordedTransaction extends Transaction {
    String recordNote;
    Double recordAmount;
    LocalDate recordDate;

    public static RecordedTransaction getInstance(Transaction transaction) { return new RecordedTransaction( transaction ); }
    public static RecordedTransaction getInstance(Element element, Transaction transaction){ return new RecordedTransaction( element, transaction ); }
    private RecordedTransaction(Transaction transaction) {
        super( transaction );
        this.setProperty( R.string.date_tag, transaction.getProperty( R.string.date_tag ) );
        this.setProperty( R.string.amount_tag, transaction.getProperty( R.string.amount_tag ) );
        this.setProperty( R.string.note_tag, transaction.getProperty( R.string.note_tag ) );
    }
    private RecordedTransaction(Element element, Transaction transaction){
        super(transaction);

        NodeList elementNodes = element.getChildNodes();

        for(int i = 0; i < elementNodes.getLength(); i++){
            Element recordProperty = (Element) elementNodes.item( i );
            this.setProperty( Integer.parseInt( recordProperty.getAttribute( "key" )), recordProperty.getTextContent() );
        }
    }


    private boolean setRecordDate(Object object){
        if(object == null || object instanceof String && ((String)object).isEmpty()) this.recordDate = LocalDate.now();
        else if( object instanceof LocalDate) this.recordDate = (LocalDate) object;
        else if( object instanceof String) this.recordDate = LocalDate.parse( (String)object );
        else if( object instanceof Long) this.recordDate = LocalDate.ofEpochDay( (Long)object );
        else return false;

        return true;
    }
    private boolean setRecordAmount(Object amount){
        if(amount == null || amount instanceof String && ((String)amount).isEmpty()) this.recordAmount = new Double( "0" );
        else if(amount instanceof Double) this.recordAmount = (Double) amount;
        else if(amount instanceof String) this.recordAmount = Double.valueOf( (String) amount );
        else return false;

        return true;
    }
    private boolean setRecordNote(Object note){
        if(note == null) this.recordNote = new String();
        else if(note instanceof String) this.recordNote = (String) note;
        else return false;

        return true;
    }

    private LocalDate getRecordDate(){ return this.recordDate; }
    private Double getRecordAmount(){ return this.recordAmount; }
    private String getRecordNote(){ return this.recordNote; }

    public boolean setProperty(int propertyTag, Object value){
        switch (propertyTag){
            case R.string.date_tag: return setRecordDate( value );
            case R.string.amount_tag: return setRecordAmount( value );
            case R.string.note_tag: return setRecordNote( value );
            default: return super.setProperty(propertyTag, value);

        }
    }
    public Object getProperty(int propertyTag){
        switch(propertyTag){
            case R.string.date_tag: return getRecordDate();
            case R.string.amount_tag: return getRecordAmount();
            case R.string.note_tag: return getRecordNote();
            default: return super.getProperty(propertyTag);
        }
    }
}
