package com.imocc.base;

/**
 * Created by Administrator on 2018/1/16.
 * Datatables响应结构
 */
public class ApiDataTableResponse  extends ApiResponse{
    private int draw;
    private long recordsTotal;
    private long recordsFiltered;

    public ApiDataTableResponse(int code, String message, Object data) {
        super(code, message, data);
    }

    public ApiDataTableResponse(ApiResponse.Status status) {
        this(status.getCode(),status.getStandardMessage(),null);
    }

    public int getDraw() {
        return draw;
    }

    public void setDraw(int draw) {
        this.draw = draw;
    }

    public long getRecordsTotal() {
        return recordsTotal;
    }

    public void setRecordsTotal(long recordsTotal) {
        this.recordsTotal = recordsTotal;
    }

    public long getRecordsFiltered() {
        return recordsFiltered;
    }

    public void setRecordsFiltered(long recordsFiltered) {
        this.recordsFiltered = recordsFiltered;
    }
}

