package com.github.firepol.cryptows;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.math.BigDecimal;
import java.util.Date;

@DatabaseTable(tableName = "order_book")
public class OrderBook {

    public static final String EXCHANGE_ID_FIELD_NAME = "exchange_id";
    public static final String BASE_FIELD_NAME = "base";
    public static final String QUOTE_FIELD_NAME = "quote";
    public static final String SIDE_FIELD_NAME = "side";
    public static final String PRICE_FIELD_NAME = "price";
    public static final String VOLUME_FIELD_NAME = "volume";
    public static final String SORT_FIELD_NAME = "sort";
    public static final String MODIFIED_FIELD_NAME = "modified";

    // TODO : verify why the DB is not created correctly... compared to sqlalchemy:
    // `with` is ignored, bigdecimal as varchar, date as timestamp...

    @DatabaseField(generatedId = true)
    public Long id;

    @DatabaseField(columnName = EXCHANGE_ID_FIELD_NAME, canBeNull = false, width = 20)
    public String exchangeId;

    @DatabaseField(columnName = SIDE_FIELD_NAME, canBeNull = false, width = 3)
    public String side; // ask / bid

    @DatabaseField(columnName = BASE_FIELD_NAME, canBeNull = false, width = 10)
    public String base;

    @DatabaseField(columnName = QUOTE_FIELD_NAME, canBeNull = false, width = 10)
    public String quote;

    @DatabaseField(columnName = PRICE_FIELD_NAME, dataType = DataType.BIG_DECIMAL, canBeNull = false)
    public BigDecimal price;

    @DatabaseField(columnName = VOLUME_FIELD_NAME, dataType = DataType.BIG_DECIMAL, canBeNull = false)
    public BigDecimal volume;

    @DatabaseField(columnName = SORT_FIELD_NAME, canBeNull = false)
    public Integer sort;

    @DatabaseField(columnName = MODIFIED_FIELD_NAME, canBeNull = false)
    public Date modified;

    public OrderBook() {
        // ORMLite needs a no-arg constructor
    }

    public OrderBook(String exchangeId, String side, String base, String quote, BigDecimal price, BigDecimal volume,
                     Integer sort, Date modified) {
        this.exchangeId = exchangeId;
        this.base = base;
        this.quote = quote;
        this.side = side;
        this.price = price;
        this.volume = volume;
        this.sort = sort;
        this.modified = modified;
    }
}