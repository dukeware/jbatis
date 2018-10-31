package com.ibatis.sqlmap.client.event;

public interface TotalRowHandler extends RowHandler {
    int handleTotal(int delta);
}
