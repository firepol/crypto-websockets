package com.github.firepol.cryptows;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.math.BigDecimal;
import java.util.Date;

@DatabaseTable(tableName = "order_book")
public class BookOrder {

    public static final String EXCHANGE_NAME_FIELD_NAME = "exchange_name";
    public static final String BASE_FIELD_NAME = "base";
    public static final String QUOTE_FIELD_NAME = "quote";
    public static final String SIDE_FIELD_NAME = "side";
    public static final String PRICE_FIELD_NAME = "price";
    public static final String VOLUME_FIELD_NAME = "volume";
    public static final String SORT_FIELD_NAME = "sort";
    public static final String DATETIME_FIELD_NAME = "datetime";

    // TODO : verify why the DB is not created correctly... compared to python's sqlalchemy:
    // `width` is ignored, bigdecimal as varchar, date as datetime...

    @DatabaseField(generatedId = true)
    public Long id;

    @DatabaseField(columnName = EXCHANGE_NAME_FIELD_NAME, canBeNull = false, width = 20)
    public String exchangeName;

    @DatabaseField(columnName = SIDE_FIELD_NAME, canBeNull = false, width = 3)
    public String side; // can be: 'ask' or 'bid'

    @DatabaseField(columnName = BASE_FIELD_NAME, canBeNull = false, width = 10)
    public String base;

    @DatabaseField(columnName = QUOTE_FIELD_NAME, canBeNull = false, width = 10)
    public String quote;

    @DatabaseField(columnName = PRICE_FIELD_NAME, dataType = DataType.BIG_DECIMAL_NUMERIC, canBeNull = false)
    public BigDecimal price;

    @DatabaseField(columnName = VOLUME_FIELD_NAME, dataType = DataType.BIG_DECIMAL_NUMERIC, canBeNull = false)
    public BigDecimal volume;

    @DatabaseField(columnName = SORT_FIELD_NAME, canBeNull = false)
    public Integer sort;

    @DatabaseField(columnName = DATETIME_FIELD_NAME, dataType = DataType.DATE_STRING, canBeNull = false)
    public Date datetime;

    public BookOrder() {
        // ORMLite needs a no-arg constructor
    }

    public BookOrder(String exchangeName, String side, String base, String quote, BigDecimal price, BigDecimal volume,
                     Integer sort, Date datetime) {
        this.exchangeName = exchangeName;
        this.base = base;
        this.quote = quote;
        this.side = side;
        this.price = price;
        this.volume = volume;
        this.sort = sort;
        this.datetime = datetime;
    }
}